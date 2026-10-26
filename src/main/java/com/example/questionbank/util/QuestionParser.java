package com.example.questionbank.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题库文本解析器（纯 JDK 实现，便于脱离 Spring 单独测试）。
 *
 * 设计要点：
 * 1. 以「答案」行为锚点做状态机切题，不依赖题号是否完整；
 * 2. 兼容题号丢失、题号被排到行尾、以及 "1、" / "1．" / "1." 多种编号；
 * 3. 兼容一行多个选项、选项换行续写、题干多行续写；
 * 4. 自动识别 单选 / 多选 / 判断 / 填空 / 简答；
 * 5. 过滤页码、页眉、纯标点等噪声行。
 */
public final class QuestionParser {

    /** 题号：1、 1． （中文顿号 / 全角句点） */
    private static final Pattern QNUM_CN = Pattern.compile("^(\\d{1,3})\\s*[、．]\\s*(.*)$");
    /** 题号：1. （半角句点，仅当与上一题号连续时才认，避免把 "1." 列表项当成题） */
    private static final Pattern QNUM_ASCII = Pattern.compile("^(\\d{1,3})\\.\\s*(.*)$");
    /** 题号被排版到行尾： "在网页中显示图像的HTML标签是 664、" */
    private static final Pattern QNUM_TAIL = Pattern.compile("^(.*\\S)\\s+(\\d{1,3})\\s*[、．]\\s*$");

    private static final Pattern OPTION = Pattern.compile("^([A-H])\\s*[、．]\\s*(.*)$");
    private static final Pattern INLINE_OPTION = Pattern.compile("(?:^|\\s)([A-H])\\s*[、．]\\s*");

    private static final Pattern ANSWER_LINE =
            Pattern.compile("^[【\\[(（]?\\s*(?:答案|正确答案|参考答案|标准答案)\\s*[】\\])）]?\\s*[：:]\\s*(.*)$");
    private static final Pattern ANALYSIS_LINE =
            Pattern.compile("^[【\\[(（]?\\s*(?:解析|分析|答案解析|答案分析|试题解析)\\s*[】\\])）]?\\s*[：:]\\s*(.*)$");
    /** 行内答案（题干与答案被排到同一行） */
    private static final Pattern ANSWER_INLINE =
            Pattern.compile("[【\\[(（]?\\s*(?:答案|正确答案|参考答案)\\s*[】\\])）]?\\s*[：:]\\s*(.*)$");

    /** 题型标题行，如 "一、单选题"、"（二）判断题" */
    private static final Pattern TYPE_HEADER = Pattern.compile(
            "^\\s*(?:第?[一二三四五六七八九十]+|[（(][一二三四五六七八九十]+[）)]|\\d{1,2})\\s*[、．.)）]?\\s*"
                    + "(?:单项选择题|单选题|多项选择题|多选题|不定项选择题|判断题|填空题|简答题|问答题|论述题|程序设计题|综合题|选择题)"
                    + "\\s*$");

    private static final Pattern PAGE_NUM_LINE = Pattern.compile("^[-—–－\\s]*\\d{1,3}[-—–－\\s]*$");
    private static final Pattern PAGE_TEXT_LINE = Pattern.compile("^第?\\s*\\d+\\s*页(?:\\s*[,，/]?\\s*共\\s*\\d+\\s*页)?$");
    private static final Pattern PAGE_SLASH_LINE = Pattern.compile("^\\d+\\s*/\\s*\\d+$");

    private static final Pattern JUDGE_WORD = Pattern.compile("^(正确|错误|对|错|是|否|√|×|T|F)$");
    private static final Pattern BLANK_MARK =
            Pattern.compile("_{2,}|＿{2,}|第\\s*\\[?\\s*\\d+\\s*\\]?\\s*空|（\\s{0,6}）|\\(\\s{0,6}\\)");

    private QuestionParser() {
    }

    /** 解析结果 + 统计信息 */
    public static class ParseResult {
        private final List<QuestionData> questions = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private int dropped;

        public List<QuestionData> getQuestions() {
            return questions;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public int getDropped() {
            return dropped;
        }

        public Map<String, Integer> getTypeStats() {
            Map<String, Integer> stats = new LinkedHashMap<>();
            for (QuestionData q : questions) {
                stats.merge(q.getQuestionType(), 1, Integer::sum);
            }
            return stats;
        }
    }

