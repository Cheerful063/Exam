package com.example.questionbank.controller;

import com.example.questionbank.entity.Question;
import com.example.questionbank.service.AnswerRecordService;
import com.example.questionbank.service.QuestionBankService;
import com.example.questionbank.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private QuestionBankService questionBankService;
    
    @Autowired
    private AnswerRecordService answerRecordService;

    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionService.getAllQuestions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getQuestionCount() {
        Map<String, Integer> count = new HashMap<>();
        count.put("total", questionService.getTotalCount());
        return ResponseEntity.ok(count);
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAll() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            answerRecordService.clearAll();
            questionService.remove(null);
            questionBankService.remove(null);
            
            response.put("success", true);
            response.put("message", "已清空所有题目信息");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "清空失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}