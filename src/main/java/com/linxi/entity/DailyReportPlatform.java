package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("daily_report_platform")
public class DailyReportPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private Long storeId;
    private String reportDate;
    private String platformCode;
    private String platformName;
    private Integer scanCount;
    private Integer goodReviewCount;
    private BigDecimal score;
    private String createTime;
    private String updateTime;
}
