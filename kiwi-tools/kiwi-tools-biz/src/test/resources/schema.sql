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

