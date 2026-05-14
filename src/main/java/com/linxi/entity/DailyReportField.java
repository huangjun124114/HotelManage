package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("daily_report_field")
public class DailyReportField {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String fieldCode;
    private String fieldName;
    private String fieldCategory;
    private String fieldType;
    private Integer required;
    private Integer visible;
    private Integer readonlyFlag;
    private String defaultValue;
    private String optionJson;
    private String formula;
    private Integer decimalScale;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer summaryFlag;
    private String summaryType;
    private Integer investorVisible;
    private Integer storeVisible;
    private Integer sortNo;
    private Integer status;
    private String remark;
    private String createTime;
    private String updateTime;
}
