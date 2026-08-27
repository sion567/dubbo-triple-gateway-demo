package com.example.dubbo.user.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AccountMapper {

    @Update("UPDATE account SET money = money - #{money} WHERE user_id = #{userId} AND money >= #{money}")
    int debit(@Param("userId") Long userId, @Param("money") double money);

    @Select("SELECT money FROM account WHERE user_id = #{userId}")
    Double selectMoney(Long userId);
}
