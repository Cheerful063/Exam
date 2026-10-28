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

    /** 题号：1、1．（中文顿号 / 全角句点） */
    private static final Pattern QNUM_CN = Pattern.compile("^(\\d{1,3})\\s*[、．]\\s*(.*)$");
    /** 题号：1.（半角句点，仅当与上一题号连续（或小节重新从 1 开始）时才认） */
    private static final Pattern QNUM_ASCII = Pattern.compile("^(\\d{1,3})\\.\\s*(.*)$");
    /** 题号被排版到行尾： "在网页中显示图像的HTML标签是 664、" */
    private static final Pattern QNUM_TAIL = Pattern.compile("^(.*\\S)\\s+(\\d{1,3})\\s*[、．]\\s*$");

    private static final Pattern OPTION = Pattern.compile("^([A-H])\\s*[、．]\\s*(.*)$");
    /** 行内多选项标记：'A、1.B、2.C、3.D、0.' / 'B、type: 2.C、type: square'。
     *  不加前置约束，靠「字母连续成序列 + 起始位置」判定，避免空格漏抽导致拆不开。 */
    private static final Pattern INLINE_OPTION = Pattern.compile("([A-H])\\s*[、．.]");

    private static final Pattern ANSWER_LINE =
            Pattern.compile("^[【\\[(（]?\\s*(?:答案|正确答案|参考答案|标准答案)\\s*[】\\])）]?\\s*[：:]\\s*(.*)$"
                    + "|^[【\\[(（]?\\s*(?:正确答案|参考答案|标准答案)\\s*[】\\])）]?\\s*$");
    private static final Pattern ANALYSIS_LINE =
            Pattern.compile("^[【\\[(（]?\\s*(?:解析|分析|答案解析|答案分析|试题解析)\\s*[】\\])）]?\\s*[：:]\\s*(.*)$");
    /** 行内答案（题干与答案被排到同一行） */
    private static final Pattern ANSWER_INLINE =
            Pattern.compile("[【\\[(（]?\\s*(?:答案|正确答案|参考答案)\\s*[】\\])）]?\\s*[：:]\\s*(.*)$");

    /** 题型标题行，如 "一、单选题"、"（二）判断题"；捕获组 1 = 题型名 */
    private static final Pattern TYPE_HEADER = Pattern.compile(
            "^\\s*(?:第?[一二三四五六七八九十]+|[（(][一二三四五六七八九十]+[）)]|\\d{1,2})\\s*[、．.)）]?\\s*"
                    + "(单项选择题|单选题|多项选择题|多选题|不定项选择题|判断题|填空题|简答题|问答题|论述题|程序设计题|综合题|选择题)"
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
            if (!handled && ctx.cur != null) {
                // 粘连题号：上一题答案/题干末尾粘着下一题的题号（期望题号精确匹配）
                handled = trySplitGluedQuestion(ctx, text1);
            }
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
        String sectionType;            // 当前题型小节名（单选题 / 多选题 / 判断题 / 填空题...）
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
            cur.setSectionType(sectionType);
            String c = trimToNull(content);
            if (c != null) {
                cur.setContent(c);
            }
            mode = "stem";
            lastOption = null;
            answerEmpty = false;
        }
    }

    /** 尝试按期望题号切分「上一题末尾 + 下一题题号」的粘连行，成功返回 true */
    private static boolean trySplitGluedQuestion(Ctx ctx, String line) {
        int expect = ctx.lastNumber + 1;
        if (expect < 2 || expect > 999) {
            return false;
        }
        Matcher m = Pattern.compile("(?<=[.。？?！!；;，,）)\\s])" + expect + "\\s*[、．.]\\s*(?=[A-Za-z\\u4e00-\\u9fa5<\\u201c\\u2018])")
                .matcher(line);
        if (!m.find()) {
            return false;
        }
        String head = line.substring(0, m.start()).trim();
        String rest = line.substring(m.end()).trim();
        if (rest.isEmpty()) {
            return false;
        }
        if (!head.isEmpty()) {
            if ("afterAnswer".equals(ctx.mode) || "analysis".equals(ctx.mode)) {
                ctx.cur.setAnalysis(joinText(ctx.cur.getAnalysis(), head));
            } else {
                ctx.cur.setContent(joinText(ctx.cur.getContent(), head));
            }
        }
        ctx.finish();
        ctx.lastNumber = expect;
        ctx.start(rest, expect);
        return true;
    }

    /**
     * 当前题是否处于“孤儿解析”状态：已作答、有解析，而解析实为缺题号新题的题干。
     * 此时一旦再出现答案信号（答案行/裸判断词/选项行），就应把解析提升为新题。
     */
    private static boolean isOrphanAnalysis(Ctx ctx) {
        return "analysis".equals(ctx.mode)
                && ctx.cur.getCorrectAnswer() != null && !ctx.cur.getCorrectAnswer().isEmpty()
                && ctx.cur.getAnalysis() != null && !ctx.cur.getAnalysis().isEmpty();
    }

    /** 把孤儿解析提升为缺题号的新题（仅在 isOrphanAnalysis 为真时调用） */
    private static void promoteOrphanAnalysis(Ctx ctx) {
        if (!isOrphanAnalysis(ctx)) {
            return;
        }
        String stem = ctx.cur.getAnalysis();
        ctx.finish();
        ctx.start(stem, null);
    }

    /** 处理结构化行，返回是否已消费该行 */
    private static boolean handleLine(Ctx ctx, String line, String title) {
        // 题型标题
        Matcher mType = TYPE_HEADER.matcher(line);
        if (mType.matches()) {
            ctx.finish();
            ctx.sectionType = mType.group(1);
            ctx.mode = "idle";
            return true;
        }

        // 判断题：题干下一行直接是 正确/错误，没有“正确答案：”前缀（如蓝桥杯判断题）
        if (ctx.cur != null
                && ("stem".equals(ctx.mode) || isOrphanAnalysis(ctx))
                && ctx.sectionType != null && ctx.sectionType.contains("判断")
                && JUDGE_WORD.matcher(line).matches()) {
            promoteOrphanAnalysis(ctx);
            ctx.cur.setCorrectAnswer(normalizeJudge(line));
            ctx.answerEmpty = false;
            ctx.mode = "afterAnswer";
            return true;
        }

        // 答案行
        Matcher mAns = ANSWER_LINE.matcher(line);
        if (mAns.matches()) {
            if (ctx.cur == null) {
                return true;
            }
            // 已答完的题又来了答案行，说明之前的“解析”是缺题号新题的题干
            promoteOrphanAnalysis(ctx);
            String value = mAns.group(1) == null ? "" : mAns.group(1).trim();
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

        // 半角句点题号：题号连续时生效；题型小节切换后允许重新从 1 开始
        Matcher mAscii = QNUM_ASCII.matcher(line);
        int asciiNum = mAscii.matches() ? parseInt(mAscii.group(1)) : -1;
        if (mAscii.matches() && (asciiNum == ctx.lastNumber + 1 || asciiNum == 1)) {
            ctx.finish();
            ctx.lastNumber = asciiNum;
            ctx.start(mAscii.group(2), asciiNum);
            return true;
        }

        // 一行多个选项（严格：从 A 起连续成序列；宽松：承接上一题最后一个选项的下一字母）
        if (splitInlineOptions(ctx, line)) {
            return true;
        }

        // 单个选项行
        Matcher mOpt = OPTION.matcher(line);
        if (mOpt.matches()) {
            if (ctx.cur == null) {
                ctx.start(null, null);
            }
            if ("analysis".equals(ctx.mode)) {
                boolean answered = ctx.cur.getCorrectAnswer() != null && !ctx.cur.getCorrectAnswer().isEmpty();
                if (answered) {
                    // 上一题已答完：这段“解析”其实是缺题号的新题干，开新题
                    String stem = ctx.cur.getAnalysis();
                    ctx.finish();
                    ctx.start(stem, null);
                } else {
                    // 未答完时的解析其实是题干的一部分（排版错位）
                    ctx.cur.setContent(joinText(ctx.cur.getContent(), ctx.cur.getAnalysis()));
                    ctx.cur.setAnalysis(null);
                }
            }
            ctx.cur.putOption(mOpt.group(1), mOpt.group(2).trim());
            ctx.lastOption = mOpt.group(1);
            ctx.mode = "stem";
            ctx.answerEmpty = false;
            return true;
        }

        return false;
    }

    /** 普通文本：可能是题干续行、选项续行、答案续行或解析 */
    private static void handlePlainText(Ctx ctx, String line) {
        if ("afterAnswer".equals(ctx.mode) && ctx.cur != null) {
            if (ctx.answerEmpty) {
                // 答案写在 "答案：" 之后的多行（代码或长句答案）
                ctx.cur.setCorrectAnswer(joinText(ctx.cur.getCorrectAnswer(), line));
            } else {
                // 答案之后、下一题题号之前的说明文字 → 解析
                ctx.cur.setAnalysis(joinText(ctx.cur.getAnalysis(), line));
                ctx.mode = "analysis";
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

        boolean sectionIsChoice = q.getSectionType() != null
                && (q.getSectionType().contains("选择") || q.getSectionType().contains("单选")
                    || q.getSectionType().contains("多选"));
        boolean judgeOptions = opts.size() == 2
                && isJudgeWord(opts.get("A")) && isJudgeWord(opts.get("B"))
                // 题型小节明确是选择类时，尊重小节（如“单选题”里出现 正确/错误 两选项仍是单选）
                && !sectionIsChoice;

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
            boolean allLetters = !letters.isEmpty()
                    && letters.length() == answer.replaceAll("\\s", "").length();
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

    /**
     * 同一行多选项拆分。
     * 严格模式：'A、1.B、2.C、3.D、0.' —— A 开头、字母连续成序列；
     * 宽松模式：'B、type: 2.C、type: square' —— 承接上一行最后一个选项的下一字母
     * （PDF 抽取时选项前的空格丢失导致两行粘成一行），首个标记前的残留文本并回上一选项。
     */
    private static boolean splitInlineOptions(Ctx ctx, String line) {
        List<int[]> marks = new ArrayList<>();
        List<String> letters = new ArrayList<>();
        Matcher m = INLINE_OPTION.matcher(line);
        while (m.find()) {
            marks.add(new int[]{m.start(), m.end()});
            letters.add(m.group(1));
        }
        if (marks.size() < 2) {
            return false;
        }

        boolean strict = "A".equals(letters.get(0)) && marks.get(0)[0] <= 1 && isConsecutive(letters, 'A');
        boolean loose = false;
        if (!strict && ctx.cur != null && ctx.lastOption != null && "stem".equals(ctx.mode)
                && marks.get(0)[0] <= 1) {
            char next = (char) (ctx.lastOption.charAt(0) + 1);
            loose = letters.get(0).charAt(0) == next && isConsecutive(letters, next);
        }
        if (!strict && !loose) {
            return false;
        }

        if (ctx.cur == null) {
            ctx.start(null, null);
        }
        if (loose && marks.get(0)[0] > 0) {
            String prev = ctx.cur.getOption(ctx.lastOption);
            ctx.cur.putOption(ctx.lastOption, joinText(prev, line.substring(0, marks.get(0)[0]).trim()));
        }
        for (int i = 0; i < marks.size(); i++) {
            int end = i + 1 < marks.size() ? marks.get(i + 1)[0] : line.length();
            ctx.cur.putOption(letters.get(i), line.substring(marks.get(i)[1], end).trim());
        }
        ctx.lastOption = letters.get(letters.size() - 1);
        ctx.mode = "stem";
        ctx.answerEmpty = false;
        return true;
    }

    /** 字母序列是否从 from 开始连续（如 A,B,C） */
    private static boolean isConsecutive(List<String> letters, char from) {
        for (String s : letters) {
            if (s.charAt(0) != from) {
                return false;
            }
            from++;
        }
        return true;
    }

    private static String normalizeJudge(String s) {
        String t = s.trim();
        return (t.contains("错") || "×".equals(t) || "F".equalsIgnoreCase(t) || t.contains("否"))
                ? "错误" : "正确";
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
