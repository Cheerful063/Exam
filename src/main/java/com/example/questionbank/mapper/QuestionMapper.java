package com.example.questionbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.questionbank.entity.Question;

import java.util.List;

public interface QuestionMapper extends BaseMapper<Question> {
    List<Question> selectAllOrderById();
}