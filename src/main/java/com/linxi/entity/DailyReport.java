package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("daily_report")
public class DailyReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private String reportDate;
    private String reportMonth;
    private String weekDay;
    private Long templateId;
    private Integer status;
    private Integer abnormalFlag;
    private String abnormalMessage;
    private Long submitUserId;
    private String submitTime;
    private Long lockUserId;
    private String lockTime;
    private String rejectReason;
    private String remark;
    @TableLogic
    private Integer deleted;
    private String createTime;
    private Long createBy;
    private String updateTime;
    private Long updateBy;

    // 关联字段（非数据库字段，用于展示）
    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String storeCode;
}
