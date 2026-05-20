package com.linxi.service;

import java.util.List;
import java.util.Map;

public interface ReportService {

    /**
     * 总部看板
     */
    Map<String, Object> dashboard(String date, String region, Long storeId, List<Long> storeIds);

    /**
     * 趋势同比对比
     */
    Map<String, Object> trendCompare(String date, String period, String metric, List<Long> storeIds);

    /**
     * 月报汇总
     */
    Map<String, Object> monthlyReport(Long storeId, String month);

    /**
     * 月报明细（按门店分组的月度汇总列表）
     */
    List<Map<String, Object>> monthlyDetail(String month, List<Long> storeIds);

    /**
     * 趋势数据
     */
    List<Map<String, Object>> trend(List<Long> storeIds, String startDate, String endDate, String metric, String period);

    /**
     * 渠道分析
     */
    List<Map<String, Object>> channelAnalysis(List<Long> storeIds, String startDate, String endDate);

    /**
     * 门店排名
     * @param date 基准日期（用于计算天/周/月范围）
     * @param period 时间维度：day=当天, week=当周, month=当月
     * @param metric 指标类型
     */
    List<Map<String, Object>> storeRanking(String date, String period, String metric);
}
