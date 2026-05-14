package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("daily_report_summary")
public class DailyReportSummary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private Long storeId;
    private String reportDate;
    private String reportMonth;
    private BigDecimal ownRoomCount;
    private BigDecimal repairRoomCount;
    private BigDecimal hourlyRoomCount;
    private BigDecimal roomNights;
    private BigDecimal occupancyRate;
    private BigDecimal adr;
    private BigDecimal revpar;
    private BigDecimal dailyRoomFee;
    private BigDecimal hourlyRoomFee;
    private BigDecimal otherFee;
    private BigDecimal totalRevenue;
    private BigDecimal depositAmount;
    private String createTime;
    private String updateTime;
}
