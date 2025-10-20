CREATE TABLE IF NOT EXISTS project (
  id            VARCHAR(64) PRIMARY KEY,
  project_code  VARCHAR(64) UNIQUE,
  name          VARCHAR(100) NOT NULL,
  client_name   VARCHAR(100),
  client_phone  VARCHAR(30),
  address       VARCHAR(200),
  sales_person  VARCHAR(100),
  installer     VARCHAR(100),
  team_members  VARCHAR(255),
  start_date    VARCHAR(10),
  end_date      VARCHAR(10),
  status        VARCHAR(32),
  today_task    VARCHAR(1000),
  progress_note VARCHAR(2000),
  photo_url     VARCHAR(512),
  created_at    TIMESTAMP,
  archived      BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_project_created_at ON project(created_at);
CREATE INDEX IF NOT EXISTS idx_project_code ON project(project_code);
CREATE INDEX IF NOT EXISTS idx_project_status ON project(status);
CREATE INDEX IF NOT EXISTS idx_project_archived ON project(archived);

-- Todo tables for tests
CREATE TABLE IF NOT EXISTS todo_task (
  id               VARCHAR(64) PRIMARY KEY,
  user_id          INT,
  title            VARCHAR(200),
  description      VARCHAR(2000),
  success_points   INT,
  fail_points      INT,
  frequency        VARCHAR(32),
  custom_days      INT,
  status           VARCHAR(32),
  metadata         CLOB,
  idempotency_key  VARCHAR(128),
  created_at       TIMESTAMP,
  updated_at       TIMESTAMP,
  deleted_at       TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_todo_task_user ON todo_task(user_id);
CREATE INDEX IF NOT EXISTS idx_todo_task_status ON todo_task(status);
CREATE INDEX IF NOT EXISTS idx_todo_task_created ON todo_task(created_at);
CREATE INDEX IF NOT EXISTS idx_todo_task_updated ON todo_task(updated_at);
CREATE INDEX IF NOT EXISTS idx_todo_task_deleted ON todo_task(deleted_at);
CREATE INDEX IF NOT EXISTS idx_todo_task_idemp ON todo_task(idempotency_key);

CREATE TABLE IF NOT EXISTS todo_history (
  id               VARCHAR(64) PRIMARY KEY,
  user_id          INT,
  task_id          VARCHAR(64),
  title            VARCHAR(200),
  description      VARCHAR(2000),
  success_points   INT,
  fail_points      INT,
  status           VARCHAR(32),
  points_applied   INT,
  idempotency_key  VARCHAR(128),
  completed_at     TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_todo_history_user ON todo_history(user_id);
CREATE INDEX IF NOT EXISTS idx_todo_history_task ON todo_history(task_id);
CREATE INDEX IF NOT EXISTS idx_todo_history_completed ON todo_history(completed_at);
CREATE INDEX IF NOT EXISTS idx_todo_history_idemp ON todo_history(idempotency_key);

CREATE TABLE IF NOT EXISTS todo_trash (
  id               VARCHAR(64) PRIMARY KEY,
  user_id          INT,
  title            VARCHAR(200),
  description      VARCHAR(2000),
  success_points   INT,
  fail_points      INT,
  frequency        VARCHAR(32),
  custom_days      INT,
  status           VARCHAR(32),
  original_date    TIMESTAMP,
  deleted_date     TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_todo_trash_user ON todo_trash(user_id);
CREATE INDEX IF NOT EXISTS idx_todo_trash_deleted ON todo_trash(deleted_date);
