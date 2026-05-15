package com.linxi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.entity.SysMenu;
import com.linxi.entity.SysRole;
import com.linxi.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<SysRole>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<SysRole> page = roleService.page(pageNo, pageSize);
        PageResult<SysRole> pageResult = PageResult.of(
                page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords()
        );
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.update(role);
        return Result.success();
    }

    @PutMapping("/{id}/menus")
    public Result<Void> saveMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.saveRoleMenus(id, menuIds);
        return Result.success();
    }
}
