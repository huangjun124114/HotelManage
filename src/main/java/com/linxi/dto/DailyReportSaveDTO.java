package com.linxi.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DailyReportSaveDTO {

    private Long storeId;
    private String reportDate;
    private Long templateId;
    private Long reportId;
    /**
     * 字段值映射，key为fieldCode，value为字段值
     */
    private Map<String, Object> values;

    /**
     * 渠道间夜数据：key为channelCode，value为间夜数
     */
    private Map<String, Double> channelData;

    /**
     * 平台评价数据：key为platformCode，value为评分等
     */
    private Map<String, Object> platformData;
}
