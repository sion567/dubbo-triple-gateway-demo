-- 订单库（H2 内存库，MySQL 兼容模式）
-- AT 模式回滚日志表：每个参与全局事务的库都必须有
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

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    product_code VARCHAR(64)  NOT NULL,
    product      VARCHAR(64),
    count        INT          NOT NULL DEFAULT 1,
    money        DOUBLE       NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'INIT',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
