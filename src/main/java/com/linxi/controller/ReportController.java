package com.linxi.controller;

import com.linxi.common.Result;
import com.linxi.security.SecurityUtils;
import com.linxi.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('analysis:dashboard', 'ROLE_SUPER_ADMIN', 'ROLE_CEO')")
    public Result<Map<String, Object>> dashboard(@RequestParam(required = false) String date,
                                                   @RequestParam(required = false) String region,
                                                   @RequestParam(required = false) Long storeId,
                                                   @RequestParam(required = false) String storeIds) {
        List<Long> storeIdList = parseStoreIds(storeIds);
        // 数据范围过滤：非总部人员只能看自己权限范围内的门店
        storeIdList = applyDataScope(storeIdList, storeId);
        return Result.success(reportService.dashboard(date, region, storeId, storeIdList));
    }

    @GetMapping("/trend-compare")
    @PreAuthorize("hasAnyAuthority('analysis:trend', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_INVESTOR')")
    public Result<Map<String, Object>> trendCompare(@RequestParam(required = false) String date,
                                                      @RequestParam(defaultValue = "day") String period,
                                                      @RequestParam(defaultValue = "revenue") String metric,
                                                      @RequestParam(required = false) String storeIds) {
        List<Long> storeIdList = parseStoreIds(storeIds);
        storeIdList = applyDataScope(storeIdList, null);
        return Result.success(reportService.trendCompare(date, period, metric, storeIdList));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyAuthority('analysis:monthly', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_INVESTOR')")
    public Result<Map<String, Object>> monthly(@RequestParam(required = false) Long storeId,
                                                @RequestParam String month) {
        // 数据范围过滤
        List<Long> accessibleIds = SecurityUtils.getCurrentUserStoreIds();
        if (accessibleIds != null && storeId != null && !accessibleIds.contains(storeId)) {
            return Result.success(Collections.<String, Object>emptyMap());
        }
        return Result.success(reportService.monthlyReport(storeId, month));
    }

    @GetMapping("/monthly-detail")
    @PreAuthorize("hasAnyAuthority('analysis:monthly', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_INVESTOR')")
    public Result<List<Map<String, Object>>> monthlyDetail(@RequestParam String month,
                                                             @RequestParam(required = false) String storeIds) {
        List<Long> storeIdList = parseStoreIds(storeIds);
        storeIdList = applyDataScope(storeIdList, null);
        return Result.success(reportService.monthlyDetail(month, storeIdList));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyAuthority('analysis:trend', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_INVESTOR')")
    public Result<List<Map<String, Object>>> trend(@RequestParam(required = false) String storeIds,
                                                     @RequestParam String startDate,
                                                     @RequestParam String endDate,
                                                     @RequestParam(defaultValue = "revenue") String metric,
                                                     @RequestParam(defaultValue = "day") String period) {
        List<Long> storeIdList = parseStoreIds(storeIds);
        storeIdList = applyDataScope(storeIdList, null);
        return Result.success(reportService.trend(storeIdList, startDate, endDate, metric, period));
    }

    @GetMapping("/channel-analysis")
    @PreAuthorize("hasAnyAuthority('analysis:channel', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_INVESTOR')")
    public Result<List<Map<String, Object>>> channelAnalysis(@RequestParam(required = false) String storeId,
                                                               @RequestParam String startDate,
                                                               @RequestParam String endDate) {
        List<Long> storeIdList = parseStoreIds(storeId);
        storeIdList = applyDataScope(storeIdList, null);
        return Result.success(reportService.channelAnalysis(storeIdList, startDate, endDate));
    }

    @GetMapping("/store-ranking")
    @PreAuthorize("hasAnyAuthority('analysis:ranking', 'ROLE_SUPER_ADMIN', 'ROLE_CEO', 'ROLE_INVESTOR')")
    public Result<List<Map<String, Object>>> storeRanking(@RequestParam String date,
                                                            @RequestParam(defaultValue = "day") String period,
                                                            @RequestParam(defaultValue = "revenue") String metric) {
        // 排名数据：非总部人员只能看到自己权限范围内的门店排名
        List<Long> accessibleIds = SecurityUtils.getCurrentUserStoreIds();
        List<Map<String, Object>> ranking = reportService.storeRanking(date, period, metric);
        if (accessibleIds != null) {
            ranking = ranking.stream()
                    .filter(r -> r.get("storeId") != null && accessibleIds.contains(Long.valueOf(r.get("storeId").toString())))
                    .collect(java.util.stream.Collectors.toList());
        }
        return Result.success(ranking);
    }

    /**
     * 解析逗号分隔的门店ID字符串为List
     */
    private List<Long> parseStoreIds(String storeIds) {
        if (storeIds == null || storeIds.trim().isEmpty()) {
            return null;
        }
        List<Long> result = new ArrayList<>();
        for (String s : storeIds.split(",")) {
            try {
                result.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid storeId: {}", s);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 应用数据范围过滤
     * 非总部人员（门店人员/投资人）只能查看自己权限范围内的门店数据
     * @param requestedIds 请求中指定的门店ID列表
     * @param singleStoreId 请求中的单个门店ID
     * @return 过滤后的门店ID列表
     */
    private List<Long> applyDataScope(List<Long> requestedIds, Long singleStoreId) {
        List<Long> accessibleIds = SecurityUtils.getCurrentUserStoreIds();
        if (accessibleIds == null) {
            // 总部人员：不限制
            return requestedIds;
        }
        // 非总部人员：取交集
        if (requestedIds != null) {
            List<Long> filtered = new ArrayList<>(requestedIds);
            filtered.retainAll(accessibleIds);
            return filtered;
        }
        // 没有指定门店，但有权限范围：返回全部可访问的门店
        return accessibleIds;
    }
}
