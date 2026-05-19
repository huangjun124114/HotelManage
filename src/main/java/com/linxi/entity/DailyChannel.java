package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("daily_channel")
public class DailyChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dailyReportId;
    private Long storeId;
    private LocalDate reportDate;
    private Long templateId;

    // 好评数字段
    private Integer ctripGoodReviewCount;
    private Integer meituanGoodReviewCount;
    private Integer lyGoodReviewCount;
    private Integer qunarGoodReviewCount;
    private Integer zhixingGoodReviewCount;
    private Integer fliggyGoodReviewCount;

    // 评分字段
    private BigDecimal ctripScore;
    private BigDecimal lyScore;
    private BigDecimal qunarScore;
    private BigDecimal zhixingScore;
    private BigDecimal meituanScore;
    private BigDecimal fliggyScore;

    // 预留扩展字段（10个）
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
    private String ext6;
    private String ext7;
    private String ext8;
    private String ext9;
    private String ext10;

    // 元数据字段
    @TableLogic
    private Integer deleted;
    private String createTime;
    private String updateTime;
}
