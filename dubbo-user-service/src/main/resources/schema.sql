-- 账户库（H2 内存库，MySQL 兼容模式）
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT        NOT NULL,
    xid           VARCHAR(128)  NOT NULL,
    context       VARCHAR(128)  NOT NULL,
    rollback_info VARBINARY(1024 * 1024) NOT NULL,
    log_status    INT           NOT NULL,
    log_created   TIMESTAMP(6)  NOT NULL,
    log_modified  TIMESTAMP(6)  NOT NULL,
    UNIQUE (xid, branch_id)
);

CREATE TABLE IF NOT EXISTS account (
    id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    money   DOUBLE NOT NULL DEFAULT 0,
    UNIQUE (user_id)
);
