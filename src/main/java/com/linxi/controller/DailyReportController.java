package com.linxi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.dto.DailyReportQueryDTO;
import com.linxi.dto.DailyReportSaveDTO;
import com.linxi.entity.DailyReport;
import com.linxi.entity.DailyReportField;
import com.linxi.entity.Store;
import com.linxi.mapper.StoreMapper;
import com.linxi.service.DailyReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/daily-reports")
public class DailyReportController {

    @Autowired
    private DailyReportService dailyReportService;

    @Autowired
    private StoreMapper storeMapper;

    @GetMapping("/today")
    public Result<Map<String, Object>> today(@RequestParam Long storeId, @RequestParam(required = false) String reportDate) {
        Map<String, Object> result = dailyReportService.getTodayReport(storeId, reportDate);
        return Result.success(result);
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam(required = false) Long storeId,
                                               @RequestParam String reportDate) {
        Map<String, Object> result = dailyReportService.getDetail(storeId, reportDate);
        return Result.success(result);
    }

    @GetMapping("/draft")
    public Result<Map<String, Object>> draft(@RequestParam Long storeId,
                                              @RequestParam String reportDate) {
        Map<String, Object> result = dailyReportService.getDetail(storeId, reportDate);
        return Result.success(result);
    }

    @PostMapping("/draft")
    public Result<Void> saveDraft(@RequestBody DailyReportSaveDTO dto) {
        dailyReportService.saveDraft(dto);
        return Result.success();
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyAuthority('report:fill', 'ROLE_STORE_MANAGER', 'ROLE_STORE_STAFF')")
    public Result<Void> submit(@RequestBody DailyReportSaveDTO dto) {
        dailyReportService.submit(dto);
        return Result.success();
    }

    @GetMapping("/query")
    public Result<PageResult<DailyReport>> query(DailyReportQueryDTO query) {
        Page<DailyReport> page = dailyReportService.query(query);
        PageResult<DailyReport> pageResult = PageResult.of(
                page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords()
        );
        return Result.success(pageResult);
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAnyAuthority('report:audit')")
    public Result<Void> lock(@PathVariable Long id) {
        dailyReportService.lock(id);
        return Result.success();
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAnyAuthority('report:audit')")
    public Result<Void> unlock(@PathVariable Long id) {
        dailyReportService.unlock(id);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('report:audit')")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        dailyReportService.reject(id, body.get("reason"));
        return Result.success();
    }

    @GetMapping("/template/{templateId}/fields")
    public Result<List<DailyReportField>> getFields(@PathVariable Long templateId) {
        return Result.success(dailyReportService.getTemplateFields(templateId));
    }

    @GetMapping("/unfilled")
    public Result<Map<String, Object>> getUnfilledStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<Map<String, Object>> stores = dailyReportService.getUnfilledStats(startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        int total = storeMapper.selectCount(
            new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1)
        ).intValue();
        result.put("total", total);
        result.put("filled", total - stores.size());
        result.put("unfilled", stores.size());
        result.put("stores", stores);
        return Result.success(result);
    }
}
