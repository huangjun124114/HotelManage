package com.linxi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.dto.UserQueryDTO;
import com.linxi.entity.SysUser;
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
    public Result<Void> save(@RequestBody SysUser user) {
        userService.save(user);
        return Result.success();
    }

    @PutMapping("/{id}")
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
