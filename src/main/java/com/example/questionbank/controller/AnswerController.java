package com.example.questionbank.controller;

import com.example.questionbank.entity.AnswerRecord;
import com.example.questionbank.entity.Question;
import com.example.questionbank.service.AnswerRecordService;
import com.example.questionbank.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/answers")
public class AnswerController {

    @Autowired
    private AnswerRecordService answerRecordService;

    @Autowired
    private QuestionService questionService;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestBody Map<String, String> request) {
        Long questionId = parseId(request.get("questionId"));
        if (questionId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "questionId 缺失或格式不正确");
            return ResponseEntity.badRequest().body(error);
        }
        String userAnswer = request.get("userAnswer");

        Question question = questionService.getQuestionById(questionId);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isCorrect = checkAnswer(question, userAnswer);
        answerRecordService.saveAnswer(questionId, userAnswer, isCorrect);

        Map<String, Object> response = new HashMap<>();
        response.put("isCorrect", isCorrect);
        response.put("correctAnswer", question.getCorrectAnswer());
        response.put("analysis", question.getAnalysis());

        return ResponseEntity.ok(response);
    }

    /** questionId 容错解析：null / 空串 / 非数字 均返回 null（交由上层回 400） */
    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean checkAnswer(Question question, String userAnswer) {
        String correctAnswer = question.getCorrectAnswer();
        String questionType = question.getQuestionType();

        if (correctAnswer == null) {
            correctAnswer = "";
        }
        if (userAnswer == null) {
            userAnswer = "";
        }

        // 题库本身没有标准答案（源文件缺失）：任何作答都算完成，避免出现无法答对的题
        if (correctAnswer.isBlank()) {
            return true;
        }

        if ("single".equals(questionType)) {
            return correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
        }
        if ("multiple".equals(questionType)) {
            // 多选：答案按字母集合比较，与顺序、分隔符无关（"ACD" == "ADC"）
            String expected = sortsLetters(correctAnswer);
            String actual = sortsLetters(userAnswer);
            return !expected.isEmpty() && expected.equals(actual);
        }
        if ("judge".equals(questionType)) {
            return isJudgeAnswerCorrect(correctAnswer, userAnswer);
        }
        // 填空 / 简答：忽略标点、空白、大小写的宽松比较
        return isTextAnswerCorrect(correctAnswer, userAnswer);
    }

    private String sortsLetters(String answer) {
        if (answer == null) {
            return "";
        }
        char[] letters = answer.toUpperCase().replaceAll("[^A-H]", "").toCharArray();
        java.util.Arrays.sort(letters);
        return new String(letters);
    }

    /** 文字类答案：完全一致 / 互相包含 / 插入删除距离足够小 均判对 */
    private boolean isTextAnswerCorrect(String correctAnswer, String userAnswer) {
        String expected = normalizeAnswer(correctAnswer);
        String actual = normalizeAnswer(userAnswer);
        if (expected.isEmpty()) {
            // 题库本身没有标准答案（如部分编程题），练习过即视为完成
            return true;
        }
        if (actual.isEmpty()) {
            return false;
        }
        if (expected.equals(actual)) {
            return true;
        }
        if (expected.contains(actual) || actual.contains(expected)) {
            return true;
        }
        int distance = editDistance(expected, actual);
        double similarity = 1.0 - (double) distance / Math.max(expected.length(), actual.length());
        return similarity >= 0.75;
    }

    private int editDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }
    
    private boolean isJudgeAnswerCorrect(String correctAnswer, String userAnswer) {
        Set<String> trueAnswers = Set.of("对", "正确", "是", "√", "T", "TRUE", "Y", "YES", "1", "true");
        Set<String> falseAnswers = Set.of("错", "错误", "否", "×", "F", "FALSE", "N", "NO", "0", "false");
        
        String normalizedCorrect = normalizeJudgeAnswer(correctAnswer);
        String normalizedUser = normalizeJudgeAnswer(userAnswer);
        
        boolean correctIsTrue = trueAnswers.contains(normalizedCorrect);
        boolean userIsTrue = trueAnswers.contains(normalizedUser);
        // 标准答案无法识别为对/错时（如源文件标注异常），不做惩罚
        if (!correctIsTrue && !falseAnswers.contains(normalizedCorrect)) {
            return true;
        }

        return correctIsTrue == userIsTrue;
    }
    
    private String normalizeJudgeAnswer(String answer) {
        if (answer == null) return "";
        return answer.trim().replaceAll("[\\s\\p{Punct}]", "").toUpperCase();
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) return "";
        return answer.toLowerCase()
                .replaceAll("[，。、；：''\"\"（）\\[\\]{}]", "")
                .replaceAll("[,.;:''\"\"\\(\\)\\[\\]{}]", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getProgress() {
        List<AnswerRecord> records = answerRecordService.getAllRecords();
        List<Question> allQuestions = questionService.getAllQuestions();
        
        Map<Long, AnswerRecord> answeredMap = new HashMap<>();
        for (AnswerRecord record : records) {
            answeredMap.put(record.getQuestionId(), record);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("allQuestions", allQuestions.size());
        response.put("answered", answeredMap.size());
        response.put("progress", answeredMap);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearProgress() {
        answerRecordService.clearAll();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
