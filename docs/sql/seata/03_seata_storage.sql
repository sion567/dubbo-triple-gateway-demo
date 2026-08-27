-- ================================================
-- Seata 示例：库存库
-- ================================================
CREATE DATABASE IF NOT EXISTS seata_storage;
USE seata_storage;

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

-- 业务表：库存
CREATE TABLE IF NOT EXISTS storage (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
    count        INT         NOT NULL DEFAULT 0 COMMENT '库存数量',
    UNIQUE KEY uk_product (product_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存表';

-- 初始数据：iPhone 库存 100，MacBook 库存 50
INSERT INTO storage(product_code, count) VALUES ('iPhone15', 100), ('MacBookPro', 50)
ON DUPLICATE KEY UPDATE count = count;
