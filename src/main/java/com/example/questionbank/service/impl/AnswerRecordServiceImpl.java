package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.questionbank.entity.AnswerRecord;
import com.example.questionbank.mapper.AnswerRecordMapper;
import com.example.questionbank.service.AnswerRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnswerRecordServiceImpl extends ServiceImpl<AnswerRecordMapper, AnswerRecord> implements AnswerRecordService {

    @Override
    public void saveAnswer(Long questionId, String userAnswer, Boolean isCorrect) {
        // 先删除已存在的答案（支持重答）
        LambdaQueryWrapper<AnswerRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnswerRecord::getQuestionId, questionId);
        remove(wrapper);
        
        AnswerRecord record = new AnswerRecord();
        record.setQuestionId(questionId);
        record.setUserAnswer(userAnswer);
        record.setIsCorrect(isCorrect ? 1 : 0);
        save(record);
    }

    @Override
    public List<AnswerRecord> getAllRecords() {
        return list();
    }

    @Override
    public void clearAll() {
        remove(new LambdaQueryWrapper<>());
    }
}