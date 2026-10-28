package com.example.questionbank.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件 -> 纯文本 -> 题目 的入口。
 *
 * 解析质量的三个关键点：
 * 1. PDF 必须按坐标排序读取（sortByPosition），否则 PDFBox 会按内容流顺序输出，
 *    题干、选项、答案会被打乱，导致大量题目匹配不上；
 * 2. 表格、页眉页脚、分页符统一清洗成「一行一条内容」的纯文本；
 * 3. 题目切分交给 {@link QuestionParser}，与文件格式无关。
 */
@Component
public class FileParserUtil {

    /** 可选的 OCR 开关（扫描版 PDF / 图片）；找不到语言包时自动跳过 */
    private static volatile boolean ocrAvailable = true;

    @org.springframework.beans.factory.annotation.Value("${file.upload-dir:./uploads/}")
    private String uploadDir;

    /** PDF 解析结果：纯文本 + 内嵌图片（已保存到磁盘） */
    public static class PdfImage {
        public final int pageIndex;   // 0-based
        public final String url;      // 访问路径，如 /uploads/pdfimg/xxx.png

        public PdfImage(int pageIndex, String url) {
            this.pageIndex = pageIndex;
            this.url = url;
        }
    }

    public static class PdfParseResult {
        public String text;
        public final List<String> pageTexts = new ArrayList<>();
        public final List<PdfImage> images = new ArrayList<>();
    }

