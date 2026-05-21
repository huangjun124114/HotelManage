package com.linxi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.annotation.OperationLog;
import com.linxi.dto.UserQueryDTO;
import com.linxi.entity.Store;
import com.linxi.entity.SysUser;
import com.linxi.service.StoreService;
import com.linxi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private StoreService storeService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<SysUser>> list(UserQueryDTO query) {
        Page<SysUser> page = userService.page(query);
        PageResult<SysUser> pageResult = PageResult.of(
                page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords()
        );
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:user', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "用户管理", type = "CREATE", description = "新增用户")
    public Result<Void> save(@RequestBody SysUser user) {
        // 如果请求中包含 storeId，校验该门店是否可用
        if (user.getStoreId() != null) {
            Store store = storeService.getById(user.getStoreId());
            if (store != null && Integer.valueOf(0).equals(store.getStatus())) {
                throw new BusinessException("该门店已禁用，不能新增人员");
            }
        }
        userService.save(user);
        return Result.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "用户管理", type = "UPDATE", description = "编辑用户")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.update(user);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:user', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "用户管理", type = "DELETE", description = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return Result.success();
    }

    @PutMapping("/{id}/stores")
    public Result<Void> assignStores(@PathVariable Long id, @RequestBody List<Long> storeIds) {
        userService.assignStores(id, storeIds);
        return Result.success();
    }
}
