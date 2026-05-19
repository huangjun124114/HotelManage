package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dim_date")
public class DimDate {

    @TableId(type = IdType.INPUT)
    private String dateKey;
    private Integer theYear;
    private Integer theQuarter;
    private Integer theMonth;
    private String yearMonth;
    private Integer isoWeek;
    private String yearWeek;
    private String weekStart;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private Integer dayOfYear;
    private Integer isWeekend;
}
