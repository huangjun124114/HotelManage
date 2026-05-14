package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("daily_report_value")
public class DailyReportValue {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private Long fieldId;
    private String fieldCode;
    private String fieldName;
    private String fieldType;
    private String valueText;
    private BigDecimal valueNumber;
    private String valueDate;
    private String valueJson;
    private Integer sortNo;
    private String createTime;
    private String updateTime;
}
