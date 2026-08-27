-- ================================================
-- Seata 示例：账户库
-- ================================================
CREATE DATABASE IF NOT EXISTS seata_account;
USE seata_account;

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

-- 业务表：账户
CREATE TABLE IF NOT EXISTS account (
    id      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT      NOT NULL COMMENT '用户id',
    money   DOUBLE      NOT NULL DEFAULT 0 COMMENT '余额',
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账户表';

-- 初始数据：3 个用户，各 10000 元
INSERT INTO account(user_id, money) VALUES (1, 10000), (2, 10000), (3, 10000)
ON DUPLICATE KEY UPDATE money = money;
