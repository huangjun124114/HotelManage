package com.linxi.controller;

import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.entity.SysOperationLog;
import com.linxi.service.SysOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/operation-logs")
public class SysOperationLogController {

    @Autowired
    private SysOperationLogService sysOperationLogService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<SysOperationLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PageResult<SysOperationLog> result = sysOperationLogService.pageResult(page, size, operator, module, startDate, endDate);
        return Result.success(result);
    }
}
