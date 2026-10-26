package com.example.questionbank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.questionbank.entity.Question;

import java.util.List;

public interface QuestionService extends IService<Question> {
    List<Question> getAllQuestions();
    Question getQuestionById(Long id);
    int getTotalCount();
}