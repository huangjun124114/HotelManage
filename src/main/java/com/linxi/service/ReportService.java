package com.linxi.service;

import java.util.List;
import java.util.Map;

public interface ReportService {

    /**
     * 总部看板
     */
    Map<String, Object> dashboard(String date, String region, Long storeId);

    /**
     * 月报汇总
     */
    Map<String, Object> monthlyReport(Long storeId, String month);

    /**
     * 趋势数据
     */
    List<Map<String, Object>> trend(Long storeId, String startDate, String endDate, String metric);

    /**
     * 渠道分析
     */
    List<Map<String, Object>> channelAnalysis(Long storeId, String startDate, String endDate);

    /**
     * 门店排名
     */
    List<Map<String, Object>> storeRanking(String date, String metric);
}
