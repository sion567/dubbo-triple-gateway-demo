package com.example.dubbo.user.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface AccountMapper {

    @Select("SELECT user_id, username, password, roles, perms FROM t_account WHERE username = #{username}")
    Map<String, Object> findByUsername(@Param("username") String username);

    @Select("SELECT user_id, username, password, roles, perms FROM t_account WHERE user_id = #{userId}")
    Map<String, Object> findByUserId(@Param("userId") Long userId);

    @Select("SELECT user_id, username, roles, perms FROM t_account")
    List<Map<String, Object>> findAll();
}
