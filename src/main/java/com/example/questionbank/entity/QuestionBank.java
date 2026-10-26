package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_bank")
public class QuestionBank {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String fileType;

    private String filePath;

    private Integer totalQuestions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}