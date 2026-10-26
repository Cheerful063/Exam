-- 修改 question 表的 correct_answer 字段
ALTER TABLE `question` MODIFY COLUMN `correct_answer` TEXT NOT NULL;

-- 修改 answer_record 表的 user_answer 字段
ALTER TABLE `answer_record` MODIFY COLUMN `user_answer` TEXT;