    public static List<QuestionData> parse(String text) {
        return parseWithReport(text).getQuestions();
    }

    public static ParseResult parseWithReport(String text) {
        ParseResult result = new ParseResult();
        if (text == null || text.isBlank()) {
            return result;
        }

        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').replace('\f', '\n').split("\n");
        Ctx ctx = new Ctx(result);
        String title = null;
        boolean firstLine = true;

        for (String rawLine : lines) {
            String line = cleanLine(rawLine);
            if (line.isEmpty()) {
                continue;
            }

            // 文档标题（通常是页眉，后续会重复出现）：只判断第一行，避免把正文误当页眉
            if (firstLine) {
                firstLine = false;
                if (looksLikeTitle(line)) {
                    title = line;
                    continue;
                }
            }
            if (title != null && line.equals(title)) {
                continue;
            }
            if (isNoise(line)) {
                continue;
            }

            // 题干与答案同一行时先拆分
            String text1 = line;
            String answerPart = null;
            Matcher mInline = ANSWER_INLINE.matcher(line);
            if (mInline.find() && mInline.start() > 0) {
                text1 = line.substring(0, mInline.start()).trim();
                answerPart = mInline.group(1).trim();
            }

            boolean handled = text1.isEmpty() || handleLine(ctx, text1, title);
            if (!handled) {
                handlePlainText(ctx, text1);
            }
            if (answerPart != null && !answerPart.isEmpty() && ctx.cur != null) {
                ctx.cur.setCorrectAnswer(append(ctx.cur.getCorrectAnswer(), answerPart));
                ctx.answerEmpty = false;
                ctx.mode = "afterAnswer";
            }
        }

        ctx.finish();

        // 收尾：清洗、判型、过滤空题
        for (QuestionData q : ctx.collected) {
            q.setContent(trimToNull(q.getContent()));
            q.setAnalysis(trimToNull(q.getAnalysis()));
            if (q.getCorrectAnswer() != null) {
                q.setCorrectAnswer(q.getCorrectAnswer().trim());
            }
            if (q.getContent() == null || q.getContent().length() < 2) {
                result.dropped++;
                continue;
            }
            classify(q);
            if (q.getCorrectAnswer() == null || q.getCorrectAnswer().isEmpty()) {
                result.getWarnings().add("第 " + (q.getNumber() == null ? "?" : q.getNumber())
                        + " 题未解析到答案：" + abbreviate(q.getContent(), 30));
            }
            if (!q.hasOptions() && ("single".equals(q.getQuestionType())
                    || "multiple".equals(q.getQuestionType()))) {
                result.getWarnings().add("第 " + (q.getNumber() == null ? "?" : q.getNumber())
                        + " 题缺少选项：" + abbreviate(q.getContent(), 30));
            }
            result.getQuestions().add(q);
        }
        return result;
    }

    /** 解析过程中的状态 */
    private static final class Ctx {
        final ParseResult result;
        final List<QuestionData> collected = new ArrayList<>();
        QuestionData cur;
        String mode = "idle";          // idle | stem | afterAnswer | analysis
        String lastOption;
        boolean answerEmpty;
        int lastNumber;

        Ctx(ParseResult result) {
            this.result = result;
        }

        void finish() {
            if (cur != null) {
                collected.add(cur);
                cur = null;
            }
        }

        void start(String content, Integer number) {
            cur = new QuestionData();
            cur.setNumber(number);
            String c = trimToNull(content);
            if (c != null) {
                cur.setContent(c);
            }
            mode = "stem";
            lastOption = null;
            answerEmpty = false;
        }
    }

