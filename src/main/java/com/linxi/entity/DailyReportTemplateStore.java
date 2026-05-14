package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("daily_report_template_store")
public class DailyReportTemplateStore {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long storeId;
    private String createTime;
}
