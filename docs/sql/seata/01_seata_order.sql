-- ================================================
-- Seata 示例：订单库
-- ================================================
CREATE DATABASE IF NOT EXISTS seata_order;
USE seata_order;

-- AT 模式回滚日志表（每个参与全局事务的业务库都必须有）
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT       NOT NULL COMMENT 'branch transaction id',
    xid           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context, such as serialization',
    rollback_info LONGBLOB     NOT NULL COMMENT 'rollback info',
    log_status    INT          NOT NULL COMMENT '0:normal status, 1:defense status',
    log_created   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    log_modified  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = 'AT transaction mode undo table';

-- 业务表：订单
CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL COMMENT '用户id',
    product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
    product      VARCHAR(64) COMMENT '商品名',
    count        INT         NOT NULL DEFAULT 1 COMMENT '数量',
    money        DOUBLE      NOT NULL COMMENT '金额',
    status       VARCHAR(16) NOT NULL DEFAULT 'INIT' COMMENT '订单状态',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';
