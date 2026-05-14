package com.linxi.controller;

import com.linxi.common.Result;
import com.linxi.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
                                                   @RequestParam(required = false) Long storeId) {
        return Result.success(reportService.dashboard(date, region, storeId));
    }

    @GetMapping("/monthly")
    public Result<Map<String, Object>> monthly(@RequestParam(required = false) Long storeId,
                                                @RequestParam String month) {
        return Result.success(reportService.monthlyReport(storeId, month));
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(required = false) Long storeId,
                                                     @RequestParam String startDate,
                                                     @RequestParam String endDate,
                                                     @RequestParam(defaultValue = "revenue") String metric) {
        return Result.success(reportService.trend(storeId, startDate, endDate, metric));
    }

    @GetMapping("/channel-analysis")
    public Result<List<Map<String, Object>>> channelAnalysis(@RequestParam(required = false) Long storeId,
                                                               @RequestParam String startDate,
                                                               @RequestParam String endDate) {
        return Result.success(reportService.channelAnalysis(storeId, startDate, endDate));
    }

    @GetMapping("/store-ranking")
    public Result<List<Map<String, Object>>> storeRanking(@RequestParam String date,
                                                            @RequestParam(defaultValue = "revenue") String metric) {
        return Result.success(reportService.storeRanking(date, metric));
    }
}
