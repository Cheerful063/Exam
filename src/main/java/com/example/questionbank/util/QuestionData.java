package com.example.questionbank.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单条题目的解析结果。
 */
public class QuestionData {

    /** 原文题号，可能为 null（部分 PDF 题号丢失或排版错位） */
    private Integer number;
    /** 题干（多项续行已合并） */
    private String content;
    /** 选项，键为 A/B/C/D/E/F/G，按出现顺序 */
    private final Map<String, String> options = new LinkedHashMap<>();
    /** 答案原文，例如 "D"、"ABD"、"正确"、"5050" */
    private String correctAnswer;
    /** 解析 */
    private String analysis;
    /** single / multiple / judge / fill / essay */
    private String questionType;
    /** PDF 内嵌图片（如填空题的效果图），保存后的访问 URL 列表 */
    private final List<String> imageUrls = new ArrayList<>();
    /** 所属题型小节名（单选题/判断题...），仅解析期使用 */
    private String sectionType;

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void addImageUrl(String url) {
        if (url != null && !url.isEmpty()) {
            imageUrls.add(url);
        }
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String getOption(String letter) {
        if (letter == null) {
            return null;
        }
        return options.get(letter.toUpperCase());
    }

    public void putOption(String letter, String text) {
        if (letter == null) {
            return;
        }
        options.put(letter.toUpperCase(), text == null ? "" : text);
    }

    public boolean hasOptions() {
        return !options.isEmpty();
    }

    public String getOptionA() {
        return getOption("A");
    }

    public String getOptionB() {
        return getOption("B");
    }

    public String getOptionC() {
        return getOption("C");
    }

    public String getOptionD() {
        return getOption("D");
    }

    public String getOptionE() {
        return getOption("E");
    }

    public String getOptionF() {
        return getOption("F");
    }

    public String getOptionG() {
        return getOption("G");
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    @Override
    public String toString() {
        return "Q" + number + "[" + questionType + "] " + content + " opts=" + options
                + " ans=" + correctAnswer;
    }
}
