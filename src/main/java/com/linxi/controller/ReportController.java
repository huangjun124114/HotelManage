package com.linxi.controller;

import com.linxi.common.Result;
import com.linxi.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(@RequestParam(required = false) String date,
                                                   @RequestParam(required = false) String region,
                                                   @RequestParam(required = false) Long storeId,
                                                   @RequestParam(required = false) String storeIds) {
        List<Long> storeIdList = parseStoreIds(storeIds);
        return Result.success(reportService.dashboard(date, region, storeId, storeIdList));
    }

    @GetMapping("/trend-compare")
    public Result<Map<String, Object>> trendCompare(@RequestParam(required = false) String date,
                                                      @RequestParam(defaultValue = "day") String period,
                                                      @RequestParam(defaultValue = "revenue") String metric,
                                                      @RequestParam(required = false) String storeIds) {
        List<Long> storeIdList = parseStoreIds(storeIds);
        return Result.success(reportService.trendCompare(date, period, metric, storeIdList));
    }

    @GetMapping("/monthly")
    public Result<Map<String, Object>> monthly(@RequestParam(required = false) Long storeId,
                                                @RequestParam String month) {
        return Result.success(reportService.monthlyReport(storeId, month));
    }

    @GetMapping("/monthly-detail")
    public Result<List<Map<String, Object>>> monthlyDetail(@RequestParam String month,
                                                             @RequestParam(required = false) String storeIds) {
        List<Long> storeIdList = parseStoreIds(storeIds);
        return Result.success(reportService.monthlyDetail(month, storeIdList));
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(required = false) String storeIds,
                                                     @RequestParam String startDate,
                                                     @RequestParam String endDate,
                                                     @RequestParam(defaultValue = "revenue") String metric,
                                                     @RequestParam(defaultValue = "day") String period) {
        List<Long> storeIdList = parseStoreIds(storeIds);
        return Result.success(reportService.trend(storeIdList, startDate, endDate, metric, period));
    }

    @GetMapping("/channel-analysis")
    public Result<List<Map<String, Object>>> channelAnalysis(@RequestParam(required = false) String storeId,
                                                               @RequestParam String startDate,
                                                               @RequestParam String endDate) {
        List<Long> storeIdList = parseStoreIds(storeId);
        return Result.success(reportService.channelAnalysis(storeIdList, startDate, endDate));
    }

    @GetMapping("/store-ranking")
    public Result<List<Map<String, Object>>> storeRanking(@RequestParam String startDate,
                                                            @RequestParam String endDate,
                                                            @RequestParam(defaultValue = "revenue") String metric) {
        return Result.success(reportService.storeRanking(startDate, endDate, metric));
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
}
