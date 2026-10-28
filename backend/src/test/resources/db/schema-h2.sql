-- 测试用 H2（MODE=MySQL）结构，与 db/schema.sql 一一对应。
-- 差异只在 MySQL 专有语法：去掉 ENGINE / COMMENT / 内联 KEY，索引单独建。

CREATE TABLE IF NOT EXISTS app_user (
  id           CHAR(36)    NOT NULL PRIMARY KEY,
  handle       VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(64) NOT NULL,
  accent_color VARCHAR(16) NOT NULL,
  created_at   TIMESTAMP(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS app_session (
  token      CHAR(36)    NOT NULL PRIMARY KEY,
  user_id    CHAR(36)    NOT NULL,
  created_at TIMESTAMP(3) NOT NULL,
  expires_at TIMESTAMP(3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_session_user ON app_session (user_id);

CREATE TABLE IF NOT EXISTS template (
  id                   VARCHAR(64)  NOT NULL PRIMARY KEY,
  title                VARCHAR(128) NOT NULL,
  category             VARCHAR(64)  NOT NULL,
  cover_url            VARCHAR(255) NOT NULL,
  description          VARCHAR(500) NOT NULL,
  required_image_count INT          NOT NULL,
  accepted_mime_types  VARCHAR(255) NOT NULL,
  max_file_size_bytes  BIGINT       NOT NULL,
  sort_order           INT          NOT NULL DEFAULT 0,
  created_at           TIMESTAMP(3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_template_category ON template (category, sort_order);

CREATE TABLE IF NOT EXISTS asset (
  id            CHAR(36)     NOT NULL PRIMARY KEY,
  user_id       CHAR(36)     NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type  VARCHAR(64)  NOT NULL,
  size_bytes    BIGINT       NOT NULL,
  storage_path  VARCHAR(255) NOT NULL,
  created_at    TIMESTAMP(3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_asset_user ON asset (user_id, created_at);

CREATE TABLE IF NOT EXISTS creation_job (
  id             CHAR(36)     NOT NULL PRIMARY KEY,
  user_id        CHAR(36)     NOT NULL,
  template_id    VARCHAR(64)  NOT NULL,
  template_title VARCHAR(128) NOT NULL,
  status         VARCHAR(16)  NOT NULL,
  stage          VARCHAR(24)  NOT NULL,
  progress       INT          NOT NULL DEFAULT 0,
  error_stage    VARCHAR(24),
  error_message  VARCHAR(500),
  result_url     VARCHAR(255),
  created_at     TIMESTAMP(3) NOT NULL,
  started_at     TIMESTAMP(3),
  finished_at    TIMESTAMP(3),
  updated_at     TIMESTAMP(3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_job_user_created ON creation_job (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_job_status ON creation_job (status);

CREATE TABLE IF NOT EXISTS job_asset (
  job_id   CHAR(36) NOT NULL,
  asset_id CHAR(36) NOT NULL,
  position INT      NOT NULL,
  PRIMARY KEY (job_id, position)
);
CREATE INDEX IF NOT EXISTS idx_job_asset_asset ON job_asset (asset_id);

CREATE TABLE IF NOT EXISTS job_event (
  id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  job_id     CHAR(36)     NOT NULL,
  stage      VARCHAR(24)  NOT NULL,
  event_type VARCHAR(24)  NOT NULL,
  message    VARCHAR(500),
  created_at TIMESTAMP(3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_event_job ON job_event (job_id, id);