    /** 处理结构化行，返回是否已消费该行 */
    private static boolean handleLine(Ctx ctx, String line, String title) {
        // 题型标题
        if (TYPE_HEADER.matcher(line).matches()) {
            ctx.finish();
            ctx.mode = "idle";
            return true;
        }

        // 答案行
        Matcher mAns = ANSWER_LINE.matcher(line);
        if (mAns.matches()) {
            if (ctx.cur == null) {
                return true;
            }
            String value = mAns.group(1).trim();
            if (value.isEmpty()) {
                ctx.answerEmpty = true;
            } else {
                ctx.cur.setCorrectAnswer(append(ctx.cur.getCorrectAnswer(), value));
                ctx.answerEmpty = false;
            }
            ctx.mode = "afterAnswer";
            return true;
        }

        // 解析行
        Matcher mAna = ANALYSIS_LINE.matcher(line);
        if (mAna.matches()) {
            if (ctx.cur == null) {
                return true;
            }
            ctx.mode = "analysis";
            ctx.cur.setAnalysis(append(ctx.cur.getAnalysis(), mAna.group(1).trim()));
            return true;
        }

        // 题号（顿号 / 全角句点）
        Matcher mNum = QNUM_CN.matcher(line);
        if (mNum.matches()) {
            ctx.finish();
            int n = parseInt(mNum.group(1));
            ctx.lastNumber = n;
            ctx.start(mNum.group(2), n);
            return true;
        }

        // 题号被排到行尾
        Matcher mTail = QNUM_TAIL.matcher(line);
        if (mTail.matches()) {
            ctx.finish();
            int n = parseInt(mTail.group(2));
            ctx.lastNumber = n;
            ctx.start(mTail.group(1).trim(), n);
            return true;
        }

        // 半角句点题号：仅在题号连续时生效
        Matcher mAscii = QNUM_ASCII.matcher(line);
        if (mAscii.matches() && parseInt(mAscii.group(1)) == ctx.lastNumber + 1) {
            ctx.finish();
            int n = parseInt(mAscii.group(1));
            ctx.lastNumber = n;
            ctx.start(mAscii.group(2), n);
            return true;
        }

        // 一行多个选项
        List<String[]> inline = splitInlineOptions(line);
        if (inline != null) {
            if (ctx.cur == null) {
                ctx.start(null, null);
            }
            for (String[] pair : inline) {
                ctx.cur.putOption(pair[0], pair[1]);
            }
            ctx.lastOption = inline.get(inline.size() - 1)[0];
            ctx.mode = "stem";
            ctx.answerEmpty = false;
            return true;
        }

        // 单个选项行
        Matcher mOpt = OPTION.matcher(line);
        if (mOpt.matches()) {
            if (ctx.cur == null) {
                ctx.start(null, null);
            }
            if ("analysis".equals(ctx.mode)) {
                // 说明上一题的“解析”其实是缺题号的新题干
                ctx.cur.setContent(joinText(ctx.cur.getContent(), ctx.cur.getAnalysis()));
                ctx.cur.setAnalysis(null);
            }
            ctx.cur.putOption(mOpt.group(1), mOpt.group(2).trim());
            ctx.lastOption = mOpt.group(1);
            ctx.mode = "stem";
            ctx.answerEmpty = false;
            return true;
        }

        return false;
    }

    /** 普通文本：可能是题干续行、选项续行、答案续行，或一道缺题号的新题 */
    private static void handlePlainText(Ctx ctx, String line) {
        if ("afterAnswer".equals(ctx.mode) && ctx.cur != null) {
            if (ctx.answerEmpty) {
                // 答案写在 "答案：" 之后的多行（代码或长句答案）
                ctx.cur.setCorrectAnswer(joinText(ctx.cur.getCorrectAnswer(), line));
            } else {
                ctx.finish();
                ctx.start(line, null);
            }
            return;
        }
        if ("analysis".equals(ctx.mode) && ctx.cur != null) {
            ctx.cur.setAnalysis(joinText(ctx.cur.getAnalysis(), line));
            return;
        }
        if (ctx.cur == null) {
            ctx.start(line, null);
            return;
        }
        // 续写到最后一个选项，否则续到题干
        if (ctx.lastOption != null && ctx.cur.getOptions().containsKey(ctx.lastOption)) {
            ctx.cur.putOption(ctx.lastOption, joinText(ctx.cur.getOption(ctx.lastOption), line));
        } else {
            ctx.cur.setContent(joinText(ctx.cur.getContent(), line));
        }
    }

