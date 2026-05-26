package com.linxi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.annotation.OperationLog;
import com.linxi.dto.StoreQueryDTO;
import com.linxi.entity.Store;
import com.linxi.security.SecurityUtils;
import com.linxi.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<Store>> list(StoreQueryDTO query) {
        // 数据范围过滤：门店人员只能看自己绑定的门店
        List<Long> storeIds = SecurityUtils.getCurrentUserStoreIds();
        if (storeIds != null && !storeIds.isEmpty()) {
            // 非总部人员：限制查询范围
            query.setStoreIds(storeIds);
        } else if (storeIds != null && storeIds.isEmpty()) {
            // 有权限但无绑定门店（异常情况）
            return Result.success(PageResult.of(0L, 0, 1, query.getPageSize(), Collections.<Store>emptyList()));
        }
        Page<Store> page = storeService.page(query);
        PageResult<Store> pageResult = PageResult.of(
                page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords()
        );
        return Result.success(pageResult);
    }
    @GetMapping("/options")
    public Result<List<Map<String, Object>>> getOptions() {
        // 数据范围过滤
        List<Long> storeIds = SecurityUtils.getCurrentUserStoreIds();
        List<Map<String, Object>> options = storeService.getOptions();
        if (storeIds != null) {
            // 非总部人员：只返回绑定的门店
            options = options.stream()
                    .filter(o -> storeIds.contains(o.get("value")))
                    .collect(Collectors.toList());
        }
        return Result.success(options);
    }

    @GetMapping("/{id}")
    public Result<Store> getById(@PathVariable Long id) {
        return Result.success(storeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:user', 'ROLE_SUPER_ADMIN', 'ROLE_CEO')")
    @OperationLog(module = "门店管理", type = "CREATE", description = "新增门店")
    public Result<Void> save(@RequestBody Store store) {
        storeService.save(store);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:user', 'ROLE_SUPER_ADMIN', 'ROLE_CEO')")
    @OperationLog(module = "门店管理", type = "UPDATE", description = "编辑门店")
    public Result<Void> update(@PathVariable Long id, @RequestBody Store store) {
        store.setId(id);
        storeService.update(store);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:user', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "门店管理", type = "DELETE", description = "删除门店")
    public Result<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return Result.success();
    }
}
