create table ai_call_history
(
    id              bigint auto_increment comment 'Primary key'
        primary key,
    user_id         bigint                               not null comment 'User ID who made the request',
    ai_url          varchar(500)                         null comment 'AI URL (endpoint used for the request)',
    prompt          varchar(2000)                        not null comment 'The text prompt sent to AI',
    prompt_mode     varchar(100)                         not null comment 'AI prompt mode (e.g., DIRECTLY_TRANSLATION, GRAMMAR_EXPLANATION)',
    target_language varchar(20)                          not null comment 'Target language for translation or analysis',
    native_language varchar(20)                          null comment 'Native language (optional, for dual-language operations)',
    timestamp       datetime                             null comment 'Request timestamp',
    is_delete       tinyint(1) default 0                 not null comment 'Soft delete flag (0: not deleted, 1: deleted)',
    create_time     datetime   default CURRENT_TIMESTAMP not null comment 'Record creation time',
    update_time     datetime                             null on update CURRENT_TIMESTAMP comment 'Record update time'
)
    comment 'AI call history table - stores each AI request made through the streaming API';

create index idx_create_time
    on ai_call_history (create_time);

create index idx_timestamp
    on ai_call_history (timestamp);

create index idx_user_id
    on ai_call_history (user_id);

create index idx_user_prompt_history
    on ai_call_history (user_id asc, prompt_mode asc, prompt(100) asc, timestamp desc);

