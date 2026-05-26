package com.linxi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.dto.DailyReportQueryDTO;
import com.linxi.dto.DailyReportSaveDTO;
import com.linxi.entity.DailyReport;
import com.linxi.entity.DailyReportField;
import com.linxi.entity.Store;
import com.linxi.mapper.StoreMapper;
import com.linxi.security.SecurityUtils;
import com.linxi.service.DailyReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/daily-reports")
public class DailyReportController {

    @Autowired
    private DailyReportService dailyReportService;

    @Autowired
    private StoreMapper storeMapper;

    @GetMapping("/today")
    @PreAuthorize("hasAnyAuthority('report:manage', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_STORE_MANAGER', 'ROLE_STORE_STAFF')")
    public Result<Map<String, Object>> today(@RequestParam Long storeId, @RequestParam(required = false) String reportDate) {
        // 数据范围过滤：门店人员只能查看自己门店的日报
        validateStoreAccess(storeId);
        Map<String, Object> result = dailyReportService.getTodayReport(storeId, reportDate);
        return Result.success(result);
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAnyAuthority('report:manage', 'report:audit', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_STORE_MANAGER', 'ROLE_STORE_STAFF')")
    public Result<Map<String, Object>> detail(@RequestParam(required = false) Long storeId,
                                               @RequestParam String reportDate) {
        // 数据范围过滤
        if (storeId != null) {
            validateStoreAccess(storeId);
        }
        Map<String, Object> result = dailyReportService.getDetail(storeId, reportDate);
        return Result.success(result);
    }

    @PostMapping("/draft")
    @PreAuthorize("hasAnyAuthority('report:manage', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_STORE_MANAGER', 'ROLE_STORE_STAFF')")
    public Result<Void> saveDraft(@RequestBody DailyReportSaveDTO dto) {
        // 数据范围过滤：门店人员只能为自己门店保存草稿
        if (dto.getStoreId() != null) {
            validateStoreAccess(dto.getStoreId());
            Store store = storeMapper.selectById(dto.getStoreId());
            if (store != null && Integer.valueOf(0).equals(store.getStatus())) {
                throw new BusinessException("该门店已禁用，不能新增日报");
            }
        }
        dailyReportService.saveDraft(dto);
        return Result.success();
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyAuthority('report:fill', 'ROLE_STORE_MANAGER', 'ROLE_STORE_STAFF')")
    public Result<Void> submit(@RequestBody DailyReportSaveDTO dto) {
        // 数据范围过滤：门店人员只能为自己门店提交日报
        if (dto.getStoreId() != null) {
            validateStoreAccess(dto.getStoreId());
            Store store = storeMapper.selectById(dto.getStoreId());
            if (store != null && Integer.valueOf(0).equals(store.getStatus())) {
                throw new BusinessException("该门店已禁用，不能新增日报");
            }
        }
        dailyReportService.submit(dto);
        return Result.success();
    }

    @GetMapping("/query")
    @PreAuthorize("hasAnyAuthority('report:manage', 'report:audit', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_STORE_MANAGER', 'ROLE_STORE_STAFF')")
    public Result<PageResult<DailyReport>> query(DailyReportQueryDTO query) {
        // 数据范围过滤：非总部人员只能查询自己门店的日报
        List<Long> storeIds = SecurityUtils.getCurrentUserStoreIds();
        if (storeIds != null) {
            if (query.getStoreId() != null && !storeIds.contains(query.getStoreId())) {
                return Result.success(PageResult.of(0L, 0, query.getPageNo(), query.getPageSize(), Collections.<DailyReport>emptyList()));
            }
            query.setStoreIds(storeIds);
        }
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
    @PreAuthorize("hasAnyAuthority('report:manage', 'system:template', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_STORE_MANAGER', 'ROLE_STORE_STAFF')")
    public Result<List<DailyReportField>> getFields(@PathVariable Long templateId) {
        return Result.success(dailyReportService.getTemplateFields(templateId));
    }

    @GetMapping("/unfilled")
    @PreAuthorize("hasAnyAuthority('report:unfilled', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_STORE_MANAGER', 'ROLE_STORE_STAFF')")
    public Result<Map<String, Object>> getUnfilledStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<Map<String, Object>> stores = dailyReportService.getUnfilledStats(startDate, endDate);
        // 数据范围过滤：非总部人员只看自己门店
        List<Long> storeIds = SecurityUtils.getCurrentUserStoreIds();
        if (storeIds != null) {
            stores = stores.stream()
                    .filter(s -> s.get("storeId") != null && storeIds.contains(Long.valueOf(s.get("storeId").toString())))
                    .collect(Collectors.toList());
        }
        Map<String, Object> result = new HashMap<>();
        int total = storeIds != null ? storeIds.size() : storeMapper.selectCount(
            new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1)
        ).intValue();
        result.put("total", total);
        result.put("filled", total - stores.size());
        result.put("unfilled", stores.size());
        result.put("stores", stores);
        return Result.success(result);
    }

    /**
     * 校验当前用户是否有权访问指定门店的数据
     */
    private void validateStoreAccess(Long storeId) {
        if (SecurityUtils.isHQUser()) {
            return; // 总部人员可访问所有门店
        }
        List<Long> storeIds = SecurityUtils.getCurrentUserStoreIds();
        if (storeIds == null || !storeIds.contains(storeId)) {
            throw new BusinessException("无权访问该门店数据");
        }
    }
}
