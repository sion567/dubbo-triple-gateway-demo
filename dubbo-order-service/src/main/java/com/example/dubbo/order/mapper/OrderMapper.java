package com.example.dubbo.order.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface OrderMapper {

    @Insert("INSERT INTO orders(user_id, product_code, product, count, money, status) " +
            "VALUES(#{userId}, #{productCode}, #{product}, #{count}, #{money}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId", keyColumn = "id")
    int insert(@Param("userId") Long userId, @Param("productCode") String productCode,
               @Param("product") String product, @Param("count") int count,
               @Param("money") double money, @Param("status") String status);

    @Select("SELECT id AS orderId, user_id AS userId, product_code AS productCode, product, count, money, status " +
            "FROM orders WHERE user_id = #{userId}")
    List<Map<String, Object>> selectByUserId(Long userId);

    @Select("SELECT id AS orderId, user_id AS userId, product_code AS productCode, product, count, money, status " +
            "FROM orders ORDER BY id DESC")
    List<Map<String, Object>> selectAll();

    @Select("SELECT COUNT(*) FROM orders WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    @Update("UPDATE orders SET status = #{status} WHERE user_id = #{userId} AND status = 'INIT'")
    int updateStatus(@Param("userId") Long userId, @Param("status") String status);
}
