package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.dto.DailyReportQueryDTO;
import com.linxi.dto.DailyReportSaveDTO;
import com.linxi.entity.DailyReport;
import com.linxi.entity.DailyReportField;

import java.util.List;
import java.util.Map;

public interface DailyReportService {

    /**
     * 获取今日日报，如不存在则根据模板自动创建草稿
     */
    Map<String, Object> getTodayReport(Long storeId);

    /**
     * 获取日报详情（含字段值）
     */
    Map<String, Object> getDetail(Long storeId, String reportDate);

    /**
     * 保存草稿
     */
    boolean saveDraft(DailyReportSaveDTO dto);

    /**
     * 提交日报
     */
    boolean submit(DailyReportSaveDTO dto);

    /**
     * 锁定日报
     */
    boolean lock(Long reportId);

    /**
     * 解锁日报
     */
    boolean unlock(Long reportId);

    /**
     * 退回日报
     */
    boolean reject(Long reportId, String reason);

    /**
     * 分页查询日报
     */
    Page<DailyReport> query(DailyReportQueryDTO query);

    /**
     * 获取模板字段列表
     */
    List<DailyReportField> getTemplateFields(Long templateId);

    /**
     * 获取未填报统计
     */
    List<Map<String, Object>> getUnfilledStats(String startDate, String endDate);
}
