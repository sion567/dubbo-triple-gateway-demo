-- 用户库（H2 内存库，MySQL 兼容模式）
-- AT 模式回滚日志表
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

-- 登录账号表（用户名/密码/角色，用于认证，user_id 关联 t_user 表）
CREATE TABLE IF NOT EXISTS t_account (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    username      VARCHAR(64) NOT NULL,
    password      VARCHAR(64) NOT NULL,
    roles         VARCHAR(255) NOT NULL,
    perms         VARCHAR(512)
);

-- 用户信息表（id=1 为管理员，id=2~6 为普通用户）
CREATE TABLE IF NOT EXISTS t_user (
    id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(64)  NOT NULL,
    money DECIMAL(12,2) NOT NULL DEFAULT 0
);
