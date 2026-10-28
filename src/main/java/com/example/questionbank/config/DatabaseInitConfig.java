package com.example.questionbank.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitConfig implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("检查数据库表结构...");
            
            // 检查 question 表的 correct_answer 字段长度
            Integer columnLength = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'question' " +
                "AND COLUMN_NAME = 'correct_answer'", 
                Integer.class
            );
            
            if (columnLength != null && columnLength > 0) {
                System.out.println("question.correct_answer 字段长度: " + columnLength);
                
                // 如果不是 TEXT 类型（长度小于等于10），则更新为 TEXT
                if (columnLength <= 10) {
                    System.out.println("正在更新 question.correct_answer 字段为 TEXT 类型...");
                    jdbcTemplate.execute("ALTER TABLE `question` MODIFY COLUMN `correct_answer` TEXT NOT NULL");
                    System.out.println("question.correct_answer 字段更新成功！");
                }
            }
            
            // 检查 answer_record 表的 user_answer 字段
            Integer userAnswerLength = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'answer_record' " +
                "AND COLUMN_NAME = 'user_answer'", 
                Integer.class
            );
            
            if (userAnswerLength != null && userAnswerLength > 0) {
                System.out.println("answer_record.user_answer 字段长度: " + userAnswerLength);
                
                if (userAnswerLength <= 10) {
                    System.out.println("正在更新 answer_record.user_answer 字段为 TEXT 类型...");
                    jdbcTemplate.execute("ALTER TABLE `answer_record` MODIFY COLUMN `user_answer` TEXT");
                    System.out.println("answer_record.user_answer 字段更新成功！");
                }
            }
            
            System.out.println("数据库表结构检查完成！");

            ensureOptionColumns(jdbcTemplate);

        } catch (Exception e) {
            System.out.println("数据库表结构检查/更新失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 扩展选项列（E/F/G），支持多于四个选项的选择题；以及 PDF 图片列 */
    private void ensureOptionColumns(JdbcTemplate jdbcTemplate) {
        String[] columns = {"option_e", "option_f", "option_g"};
        for (String column : columns) {
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND COLUMN_NAME = ?",
                Integer.class, column
            );
            if (exists == null || exists == 0) {
                System.out.println("正在为 question 表添加字段 " + column + " ...");
                jdbcTemplate.execute("ALTER TABLE `question` ADD COLUMN `" + column
                        + "` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL AFTER `option_d`");
            }
        }
        ensureImagesColumn(jdbcTemplate);
    }

    /** PDF 内嵌图片列（存 URL，逗号分隔） */
    private void ensureImagesColumn(JdbcTemplate jdbcTemplate) {
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND COLUMN_NAME = 'images'",
            Integer.class
        );
        if (exists == null || exists == 0) {
            System.out.println("正在为 question 表添加字段 images ...");
            jdbcTemplate.execute("ALTER TABLE `question` ADD COLUMN `images` TEXT NULL DEFAULT NULL AFTER `analysis`");
        }
    }
}
