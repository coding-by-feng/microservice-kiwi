-- Kiwi Database Schema
-- Auto-generated from MyBatis Plus entity classes
-- All tables use IF NOT EXISTS for idempotent execution

-- ==================== UPMS Domain ====================

CREATE TABLE IF NOT EXISTS sys_dept (
    dept_id INT AUTO_INCREMENT PRIMARY KEY,
    dept_name VARCHAR(255),
    sort INT,
    is_valid VARCHAR(1),
    parent_id INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(255),
    role_code VARCHAR(255),
    role_desc VARCHAR(255),
    del_flag VARCHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    permission VARCHAR(255),
    path VARCHAR(255),
    parent_id INT,
    icon VARCHAR(255),
    component VARCHAR(255),
    sort INT,
    keep_alive VARCHAR(1),
    type VARCHAR(1),
    del_flag VARCHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255),
    salt VARCHAR(255),
    phone VARCHAR(32),
    avatar VARCHAR(255),
    dept_id INT,
    lock_flag INT DEFAULT 0,
    del_flag INT DEFAULT 0,
    wx_openid VARCHAR(255),
    qq_openid VARCHAR(255),
    google_openid VARCHAR(255),
    email VARCHAR(255),
    real_name VARCHAR(255),
    register_source VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user_role_rel (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== Word Domain ====================

CREATE TABLE IF NOT EXISTS word_main (
    word_id INT AUTO_INCREMENT PRIMARY KEY,
    word_name VARCHAR(255),
    info_type INT,
    in_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_update_time DATETIME,
    is_del CHAR(1) DEFAULT 'N',
    INDEX idx_word_name (word_name),
    INDEX idx_info_type (info_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_character (
    character_id INT AUTO_INCREMENT PRIMARY KEY,
    word_id INT,
    character_code VARCHAR(255),
    tag VARCHAR(255),
    is_del CHAR(1) DEFAULT 'N',
    INDEX idx_word_id (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_paraphrase (
    paraphrase_id INT AUTO_INCREMENT PRIMARY KEY,
    word_id INT,
    character_id INT,
    serial_number INT,
    codes VARCHAR(255),
    paraphrase_english VARCHAR(1000),
    paraphrase_english_translate VARCHAR(1000),
    meaning_chinese VARCHAR(1000),
    translate_language VARCHAR(32),
    is_have_phrase INT DEFAULT 0,
    is_del CHAR(1) DEFAULT 'N',
    INDEX idx_word_id (word_id),
    INDEX idx_character_id (character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_paraphrase_example (
    example_id INT AUTO_INCREMENT PRIMARY KEY,
    word_id INT,
    paraphrase_id INT,
    example_sentence TEXT,
    example_translate TEXT,
    translate_language VARCHAR(32),
    serial_number INT,
    is_del CHAR(1) DEFAULT 'N',
    INDEX idx_word_id (word_id),
    INDEX idx_paraphrase_id (paraphrase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_paraphrase_phrase (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paraphrase_id INT,
    phrase VARCHAR(1000),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_valid INT DEFAULT 1,
    INDEX idx_paraphrase_id (paraphrase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_pronunciation (
    pronunciation_id INT AUTO_INCREMENT PRIMARY KEY,
    word_id INT,
    soundmark VARCHAR(255),
    soundmark_type VARCHAR(32),
    voice_file_path VARCHAR(500),
    group_name VARCHAR(255),
    character_id INT,
    source_url VARCHAR(500),
    is_del CHAR(1) DEFAULT 'N',
    INDEX idx_word_id (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_fetch_queue (
    queue_id INT AUTO_INCREMENT PRIMARY KEY,
    word_name VARCHAR(255),
    word_id INT,
    derivation VARCHAR(255),
    fetch_priority INT DEFAULT 0,
    fetch_status INT DEFAULT 0,
    is_valid VARCHAR(1) DEFAULT 'Y',
    fetch_result TEXT,
    is_lock INT DEFAULT 0,
    in_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    operate_time DATETIME,
    fetch_time INT DEFAULT 0,
    is_into_cache INT DEFAULT 0,
    info_type INT,
    INDEX idx_word_name (word_name),
    INDEX idx_fetch_status (fetch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_main_variant (
    id INT AUTO_INCREMENT PRIMARY KEY,
    word_id INT,
    variant_name VARCHAR(255),
    type INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_valid INT DEFAULT 1,
    remark VARCHAR(500),
    INDEX idx_word_id (word_id),
    INDEX idx_variant_name (variant_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_star_list (
    id INT AUTO_INCREMENT PRIMARY KEY,
    list_name VARCHAR(255),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    owner INT,
    is_del CHAR(1) DEFAULT 'N',
    sort INT DEFAULT 0,
    INDEX idx_owner (owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_star_rel (
    list_id INT NOT NULL,
    word_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_remember INT DEFAULT 0,
    remember_time DATETIME,
    PRIMARY KEY (list_id, word_id),
    INDEX idx_word_id (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_paraphrase_star_list (
    id INT PRIMARY KEY,
    list_name VARCHAR(255),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    owner INT,
    is_del CHAR(1) DEFAULT 'N',
    sort INT DEFAULT 0,
    INDEX idx_owner (owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_paraphrase_star_rel (
    list_id INT NOT NULL,
    paraphrase_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_remember INT DEFAULT 0,
    remember_time DATETIME,
    is_keep_in_mind INT DEFAULT 0,
    keep_in_mind_time DATETIME,
    PRIMARY KEY (list_id, paraphrase_id),
    INDEX idx_paraphrase_id (paraphrase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_example_star_list (
    id INT AUTO_INCREMENT PRIMARY KEY,
    list_name VARCHAR(255),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    owner INT,
    is_del CHAR(1) DEFAULT 'N',
    sort INT DEFAULT 0,
    INDEX idx_owner (owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS word_example_star_rel (
    list_id INT NOT NULL,
    example_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_remember INT DEFAULT 0,
    remember_time DATETIME,
    PRIMARY KEY (list_id, example_id),
    INDEX idx_example_id (example_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS star_rel_his (
    id INT AUTO_INCREMENT PRIMARY KEY,
    word_name VARCHAR(255),
    user_id INT,
    list_id INT,
    type INT,
    in_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    serial_num INT,
    is_del CHAR(1) DEFAULT 'N',
    is_valid INT DEFAULT 1,
    INDEX idx_user_id (user_id),
    INDEX idx_list_id (list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== AI Domain ====================

CREATE TABLE IF NOT EXISTS ai_call_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    ai_url VARCHAR(500),
    prompt TEXT,
    prompt_mode VARCHAR(32),
    target_language VARCHAR(32),
    native_language VARCHAR(32),
    ai_response MEDIUMTEXT,
    timestamp DATETIME,
    is_delete TINYINT(1) DEFAULT 0,
    is_archive TINYINT(1) DEFAULT 0,
    is_favorite TINYINT(1) DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(500),
    prompt TEXT NOT NULL,
    accent VARCHAR(10) NOT NULL DEFAULT 'US',
    duration_minutes INT NOT NULL,
    speaker_count INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_messages INT DEFAULT 0,
    total_audio_duration_ms BIGINT DEFAULT 0,
    favorited TINYINT(1) DEFAULT 0,
    is_del CHAR(1) DEFAULT 'N',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time),
    INDEX idx_status (status),
    INDEX idx_user_favorited (user_id, favorited)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_conversation_speaker (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    speaker_index INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    voice VARCHAR(50) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_conversation_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    speaker_id BIGINT NOT NULL,
    sequence INT NOT NULL,
    text TEXT NOT NULL,
    audio_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    audio_url VARCHAR(500),
    audio_duration_ms INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_speaker_id (speaker_id),
    INDEX idx_sequence (conversation_id, sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ytb_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_link VARCHAR(500),
    channel_name VARCHAR(255),
    status INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    if_valid TINYINT(1) DEFAULT 1,
    INDEX idx_channel_link (channel_link(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ytb_channel_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    channel_id BIGINT,
    status INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    if_valid TINYINT(1) DEFAULT 1,
    INDEX idx_user_id (user_id),
    INDEX idx_channel_id (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ytb_channel_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    channel_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    if_valid TINYINT(1) DEFAULT 1,
    INDEX idx_user_id (user_id),
    INDEX idx_channel_id (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ytb_channel_video (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id BIGINT,
    video_title VARCHAR(500),
    video_link VARCHAR(500),
    published_at DATETIME,
    status INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    if_valid TINYINT(1) DEFAULT 1,
    INDEX idx_channel_id (channel_id),
    INDEX idx_video_link (video_link(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ytb_video_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    video_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    if_valid TINYINT(1) DEFAULT 1,
    INDEX idx_user_id (user_id),
    INDEX idx_video_id (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ytb_video_subtitles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT,
    type INT,
    status INT DEFAULT 0,
    subtitles_text MEDIUMTEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    if_valid TINYINT(1) DEFAULT 1,
    INDEX idx_video_id (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ytb_video_subtitles_translation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subtitles_id BIGINT,
    lang VARCHAR(32),
    translation MEDIUMTEXT,
    type INT,
    status INT DEFAULT 0,
    from_ai TINYINT(1) DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    if_valid TINYINT(1) DEFAULT 1,
    INDEX idx_subtitles_id (subtitles_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== Tools Domain ====================

CREATE TABLE IF NOT EXISTS t_ins_sequence (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stub VARCHAR(1) NOT NULL DEFAULT '',
    UNIQUE KEY uk_stub (stub)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS todo_task (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT,
    title VARCHAR(255),
    description TEXT,
    success_points INT DEFAULT 0,
    fail_points INT DEFAULT 0,
    frequency VARCHAR(32),
    custom_days INT,
    status VARCHAR(32),
    metadata TEXT,
    due_date DATETIME,
    priority INT DEFAULT 0,
    category VARCHAR(64),
    tags VARCHAR(500),
    deleted TINYINT(1) DEFAULT 0,
    deleted_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS todo_history (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT,
    task_id VARCHAR(36),
    title VARCHAR(255),
    description TEXT,
    success_points INT DEFAULT 0,
    fail_points INT DEFAULT 0,
    status VARCHAR(32),
    points_applied INT DEFAULT 0,
    completed_at DATETIME,
    INDEX idx_user_id (user_id),
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS todo_trash (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT,
    title VARCHAR(255),
    description TEXT,
    success_points INT DEFAULT 0,
    fail_points INT DEFAULT 0,
    frequency VARCHAR(32),
    custom_days INT,
    status VARCHAR(32),
    original_date DATETIME,
    deleted_date DATETIME,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tools_project (
    id VARCHAR(36) PRIMARY KEY,
    project_code VARCHAR(64),
    name VARCHAR(255),
    address VARCHAR(500),
    contact_name VARCHAR(255),
    contact_phone VARCHAR(32),
    notes TEXT,
    start_date DATE,
    end_date DATE,
    glass TINYINT(1) DEFAULT 0,
    frame TINYINT(1) DEFAULT 0,
    purchase TINYINT(1) DEFAULT 0,
    transport TINYINT(1) DEFAULT 0,
    install TINYINT(1) DEFAULT 0,
    repair TINYINT(1) DEFAULT 0,
    archived TINYINT(1) DEFAULT 0,
    created_at DATE,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_code (project_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tools_project_photo (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36),
    token VARCHAR(255),
    original_name VARCHAR(500),
    content_type VARCHAR(128),
    size BIGINT,
    storage_path VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS focus_session (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT,
    duration INT,
    tree_type VARCHAR(32),
    potential_points INT DEFAULT 0,
    start_time DATETIME,
    end_time DATETIME,
    status VARCHAR(32),
    completed_at DATETIME,
    cancelled_at DATETIME,
    failed_at DATETIME,
    fail_reason VARCHAR(500),
    points INT DEFAULT 0,
    penalty INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS focus_stats (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT,
    today_trees INT DEFAULT 0,
    today_minutes INT DEFAULT 0,
    current_streak INT DEFAULT 0,
    total_trees INT DEFAULT 0,
    total_minutes INT DEFAULT 0,
    total_points INT DEFAULT 0,
    last_session_date DATE,
    stats_date DATE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS planted_tree (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT,
    session_id VARCHAR(36),
    tree_type VARCHAR(32),
    color VARCHAR(32),
    planted_at DATETIME,
    duration INT,
    points INT DEFAULT 0,
    position INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== Notes Domain ====================

CREATE TABLE IF NOT EXISTS notes_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    name VARCHAR(255),
    description VARCHAR(500),
    color VARCHAR(32),
    icon VARCHAR(64),
    sort_order INT DEFAULT 0,
    is_del CHAR(1) DEFAULT 'N',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notes_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    user_id INT,
    content TEXT,
    display_order INT DEFAULT 0,
    image_prompt TEXT,
    image_style VARCHAR(64),
    image_url VARCHAR(500),
    image_status VARCHAR(32),
    audio_url VARCHAR(500),
    audio_accent VARCHAR(32),
    audio_voice VARCHAR(64),
    audio_duration_ms INT,
    audio_status VARCHAR(32),
    is_del CHAR(1) DEFAULT 'N',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notes_user_lock (
    user_id INT PRIMARY KEY,
    passcode_hash VARCHAR(255),
    is_locked TINYINT(1) DEFAULT 0,
    lock_time DATETIME,
    failed_attempts INT DEFAULT 0,
    last_failed_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
