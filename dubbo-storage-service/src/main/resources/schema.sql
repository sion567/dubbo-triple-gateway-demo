-- 库存库（H2 内存库，MySQL 兼容模式）
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT        NOT NULL,
    xid           VARCHAR(128)  NOT NULL,
    context       VARCHAR(128)  NOT NULL,
    rollback_info VARBINARY(1048576) NOT NULL,
    log_status    INT           NOT NULL,
    log_created   TIMESTAMP(6)  NOT NULL,
    log_modified  TIMESTAMP(6)  NOT NULL,
    UNIQUE (xid, branch_id)
);

CREATE TABLE IF NOT EXISTS t_storage (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(64) NOT NULL,
    count        INT         NOT NULL DEFAULT 0,
    UNIQUE (product_code)
);
