package com.example.dubbo.user.mapper;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper {

    @Select("SELECT id as userId, name, money FROM t_user WHERE id = #{userId}")
    java.util.Map<String, Object> selectById(@Param("userId") Long userId);

    @Select("<script>SELECT id, name FROM t_user WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    java.util.List<java.util.Map<String, Object>> selectNamesByIds(@Param("ids") java.util.List<Long> ids);

    @Select("SELECT id as userId, name, money FROM t_user ORDER BY id")
    java.util.List<java.util.Map<String, Object>> selectAll();

    @Update("UPDATE t_user SET money = money - #{money} WHERE id = #{userId} AND money >= #{money}")
    int debit(@Param("userId") Long userId, @Param("money") BigDecimal money);

    @Select("SELECT money FROM t_user WHERE id = #{userId}")
    BigDecimal selectMoney(Long userId);
}
