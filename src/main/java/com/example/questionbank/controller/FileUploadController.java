package com.example.questionbank.controller;

import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.QuestionBank;
import com.example.questionbank.service.AnswerRecordService;
import com.example.questionbank.service.QuestionBankService;
import com.example.questionbank.service.QuestionService;
import com.example.questionbank.util.FileParserUtil;
import com.example.questionbank.util.QuestionData;
import com.example.questionbank.util.QuestionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /** 导入串行锁：保证「查重 -> 入库」的原子性 */
    private static final Object IMPORT_LOCK = new Object();

    @Autowired
    private FileParserUtil fileParserUtil;
    
    @Value("${file.upload-dir:./uploads/}")
    private String uploadDir;
    
    private Path getUploadPath() {
        String userDir = System.getProperty("user.dir");
        String cleanUploadDir = uploadDir.replace("./", "").replace(".\\", "");
        return Paths.get(userDir, cleanUploadDir).normalize();
    }

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerRecordService answerRecordService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replace", defaultValue = "false") boolean replace) {
        Map<String, Object> response = new HashMap<>();

        try {
            String text = fileParserUtil.parseFile(file);
            QuestionParser.ParseResult parseResult = fileParserUtil.parseQuestions(text);
            List<QuestionData> questions = parseResult.getQuestions();

            if (questions.isEmpty()) {
                response.put("success", false);
                response.put("message", "没有解析出任何题目，请确认文件格式（题目需包含「1、题干」和「答案：X」）");
                return ResponseEntity.badRequest().body(response);
            }

            // 统计数据：inserted / updated / skipped，由 lambda 回填
            final int[] stats = new int[3];

            // 事务必须包含在锁内：否则锁释放时数据还未提交，并发请求查重仍会读不到，导致重复入库。
            synchronized (IMPORT_LOCK) {
                transactionTemplate.executeWithoutResult(status -> {
                    if (replace) {
                        // 覆盖导入：清掉旧题库和答题记录，避免历史上解析错误的题目残留
                        answerRecordService.clearAll();
                        questionService.remove(null);
                        questionBankService.remove(null);
                    }

                    // 已存在的题目：按题干匹配，已存在则修正题型/选项/答案，不重复插入
                    Map<String, Question> existing = new HashMap<>();
                    for (Question question : questionService.getAllQuestions()) {
                        existing.putIfAbsent(normalizeForCompare(question.getContent()), question);
                    }

                    QuestionBank bank = new QuestionBank();
                    bank.setFileName(file.getOriginalFilename());
                    bank.setFileType(getFileType(file.getOriginalFilename()));
                    bank.setTotalQuestions(questions.size());
                    questionBankService.save(bank);

                    int inserted = 0;
                    int updated = 0;
                    int skipped = 0;
                    for (QuestionData qd : questions) {
                        String key = normalizeForCompare(qd.getContent());
                        Question question = existing.get(key);
                        if (question != null) {
                            if (fillQuestion(question, qd)) {
                                questionService.updateById(question);
                                updated++;
                            } else {
                                skipped++;
                            }
                            continue;
                        }
                        question = new Question();
                        fillQuestion(question, qd);
                        question.setSourceFileId(bank.getId());
                        questionService.save(question);
                        existing.put(key, question);
                        inserted++;
                    }
                    stats[0] = inserted;
                    stats[1] = updated;
                    stats[2] = skipped;
                });
            }

            int inserted = stats[0];
            int updated = stats[1];
            int skipped = stats[2];

            response.put("success", true);
            response.put("message", "解析出 " + questions.size() + " 道题，新增 " + inserted
                    + " 道，更新 " + updated + " 道" + (skipped > 0 ? "，跳过 " + skipped + " 道已存在的题" : ""));
            response.put("count", inserted);
            response.put("parsed", questions.size());
            response.put("skipped", skipped);
            response.put("updated", updated);
            response.put("types", parseResult.getTypeStats());
            response.put("warnings", limitWarnings(parseResult.getWarnings()));
            return ResponseEntity.ok(response);

        } catch (IOException | IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "文件解析失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /** 把解析结果写入实体，返回内容是否有变化 */
    private boolean fillQuestion(Question question, QuestionData qd) {
        String answer = qd.getCorrectAnswer() == null ? "" : qd.getCorrectAnswer();
        boolean changed = !answer.equals(question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer())
                || !eq(qd.getQuestionType(), question.getQuestionType())
                || !eq(qd.getOptionA(), question.getOptionA())
                || !eq(qd.getOptionB(), question.getOptionB())
                || !eq(qd.getOptionC(), question.getOptionC())
                || !eq(qd.getOptionD(), question.getOptionD())
                || !eq(qd.getOptionE(), question.getOptionE())
                || !eq(qd.getOptionF(), question.getOptionF())
                || !eq(qd.getOptionG(), question.getOptionG())
                || !eq(qd.getAnalysis(), question.getAnalysis())
                || !eq(qd.getContent(), question.getContent());

        question.setContent(qd.getContent());
        question.setOptionA(qd.getOptionA());
        question.setOptionB(qd.getOptionB());
        question.setOptionC(qd.getOptionC());
        question.setOptionD(qd.getOptionD());
        question.setOptionE(qd.getOptionE());
        question.setOptionF(qd.getOptionF());
        question.setOptionG(qd.getOptionG());
        question.setCorrectAnswer(answer);
        question.setAnalysis(qd.getAnalysis());
        question.setQuestionType(qd.getQuestionType());
        return changed;
    }

    private boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private String normalizeForCompare(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}]", "");
    }

    private String limitWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "";
        }
        int limit = Math.min(warnings.size(), 8);
        String joined = String.join("；", warnings.subList(0, limit));
        return warnings.size() > limit ? joined + "；等共 " + warnings.size() + " 条" : joined;
    }
    
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String text = fileParserUtil.parseFile(file);
            QuestionParser.ParseResult parseResult = fileParserUtil.parseQuestions(text);
            response.put("success", true);
            response.put("text", text);
            response.put("parsed", parseResult.getQuestions().size());
            response.put("types", parseResult.getTypeStats());
            response.put("warnings", limitWarnings(parseResult.getWarnings()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    private String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "other";
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "pdf";
            case "docx" -> "word";
            case "jpg", "jpeg", "png" -> "image";
            default -> "other";
        };
    }
    
    @PostMapping("/image-question")
    public ResponseEntity<Map<String, Object>> uploadImageQuestion(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionType", defaultValue = "essay") String questionType,
            @RequestParam(value = "correctAnswer", required = false) String correctAnswer) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String originalFilename = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
            String extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                    : "";
            
            if (!extension.matches("jpg|jpeg|png")) {
                response.put("success", false);
                response.put("message", "只支持JPG/JPEG/PNG格式的图片");
                return ResponseEntity.badRequest().body(response);
            }
            
            Path uploadPath = getUploadPath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String newFilename = UUID.randomUUID().toString() + "." + extension;
            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());
            
            String imageUrl = "/uploads/" + newFilename;
            String content = "<img src=\"" + imageUrl + "\" alt=\"题目图片\" style=\"max-width:100%;height:auto;\" />";
            
            QuestionBank bank = new QuestionBank();
            bank.setFileName("图片题目_" + originalFilename);
            bank.setFileType("image");
            bank.setTotalQuestions(1);
            questionBankService.save(bank);
            
            Question question = new Question();
            question.setContent(content);
            question.setCorrectAnswer(correctAnswer != null ? correctAnswer : "");
            question.setQuestionType(questionType);
            question.setSourceFileId(bank.getId());
            questionService.save(question);
            
            response.put("success", true);
            response.put("message", "图片题目上传成功");
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "图片上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/manual-text")
    public ResponseEntity<Map<String, Object>> addManualQuestionText(
            @RequestParam("content") String content,
            @RequestParam("questionType") String questionType,
            @RequestParam("correctAnswer") String correctAnswer) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            QuestionBank bank = new QuestionBank();
            bank.setFileName("手动添加题目");
            bank.setFileType("manual");
            bank.setTotalQuestions(1);
            questionBankService.save(bank);
            
            Question question = new Question();
            question.setContent(content);
            question.setCorrectAnswer(correctAnswer);
            question.setQuestionType(questionType);
            question.setSourceFileId(bank.getId());
            questionService.save(question);
            
            response.put("success", true);
            response.put("message", "题目添加成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> addManualQuestion(
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("questionType") String questionType,
            @RequestParam("correctAnswer") String correctAnswer,
            @RequestParam(value = "optionA", required = false) String optionA,
            @RequestParam(value = "optionB", required = false) String optionB,
            @RequestParam(value = "optionC", required = false) String optionC,
            @RequestParam(value = "optionD", required = false) String optionD) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String questionContent = content;
            
            if (file != null && !file.isEmpty()) {
                String originalFilename = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
                String extension = originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                        : "";
                
                if (!extension.matches("jpg|jpeg|png")) {
                    response.put("success", false);
                    response.put("message", "只支持JPG/JPEG/PNG格式的图片");
                    return ResponseEntity.badRequest().body(response);
                }
                
                Path uploadPath = getUploadPath();
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                String newFilename = UUID.randomUUID().toString() + "." + extension;
                Path filePath = uploadPath.resolve(newFilename);
                file.transferTo(filePath.toFile());
                
                String imageUrl = "/uploads/" + newFilename;
                questionContent = "<img src=\"" + imageUrl + "\" alt=\"题目图片\" style=\"max-width:100%;height:auto;\" />";
                
                if (content != null && !content.isEmpty()) {
                    questionContent = content + "<br/>" + questionContent;
                }
            }
            
            QuestionBank bank = new QuestionBank();
            bank.setFileName("手动添加题目");
            bank.setFileType("manual");
            bank.setTotalQuestions(1);
            questionBankService.save(bank);
            
            Question question = new Question();
            question.setContent(questionContent);
            question.setOptionA(optionA);
            question.setOptionB(optionB);
            question.setOptionC(optionC);
            question.setOptionD(optionD);
            question.setCorrectAnswer(correctAnswer);
            question.setQuestionType(questionType);
            question.setSourceFileId(bank.getId());
            questionService.save(question);
            
            response.put("success", true);
            response.put("message", "题目添加成功");
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "添加失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}