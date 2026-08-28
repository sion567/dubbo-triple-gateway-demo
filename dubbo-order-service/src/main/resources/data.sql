-- 初始订单数据
INSERT INTO t_orders (user_id, product_code, product, count, money, status) VALUES
    (2, 'P001', 'iPhone 15', 1, 6999.0, '已发货'),
    (3, 'P002', 'AirPods Pro', 1, 1899.0, '已完成'),
    (4, 'P003', 'MacBook Pro', 1, 14999.0, '待付款'),
    (5, 'P004', 'iPad Air', 1, 4799.0, '已发货'),
    (5, 'P005', 'Apple Watch', 1, 2999.0, '已完成'),
    (6, 'P006', 'AirTag', 1, 249.0, '已取消');
