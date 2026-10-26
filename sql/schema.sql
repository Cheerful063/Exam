CREATE TABLE IF NOT EXISTS `question_bank` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `file_name` VARCHAR(255) NOT NULL,
    `file_type` VARCHAR(50),
    `file_path` VARCHAR(500),
    `total_questions` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `content` TEXT NOT NULL,
    `option_a` VARCHAR(500),
    `option_b` VARCHAR(500),
    `option_c` VARCHAR(500),
    `option_d` VARCHAR(500),
    `option_e` VARCHAR(500),
    `option_f` VARCHAR(500),
    `option_g` VARCHAR(500),
    `correct_answer` TEXT NOT NULL,
    `analysis` TEXT,
    `question_type` VARCHAR(50) DEFAULT 'single',
    `source_file_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_source_file` (`source_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `answer_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `question_id` BIGINT NOT NULL,
    `user_answer` TEXT,
    `is_correct` TINYINT(1) DEFAULT 0,
    `answer_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;