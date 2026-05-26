package com.linxi.service;

import com.linxi.entity.DailyChannel;

import java.util.List;
import java.util.Map;

public interface DailyChannelService {

    /**
     * 保存或更新渠道评价数据
     */
    void saveOrUpdate(Long reportId, Long storeId, String reportDate, Map<String, Object> values);

    /**
     * 获取日报对应的渠道评价数据
     */
    DailyChannel getByReportId(Long reportId);

}
