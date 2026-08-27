package com.example.dubbo.storage.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface StorageMapper {

    @Update("UPDATE storage SET count = count - #{count} " +
            "WHERE product_code = #{productCode} AND count >= #{count}")
    int deduct(@Param("productCode") String productCode, @Param("count") int count);

    @Select("SELECT count FROM storage WHERE product_code = #{productCode}")
    Integer selectCount(String productCode);
}
