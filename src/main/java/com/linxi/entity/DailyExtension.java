package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("daily_extension")
public class DailyExtension {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dailyReportId;
    private Long storeId;
    private LocalDate reportDate;
    private Long templateId;

    // 30个弹性域字段
    @TableField("ext_field_1")
    private String extField1;
    @TableField("ext_field_2")
    private String extField2;
    @TableField("ext_field_3")
    private String extField3;
    @TableField("ext_field_4")
    private String extField4;
    @TableField("ext_field_5")
    private String extField5;
    @TableField("ext_field_6")
    private String extField6;
    @TableField("ext_field_7")
    private String extField7;
    @TableField("ext_field_8")
    private String extField8;
    @TableField("ext_field_9")
    private String extField9;
    @TableField("ext_field_10")
    private String extField10;
    @TableField("ext_field_11")
    private String extField11;
    @TableField("ext_field_12")
    private String extField12;
    @TableField("ext_field_13")
    private String extField13;
    @TableField("ext_field_14")
    private String extField14;
    @TableField("ext_field_15")
    private String extField15;
    @TableField("ext_field_16")
    private String extField16;
    @TableField("ext_field_17")
    private String extField17;
    @TableField("ext_field_18")
    private String extField18;
    @TableField("ext_field_19")
    private String extField19;
    @TableField("ext_field_20")
    private String extField20;
    @TableField("ext_field_21")
    private String extField21;
    @TableField("ext_field_22")
    private String extField22;
    @TableField("ext_field_23")
    private String extField23;
    @TableField("ext_field_24")
    private String extField24;
    @TableField("ext_field_25")
    private String extField25;
    @TableField("ext_field_26")
    private String extField26;
    @TableField("ext_field_27")
    private String extField27;
    @TableField("ext_field_28")
    private String extField28;
    @TableField("ext_field_29")
    private String extField29;
    @TableField("ext_field_30")
    private String extField30;

    // 元数据字段
    @TableLogic
    private Integer deleted;
    private String createTime;
    private String updateTime;
}
