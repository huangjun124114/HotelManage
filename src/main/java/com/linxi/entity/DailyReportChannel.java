package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("daily_report_channel")
public class DailyReportChannel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private Long storeId;
    private String reportDate;
    private String channelCode;
    private String channelName;
    private BigDecimal roomNights;
    private String createTime;
    private String updateTime;
}
