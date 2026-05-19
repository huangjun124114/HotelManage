package com.linxi.service;

import com.linxi.entity.DailyExtension;

import java.util.Map;

public interface DailyExtensionService {

    /**
     * 保存或更新弹性域数据
     */
    void saveOrUpdate(Long reportId, Long storeId, String reportDate, Map<String, Object> values);

    /**
     * 获取日报对应的弹性域数据
     */
    DailyExtension getByReportId(Long reportId);

    /**
     * 获取门店某日的弹性域
     */
    DailyExtension getByStoreAndDate(Long storeId, String reportDate);
}
