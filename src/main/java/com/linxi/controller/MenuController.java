package com.linxi.controller;

import com.linxi.common.Result;
import com.linxi.entity.SysMenu;
import com.linxi.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.success(menuService.getMenuTree());
    }

    @GetMapping("/user")
    public Result<List<SysMenu>> userMenus() {
        // 从SecurityContext获取当前用户ID
        return Result.success(menuService.getMenuTree());
    }
}
