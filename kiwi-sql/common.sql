# 查询正在使用的表
show open tables where in_use > 0;

# 查询是否开启自动提交事务
SHOW VARIABLES LIKE 'AUTOCOMMIT';
select @@AUTOCOMMIT;

# mysql会把当前正在运行的mysql线程信息实时更新到processlist表
show full processlist;
select p.*
from information_schema.processlist p
where p.COMMAND != 'Sleep';
# 查找当前正在执行超过1分钟的线程信息
select *
from information_schema.processlist
where command != 'Sleep'
  and time > 20
order by time desc;

SHOW GLOBAL VARIABLES LIKE 'innodb_lock_wait_timeout';
# 设置事务超时时间
SET GLOBAL innodb_lock_wait_timeout = 120;

select it.trx_mysql_thread_id
from information_schema.innodb_trx it;
# 75940:143:4:2
select *
from information_schema.innodb_trx;

SELECT CONCAT('KILL ', ID, ';')
FROM INFORMATION_SCHEMA.PROCESSLIST;
select *
from INFORMATION_SCHEMA.PROCESSLIST;


KILL 891307;

select trx_mysql_thread_id
from information_schema.innodb_trx it
         JOIN information_schema.INNODB_LOCK_WAITS ilw
              on ilw.blocking_trx_id = it.trx_id;

select *
from information_schema.innodb_locks;

select *
from information_schema.innodb_lock_waits;

show tables;

SET AUTOCOMMIT = 0;

set global max_connections = 500;
set global time_zone = ' + 8 : 00 ';
set global system_time_zone = 'Asia/Shanghai';
flush privileges;

show variables like 'general_log';
show variables like '%max_connections%';
show global status like 'Max_used_connections';
SHOW STATUS LIKE '%Connection%';
show variables like '%time_zone%';
SHOW STATUS LIKE '%Threads_connected%';
SHOW STATUS LIKE '%Threads_running%';
show processlist;
show status like 'thread_pool_size';
show global status like 'thread_pool_size';
show variables like 'thread_cache_size';
# 该参数是配置线程模型，默认情况是one-thread-per-connection，即不启用线程池；将该参数设置为pool-of-threads即启用了线程池。
show variables like 'thread_handling';
SET GLOBAL thread_pool_size = '';
SET wait_timeout = 10;
SET global thread_pool_size = 24;
show variables like 'thread_pool_size';
show global variables like 'thread_pool_size';
show variables like '%timeout%';
show variables like 'interactive_timeout';
show variables like 'connect_timeout';
show status like 'uptime';
show status like 'threads_cached';
show variables like '%wait_timeout%';
show variables like 'slow_query_log%';
show variables like 'long_query_time%';
show variables like 'log_queries_not_using_indexes%';
show variables like 'log_output%';
SELECT VERSION(); #查询版本号

SELECT UUID();
select UUID_TO_BIN();
select current_timestamp;
select now();
select curdate();
select DATE_SUB(curdate(), INTERVAL 1 DAY);

# 列出非内置存储过程
SELECT *
FROM information_schema.Routines r
where r.ROUTINE_SCHEMA = 'sys';

# 查询当前时间
select now();
select current_timestamp();
select sysdate();

# 查看DDL
SHOW CREATE TABLE snippet_upload_task;
show variables like 'wait_timeout';

alter database kiwi_db character set utf8mb4;

ALTER TABLE word_review_audio
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

SHOW CHARACTER SET;

