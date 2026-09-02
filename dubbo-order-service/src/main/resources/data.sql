-- 初始订单数据（product_code 需与 t_storage 库存表一致）
INSERT INTO t_orders (user_id, product_code, product, count, money, status) VALUES
    (2, 'iPhone15', 'iPhone 15', 1, 6999.0, 'SUCCESS'),
    (3, 'MacBookPro', 'MacBook Pro', 1, 14999.0, 'SUCCESS'),
    (4, 'iPhone15', 'iPhone 15', 1, 6999.0, 'INIT'),
    (5, 'MacBookPro', 'MacBook Pro', 1, 14999.0, 'SUCCESS'),
    (5, 'iPhone15', 'iPhone 15', 2, 13998.0, 'SUCCESS');
