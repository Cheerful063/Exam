package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.questionbank.entity.QuestionBank;
import com.example.questionbank.mapper.QuestionBankMapper;
import com.example.questionbank.service.QuestionBankService;
import org.springframework.stereotype.Service;

@Service
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {
}