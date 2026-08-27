package com.example.dubbo.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * 库存服务（RM）：扣减库存，参与 Seata AT 全局事务；同时暴露 tri rest 端点给前端。
 */
@RequestMapping("/storage")
public interface StorageService {

    /**
     * 扣减库存。库存不足时抛异常，触发全局回滚
     *
     * @param productCode 商品编码
     * @param count       扣减数量
     */
    void deduct(String productCode, int count);

    /** 全量库存列表 */
    @GetMapping("/list")
    List<Map<String, Object>> list();

    /** 新增/补货（存在则累加数量） */
    @PostMapping("/save")
    Map<String, Object> save(@RequestBody Map<String, Object> storage);

    /** 删除 */
    @DeleteMapping("/delete/{productCode}")
    Map<String, Object> delete(@PathVariable("productCode") String productCode);
}
