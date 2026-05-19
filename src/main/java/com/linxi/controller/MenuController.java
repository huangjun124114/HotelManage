package com.linxi.controller;

import com.linxi.common.Result;
import com.linxi.annotation.OperationLog;
import com.linxi.entity.SysMenu;
import com.linxi.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        // 管理页面显示所有菜单（包括停用的）
        return Result.success(menuService.getAllMenuTree());
    }

    @GetMapping("/user")
    public Result<List<SysMenu>> userMenus() {
        // 从SecurityContext获取当前用户ID
        Long userId = menuService.getCurrentUserId();
        return Result.success(menuService.getUserMenus(userId));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:menu', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "菜单管理", type = "CREATE", description = "新增菜单")
    public Result<Void> save(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:menu', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "菜单管理", type = "UPDATE", description = "编辑菜单")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.update(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:menu', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "菜单管理", type = "DELETE", description = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success();
    }
}
