CREATE TABLE `ytb_channel` (
                               `id` int NOT NULL COMMENT 'Primary key',
                               `channel_link` varchar(255) NOT NULL COMMENT 'YouTube channel link',
                               `channel_name` varchar(100) NOT NULL COMMENT 'YouTube channel name',
                               `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0-ready, 1-processing, 2-finish',
                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
                               `if_valid` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If the record is valid: 1-valid, 0-invalid',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_channel_link` (`channel_link`),
                               KEY `idx_channel_name` (`channel_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='YouTube channel information';

CREATE TABLE `ytb_channel_user` (
                                    `id` int NOT NULL COMMENT 'Primary key',
                                    `user_id` int NOT NULL COMMENT 'User ID',
                                    `channel_id` int NOT NULL COMMENT 'Channel ID',
                                    `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0-ready, 1-processing, 2-finish',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
                                    `if_valid` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If the record is valid: 1-valid, 0-invalid',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_channel` (`user_id`,`channel_id`),
                                    KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-channel subscription relationship';

CREATE TABLE `ytb_channel_video` (
                                     `id` int NOT NULL COMMENT 'Primary key',
                                     `channel_id` int NOT NULL COMMENT 'Channel ID',
                                     `video_title` varchar(500) NOT NULL COMMENT 'Video title',
                                     `video_link` varchar(100) NOT NULL COMMENT 'Video link',
                                     `published_at` datetime NULL COMMENT 'Video publication time',
                                     `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0-ready, 1-processing, 2-finish',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
                                     `if_valid` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If the record is valid: 1-valid, 0-invalid',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_video_link` (`video_link`),
                                     KEY `idx_channel_id` (`channel_id`),
                                     KEY `idx_channel_published` (`channel_id`, `published_at`),
                                     KEY `idx_published` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='YouTube video information';

CREATE TABLE `ytb_video_subtitles` (
                                       `id` int NOT NULL COMMENT 'Primary key',
                                       `video_id` int NOT NULL COMMENT 'Video ID',
                                       `type` tinyint(1) NOT NULL COMMENT 'Subtitles type: 1-professional, 2-auto-generated',
                                       `subtitles_text` longtext NOT NULL COMMENT 'Subtitles text content',
                                       `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0-ready, 1-processing, 2-finish',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
                                       `if_valid` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If the record is valid: 1-valid, 0-invalid',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Video subtitles';

CREATE TABLE `ytb_video_subtitles_translation` (
                                                   `id` int NOT NULL COMMENT 'Primary key',
                                                   `subtitles_id` int NOT NULL COMMENT 'Subtitles ID',
                                                   `lang` varchar(10) NOT NULL COMMENT 'Language code (ISO standard)',
                                                   `translation` longtext NOT NULL COMMENT 'Translation content',
                                                   `type` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Type: 1-hasn''t-proofread-yet, 2-proofread',
                                                   `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0-ready, 1-processing, 2-finish',
                                                   `from_ai` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If the translation is from AI: 1-yes, 0-no',
                                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
                                                   `if_valid` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If the record is valid: 1-valid, 0-invalid',
                                                   PRIMARY KEY (`id`),
                                                   UNIQUE KEY `uk_subtitles_lang` (`subtitles_id`,`lang`),
                                                   KEY `idx_subtitles_id` (`subtitles_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Subtitles translation';

-- New: favorites tables
CREATE TABLE `ytb_channel_favorite` (
    `id` int NOT NULL COMMENT 'Primary key',
    `user_id` int NOT NULL COMMENT 'User ID',
    `channel_id` int NOT NULL COMMENT 'Channel ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    `if_valid` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If the record is valid: 1-valid, 0-invalid',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_channel_fav` (`user_id`,`channel_id`),
    KEY `idx_fav_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User favorites for channels';

CREATE TABLE `ytb_video_favorite` (
    `id` int NOT NULL COMMENT 'Primary key',
    `user_id` int NOT NULL COMMENT 'User ID',
    `video_id` int NOT NULL COMMENT 'Video ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    `if_valid` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If the record is valid: 1-valid, 0-invalid',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_video_fav` (`user_id`,`video_id`),
    KEY `idx_fav_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User favorites for videos';
