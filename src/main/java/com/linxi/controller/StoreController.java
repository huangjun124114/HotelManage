package com.linxi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.annotation.OperationLog;
import com.linxi.dto.StoreQueryDTO;
import com.linxi.entity.Store;
import com.linxi.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<Store>> list(StoreQueryDTO query) {
        Page<Store> page = storeService.page(query);
        PageResult<Store> pageResult = PageResult.of(
                page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords()
        );
        return Result.success(pageResult);
    }

    @GetMapping("/list")
    public Result<List<Store>> listAll() {
        return Result.success(storeService.listAll());
    }

    @GetMapping("/options")
    public Result<List<Map<String, Object>>> getOptions() {
        return Result.success(storeService.getOptions());
    }

    @GetMapping("/{id}")
    public Result<Store> getById(@PathVariable Long id) {
        return Result.success(storeService.getById(id));
    }

    @PostMapping
    @OperationLog(module = "门店管理", type = "CREATE", description = "新增门店")
    public Result<Void> save(@RequestBody Store store) {
        storeService.save(store);
        return Result.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "门店管理", type = "UPDATE", description = "编辑门店")
    public Result<Void> update(@PathVariable Long id, @RequestBody Store store) {
        store.setId(id);
        storeService.update(store);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "门店管理", type = "DELETE", description = "删除门店")
    public Result<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return Result.success();
    }
}
