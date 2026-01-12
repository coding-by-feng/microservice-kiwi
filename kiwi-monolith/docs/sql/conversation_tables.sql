-- AI Conversation Generator Tables
-- Run this script to create the conversation-related tables

-- =====================================================
-- Table: ai_conversation
-- Main conversation metadata
-- =====================================================
CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'User ID who created the conversation',
    topic VARCHAR(500) NULL COMMENT 'Generated topic title (set after script generation)',
    prompt TEXT NOT NULL COMMENT 'Original user prompt/topic description',
    accent VARCHAR(10) NOT NULL DEFAULT 'US' COMMENT 'Accent: US, UK, AU, IN',
    duration_minutes INT NOT NULL COMMENT 'Target duration in minutes',
    speaker_count INT NOT NULL COMMENT 'Number of speakers (2-4)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, GENERATING_SCRIPT, GENERATING_AUDIO, COMPLETED, FAILED',
    total_messages INT DEFAULT 0 COMMENT 'Total number of messages',
    total_audio_duration_ms BIGINT DEFAULT 0 COMMENT 'Total audio duration in milliseconds',
    is_del CHAR(1) DEFAULT 'N' COMMENT 'Soft delete flag: N=active, Y=deleted',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI Conversation metadata';

-- =====================================================
-- Table: ai_conversation_speaker
-- Speakers in a conversation
-- =====================================================
CREATE TABLE IF NOT EXISTS ai_conversation_speaker (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT 'Reference to ai_conversation',
    speaker_index INT NOT NULL COMMENT 'Speaker index (0, 1, 2, 3)',
    name VARCHAR(100) NOT NULL COMMENT 'Speaker name (character/role name used in conversation)',
    voice VARCHAR(50) NOT NULL COMMENT 'TTS voice (alloy, echo, fable, etc.)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    CONSTRAINT fk_speaker_conversation FOREIGN KEY (conversation_id)
        REFERENCES ai_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Conversation speakers';

-- =====================================================
-- Table: ai_conversation_message
-- Individual messages in a conversation
-- =====================================================
CREATE TABLE IF NOT EXISTS ai_conversation_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT 'Reference to ai_conversation',
    speaker_id BIGINT NOT NULL COMMENT 'Reference to ai_conversation_speaker',
    sequence INT NOT NULL COMMENT 'Message sequence number (1, 2, 3...)',
    text TEXT NOT NULL COMMENT 'Message text content',
    audio_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, GENERATING, READY, FAILED',
    audio_url VARCHAR(500) COMMENT 'FTP path to audio file',
    audio_duration_ms INT DEFAULT 0 COMMENT 'Audio duration in milliseconds',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_speaker_id (speaker_id),
    INDEX idx_sequence (conversation_id, sequence),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES ai_conversation(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_speaker FOREIGN KEY (speaker_id)
        REFERENCES ai_conversation_speaker(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Conversation messages';

-- =====================================================
-- Optional: Add indexes for common queries
-- =====================================================
-- Index for getting user's recent conversations
CREATE INDEX idx_user_recent ON ai_conversation(user_id, create_time DESC);

-- Index for finding incomplete conversations (for cleanup)
CREATE INDEX idx_incomplete ON ai_conversation(status, create_time);