    /** 判断题型 */
    private static void classify(QuestionData q) {
        String answer = q.getCorrectAnswer() == null ? "" : q.getCorrectAnswer().trim();
        Map<String, String> opts = q.getOptions();

        boolean judgeOptions = opts.size() == 2
                && isJudgeWord(opts.get("A")) && isJudgeWord(opts.get("B"));

        if (judgeOptions) {
            // 选项只有“正确/错误”两项，等价于判断题，答案可能是字母也可能是文字
            String letter = answer.replaceAll("[^A-Ha-h]", "");
            if (letter.length() == 1) {
                String optionText = opts.get(letter.toUpperCase(Locale.ROOT));
                if (optionText != null) {
                    String t = optionText.trim().replaceAll("[\\s。.．]", "");
                    q.setCorrectAnswer(t.contains("错") || t.contains("否") ? "错误" : "正确");
                }
            }
            opts.clear();
            q.setQuestionType("judge");
            return;
        }

        if (!opts.isEmpty()) {
            String letters = answer.toUpperCase(Locale.ROOT).replaceAll("[^A-H]", "");
            boolean allLetters = !letters.isEmpty() && letters.length() == answer.length();
            q.setQuestionType(allLetters && letters.length() >= 2 ? "multiple" : "single");
            return;
        }

        if (isJudgeWord(answer)) {
            q.setQuestionType("judge");
            q.setCorrectAnswer(answer.contains("错") || "×".equals(answer) || "F".equalsIgnoreCase(answer)
                    ? "错误" : "正确");
            return;
        }

        String content = q.getContent() == null ? "" : q.getContent();
        q.setQuestionType(BLANK_MARK.matcher(content).find() ? "fill" : "essay");
    }

    private static boolean isJudgeWord(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim().replaceAll("[\\s。.．]", "");
        return JUDGE_WORD.matcher(t).matches();
    }

    /** 过滤页码、页眉、纯标点行 */
    private static boolean isNoise(String line) {
        if (PAGE_NUM_LINE.matcher(line).matches()) {
            return true;
        }
        if (PAGE_TEXT_LINE.matcher(line).matches()) {
            return true;
        }
        if (PAGE_SLASH_LINE.matcher(line).matches()) {
            return true;
        }
        return line.replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}]", "").isEmpty();
    }

    /** 首行是否为文档标题（页眉），而不是题目内容 */
    private static boolean looksLikeTitle(String line) {
        if (line.length() > 40) {
            return false;
        }
        if (line.contains("、") || line.contains("答案") || line.contains("（）") || line.contains("()")) {
            return false;
        }
        return !OPTION.matcher(line).matches();
    }

    private static String cleanLine(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\u00A0', ' ').replace('\u3000', ' ').replaceAll("[ \\t]+", " ").trim();
    }

    /** 合并续行：中文直接拼接，两侧都是英文/数字时才补空格 */
    private static String joinText(String a, String b) {
        String left = a == null ? "" : a.trim();
        String right = b == null ? "" : b.trim();
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        boolean bothAscii = isAsciiWordChar(left.charAt(left.length() - 1)) && isAsciiWordChar(right.charAt(0));
        return bothAscii ? left + " " + right : left + right;
    }

    /** 多段内容用空格连接（答案、解析） */
    private static String append(String a, String b) {
        String left = a == null ? "" : a.trim();
        String right = b == null ? "" : b.trim();
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + " " + right;
    }

    private static boolean isAsciiWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    /** "A、01 B、02 C、03" 这类同一行多选项的拆分 */
    private static List<String[]> splitInlineOptions(String line) {
        List<int[]> marks = new ArrayList<>();
        List<String> letters = new ArrayList<>();
        Matcher m = INLINE_OPTION.matcher(line);
        while (m.find()) {
            marks.add(new int[]{m.start(), m.end()});
            letters.add(m.group(1));
        }
        if (marks.size() < 2 || !"A".equals(letters.get(0)) || marks.get(0)[0] > 2) {
            return null;
        }
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < marks.size(); i++) {
            int end = i + 1 < marks.size() ? marks.get(i + 1)[0] : line.length();
            out.add(new String[]{letters.get(i), line.substring(marks.get(i)[1], end).trim()});
        }
        return out;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String abbreviate(String s, int len) {
        if (s == null) {
            return "";
        }
        return s.length() <= len ? s : s.substring(0, len) + "...";
    }
}
