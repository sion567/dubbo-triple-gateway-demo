package com.example.dubbo.order.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

import com.example.dubbo.order.api.vo.OrderDTO;

public interface OrderMapper {

    @Insert("INSERT INTO t_orders(user_id, product_code, product, count, money, status) " +
            "VALUES(#{userId}, #{productCode}, #{product}, #{count}, #{price}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId", keyColumn = "id")
    int insert(OrderDTO order);

    @Select("SELECT id AS orderId, user_id AS userId, product_code AS productCode, product, count, money, status " +
            "FROM t_orders WHERE user_id = #{userId}")
    List<Map<String, Object>> selectByUserId(Long userId);

    @Select("SELECT id AS orderId, user_id AS userId, product_code AS productCode, product, count, money, status " +
            "FROM t_orders ORDER BY id DESC")
    List<Map<String, Object>> selectAll();

    @Select("SELECT id AS orderId, user_id AS userId, product_code AS productCode, product, count, money, status " +
            "FROM t_orders WHERE id = #{orderId}")
    Map<String, Object> selectById(Long orderId);

    @Select("SELECT COUNT(*) FROM t_orders WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    @Update("UPDATE t_orders SET status = #{status} WHERE user_id = #{userId} AND status = 'INIT'")
    int updateStatus(@Param("userId") Long userId, @Param("status") String status);

    @Update("UPDATE t_orders SET status = #{status} WHERE id = #{id}")
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM t_orders WHERE id = #{id}")
    int deleteById(Long id);
}
