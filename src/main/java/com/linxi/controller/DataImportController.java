package com.linxi.controller;

import com.linxi.common.Result;
import com.linxi.service.DataImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/data")
public class DataImportController {

    @Autowired
    private DataImportService dataImportService;

    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('system:user')")
    public Result<Integer> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            int count = dataImportService.importExcel(file);
            return Result.success("导入成功，共导入" + count + "条数据", count);
        } catch (Exception e) {
            log.error("导入失败", e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }
}
