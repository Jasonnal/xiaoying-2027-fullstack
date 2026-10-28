-- 素材制作网站 · MySQL 8.0 结构定义
-- 幂等：应用每次启动都会执行（spring.sql.init.mode=always），因此全部使用 IF NOT EXISTS。

CREATE TABLE IF NOT EXISTS app_user (
  id            CHAR(36)     NOT NULL,
  handle        VARCHAR(64)  NOT NULL COMMENT '登录标识，演示账号用固定 handle',
  display_name  VARCHAR(64)  NOT NULL,
  accent_color  VARCHAR(16)  NOT NULL COMMENT '头像底色，前端展示用',
  created_at    DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_handle (handle)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '用户';

CREATE TABLE IF NOT EXISTS app_session (
  token       CHAR(36)     NOT NULL COMMENT '不透明会话令牌，HttpOnly Cookie 承载',
  user_id     CHAR(36)     NOT NULL,
  created_at  DATETIME(3)  NOT NULL,
  expires_at  DATETIME(3)  NOT NULL,
  PRIMARY KEY (token),
  KEY idx_session_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '登录会话';

CREATE TABLE IF NOT EXISTS template (
  id                    VARCHAR(64)  NOT NULL,
  title                 VARCHAR(128) NOT NULL,
  category              VARCHAR(64)  NOT NULL,
  cover_url             VARCHAR(255) NOT NULL,
  description           VARCHAR(500) NOT NULL,
  required_image_count  INT          NOT NULL COMMENT '需要的素材数量',
  accepted_mime_types   VARCHAR(255) NOT NULL COMMENT '逗号分隔，如 image/jpeg,image/png',
  max_file_size_bytes   BIGINT       NOT NULL,
  sort_order            INT          NOT NULL DEFAULT 0,
  created_at            DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_template_category (category, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '模板';

CREATE TABLE IF NOT EXISTS asset (
  id             CHAR(36)     NOT NULL,
  user_id        CHAR(36)     NOT NULL,
  original_name  VARCHAR(255) NOT NULL,
  content_type   VARCHAR(64)  NOT NULL,
  size_bytes     BIGINT       NOT NULL,
  storage_path   VARCHAR(255) NOT NULL COMMENT '相对存储根目录的路径',
  created_at     DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_asset_user (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '用户上传的素材';

CREATE TABLE IF NOT EXISTS creation_job (
  id             CHAR(36)     NOT NULL,
  user_id        CHAR(36)     NOT NULL,
  template_id    VARCHAR(64)  NOT NULL,
  template_title VARCHAR(128) NOT NULL COMMENT '冗余快照，避免模板变更影响历史展示',
  status         VARCHAR(16)  NOT NULL COMMENT 'PENDING / PROCESSING / SUCCEEDED / FAILED',
  stage          VARCHAR(24)  NOT NULL COMMENT 'QUEUED / MATERIAL_CHECK / GENERATE / FINALIZE / DONE',
  progress       INT          NOT NULL DEFAULT 0,
  error_stage    VARCHAR(24)  NULL,
  error_message  VARCHAR(500) NULL,
  result_url     VARCHAR(255) NULL,
  created_at     DATETIME(3)  NOT NULL,
  started_at     DATETIME(3)  NULL,
  finished_at    DATETIME(3)  NULL,
  updated_at     DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_job_user_created (user_id, created_at),
  KEY idx_job_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '制作任务';

CREATE TABLE IF NOT EXISTS job_asset (
  job_id    CHAR(36) NOT NULL,
  asset_id  CHAR(36) NOT NULL,
  position  INT      NOT NULL COMMENT '素材在模板中的槽位序号，从 0 开始',
  PRIMARY KEY (job_id, position),
  KEY idx_job_asset_asset (asset_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '任务与素材的槽位关系';

CREATE TABLE IF NOT EXISTS job_event (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  job_id      CHAR(36)     NOT NULL,
  stage       VARCHAR(24)  NOT NULL,
  event_type  VARCHAR(24)  NOT NULL COMMENT 'STAGE_STARTED / STAGE_COMPLETED / STAGE_FAILED',
  message     VARCHAR(500) NULL,
  created_at  DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_event_job (job_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '任务阶段事件（执行边界的可观察记录）';
