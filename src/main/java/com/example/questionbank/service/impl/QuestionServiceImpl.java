package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.questionbank.entity.Question;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Override
    public List<Question> getAllQuestions() {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Question::getId);
        return list(wrapper);
    }

    @Override
    public Question getQuestionById(Long id) {
        return getById(id);
    }

    @Override
    public int getTotalCount() {
        return Math.toIntExact(count());
    }
}