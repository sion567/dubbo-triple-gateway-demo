-- 用户基础信息（id=1 为管理员，无余额）
INSERT INTO t_user (id, name, money) VALUES
   (1,'管理员', 0),
   (2,'李四', 10000.00),
   (3,'王五', 10000.00),
   (4,'赵六', 10000.00),
   (5,'孙七', 10000.00),
   (6,'周八', 10000.00);


-- 登录账号（明文密码）
-- admin: ROLE_ADMIN,ROLE_USER (只能看，不能下单，t_user表里id=1余额=0)
-- user2~user6: ROLE_USER (正常用户，可下单)
INSERT INTO t_account (user_id, username, password, roles, perms) VALUES
    (1, 'admin', 'admin123', 'ROLE_ADMIN,ROLE_USER', 'order:query,order:manage:list'),
    (2, 'user2', '123456', 'ROLE_USER', 'order:query,order:create'),
    (3, 'user3', '123456', 'ROLE_USER', 'order:query,order:create'),
    (4, 'user4', '123456', 'ROLE_USER', 'order:query,order:create'),
    (5, 'user5', '123456', 'ROLE_USER', 'order:query,order:create'),
    (6, 'user6', '123456', 'ROLE_USER', 'order:query,order:create');

