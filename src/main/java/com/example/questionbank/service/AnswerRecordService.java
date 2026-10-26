package com.example.questionbank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.questionbank.entity.AnswerRecord;

import java.util.List;

public interface AnswerRecordService extends IService<AnswerRecord> {
    void saveAnswer(Long questionId, String userAnswer, Boolean isCorrect);
    List<AnswerRecord> getAllRecords();
    void clearAll();
}