    public String parseFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("文件名不合法");
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> parsePdf(file.getBytes());
            case "docx", "doc" -> parseDocx(file.getInputStream());
            case "txt", "text", "md" -> new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            case "jpg", "jpeg", "png", "bmp" -> parseImage(file);
            default -> throw new IllegalArgumentException("不支持的文件格式: " + extension);
        };
    }

    /**
     * PDF 文本抽取：按坐标排序 + 保留视觉换行。
     * 对于没有文本层的扫描件，退回 OCR。
     */
    public String parsePdf(byte[] bytes) throws IOException {
        return parsePdfDetailed(bytes).text;
    }

    /**
     * 带图片提取的 PDF 解析：
     * 1. 逐页抽取文本（保持 sortByPosition，页与页之间互不干扰）；
     * 2. 提取每页的内嵌位图（题库里的效果图 / 代码截图），保存到 uploads/pdfimg/ 下；
     * 3. 文本层缺失时回退 OCR（此时无图片关联）。
     */
    public PdfParseResult parsePdfDetailed(byte[] bytes) throws IOException {
        PdfParseResult result = new PdfParseResult();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);      // 关键：按页面坐标排序，恢复阅读顺序
            stripper.setLineSeparator("\n");
            stripper.setAddMoreFormatting(false);
            stripper.setSuppressDuplicateOverlappingText(true);

            StringBuilder all = new StringBuilder();
            int pages = document.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(document);
                result.pageTexts.add(pageText);
                if (i > 0) {
                    all.append('\n');
                }
                all.append(pageText);
            }
            String text = all.toString();

            if (looksLikeScanned(text)) {
                String ocr = ocrDocument(document);
                if (ocr != null && !ocr.isBlank()) {
                    result.text = ocr;
                    return result;
                }
                throw new IOException("该 PDF 为扫描件且未识别出文字，请安装 Tesseract 中文语言包（chi_sim）后重试");
            }
            result.text = text;
            extractEmbeddedImages(document, result);
            return result;
        }
    }

    /** 提取 PDF 每页的内嵌位图，保存为 PNG 并记录访问 URL（同一张图跨页复用只存一次） */
    private void extractEmbeddedImages(PDDocument document, PdfParseResult result) {
        Path dir;
        try {
            String configured = uploadDir == null || uploadDir.isBlank() ? "./uploads/" : uploadDir;
            String cleanUploadDir = configured.replace("./", "").replace(".\\", "");
            dir = Paths.get(System.getProperty("user.dir"), cleanUploadDir, "pdfimg").normalize();
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("创建图片目录失败，跳过 PDF 图片提取: " + e.getMessage());
            return;
        }

        int pageIndex = 0;
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources != null) {
                for (COSName name : resources.getXObjectNames()) {
                    try {
                        if (!(resources.getXObject(name) instanceof PDImageXObject image)) {
                            continue;
                        }
                        // 过滤过小的图（装饰性图标/透明蒙版）
                        if (image.getWidth() < 50 || image.getHeight() < 20) {
                            continue;
                        }
                        BufferedImage buffered = image.getImage();
                        String filename = UUID.randomUUID().toString().replace("-", "") + ".png";
                        javax.imageio.ImageIO.write(buffered, "png", dir.resolve(filename).toFile());
                        result.images.add(new PdfImage(pageIndex, "/uploads/pdfimg/" + filename));
                    } catch (Exception e) {
                        System.err.println("提取 PDF 内嵌图片失败（已跳过）: " + e.getMessage());
                    }
                }
            }
            pageIndex++;
        }
    }

    /** 文本层是否基本为空（扫描件特征） */
    private boolean looksLikeScanned(String text) {
        if (text == null) {
            return true;
        }
        long effective = text.replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}]", "").length();
        return effective < 20;
    }

    private String ocrDocument(PDDocument document) {
        if (!ocrAvailable) {
            return null;
        }
        try {
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            tesseract.setLanguage("chi_sim+eng");
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder sb = new StringBuilder();
            int pages = Math.min(document.getNumberOfPages(), 60);
            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 200, ImageType.GRAY);
                sb.append(tesseract.doOCR(image)).append('\n');
            }
            return sb.toString();
        } catch (Throwable e) {
            ocrAvailable = false;              // 语言包缺失等，后续不再尝试
            System.err.println("OCR 不可用，已跳过: " + e.getMessage());
            return null;
        }
    }

    /** Word：按文档顺序输出段落与表格（表格压成一行文本，方便按行切题） */
    public String parseDocx(InputStream in) throws IOException {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(in)) {
            for (Object element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String paraText = paragraph.getText().trim();
                    if (!paraText.isEmpty()) {
                        text.append(paraText).append('\n');
                    }
                } else if (element instanceof XWPFTable table) {
                    text.append(parseTable(table)).append('\n');
                }
            }
        }
        return text.toString();
    }

    private String parseTable(XWPFTable table) {
        List<String> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = cell.getText().replaceAll("\\s+", " ").trim();
                if (!cellText.isEmpty()) {
                    cells.add(cellText);
                }
            }
            if (!cells.isEmpty()) {
                rows.add(String.join(" | ", cells));
            }
        }
        return String.join(" / ", rows);
    }

    private String parseImage(MultipartFile file) throws IOException {
        if (!ocrAvailable) {
            return "[IMAGE]";
        }
        try {
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            tesseract.setLanguage("chi_sim+eng");
            return tesseract.doOCR(javax.imageio.ImageIO.read(file.getInputStream()));
        } catch (Throwable e) {
            ocrAvailable = false;
            System.err.println("图片 OCR 失败: " + e.getMessage());
            return "[IMAGE]";
        }
    }

    /** 兼容旧调用：文本 -> 题目列表 */
    public List<QuestionData> extractQuestions(String text) {
        QuestionParser.ParseResult result = QuestionParser.parseWithReport(text);
        System.out.println("=== 题目解析完成：共 " + result.getQuestions().size()
                + " 道，题型分布 " + result.getTypeStats()
                + "，跳过 " + result.getDropped() + " 条");
        for (String warning : result.getWarnings()) {
            System.out.println("  [警告] " + warning);
        }
        return result.getQuestions();
    }

    public QuestionParser.ParseResult parseQuestions(String text) {
        return QuestionParser.parseWithReport(text);
    }
}
