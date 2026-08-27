package com.example.dubbo.storage.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface StorageMapper {

    @Update("UPDATE storage SET count = count - #{count} " +
            "WHERE product_code = #{productCode} AND count >= #{count}")
    int deduct(@Param("productCode") String productCode, @Param("count") int count);

    @Select("SELECT count FROM storage WHERE product_code = #{productCode}")
    Integer selectCount(String productCode);

    @Select("SELECT product_code AS productCode, count FROM storage ORDER BY product_code")
    List<Map<String, Object>> selectAll();

    /** 新增或补货：存在则累加（H2 MySQL 模式支持该语法） */
    @Insert("INSERT INTO storage(product_code, count) VALUES(#{productCode}, #{count}) " +
            "ON DUPLICATE KEY UPDATE count = count + #{count}")
    int upsert(@Param("productCode") String productCode, @Param("count") int count);

    @Delete("DELETE FROM storage WHERE product_code = #{productCode}")
    int delete(String productCode);
}
