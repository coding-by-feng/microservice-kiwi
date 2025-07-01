-- AI Call History Table
-- This table stores each AI request made through the WebSocket streaming API
CREATE TABLE `ai_call_history` (
                                   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
                                   `user_id` bigint(20) NOT NULL COMMENT 'User ID who made the request',
                                   `ai_url` varchar(500) DEFAULT NULL COMMENT 'AI URL (endpoint used for the request)',
                                   `prompt` text NOT NULL COMMENT 'The text prompt sent to AI',
                                   `prompt_mode` varchar(100) NOT NULL COMMENT 'AI prompt mode (e.g., DIRECTLY_TRANSLATION, GRAMMAR_EXPLANATION)',
                                   `target_language` varchar(20) NOT NULL COMMENT 'Target language for translation or analysis',
                                   `native_language` varchar(20) DEFAULT NULL COMMENT 'Native language (optional, for dual-language operations)',
                                   `timestamp` datetime DEFAULT NULL COMMENT 'Request timestamp',
                                   `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Soft delete flag (0: not deleted, 1: deleted)',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
                                   `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update time',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_timestamp` (`timestamp`),
                                   KEY `idx_create_time` (`create_time`),
                                   KEY `idx_prompt_mode` (`prompt_mode`),
                                   KEY `idx_target_language` (`target_language`),
                                   KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI call history table - stores each AI request made through the streaming API';

-- Add indexes for better query performance
-- Index for user history queries (most common use case)
CREATE INDEX `idx_user_history` ON `ai_call_history` (`user_id`, `is_delete`, `timestamp` DESC);

-- Index for user history with prompt mode filtering
CREATE INDEX `idx_user_prompt_history` ON `ai_call_history` (`user_id`, `prompt_mode`, `is_delete`, `timestamp` DESC);

-- Index for language-based queries
CREATE INDEX `idx_language_queries` ON `ai_call_history` (`target_language`, `native_language`, `is_delete`, `create_time` DESC);