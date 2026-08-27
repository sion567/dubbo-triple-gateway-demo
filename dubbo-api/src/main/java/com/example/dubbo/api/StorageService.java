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
 * 库存 REST 端点（tri rest, JSON）：给网关/前端用。
 * 内部 RPC（扣库存扣减，参与 Seata 全局事务）已改为 IDL 模式：见 dubbo-proto-api 的 storage.proto。
 */
@RequestMapping("/storage")
public interface StorageService {

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
