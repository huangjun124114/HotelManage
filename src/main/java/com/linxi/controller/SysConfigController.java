package com.linxi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.entity.SysConfig;
import com.linxi.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/configs")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<SysConfig>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String configKey) {
        PageResult<SysConfig> result = sysConfigService.pageResult(page, size, configKey);
        return Result.success(result);
    }
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Result<Void> save(@RequestBody SysConfig config) {
        sysConfigService.save(config);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysConfig config) {
        config.setId(id);
        sysConfigService.updateById(config);
        return Result.success();
    }}
