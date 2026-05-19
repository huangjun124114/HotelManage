package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("investor_store")
public class InvestorStore {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long investorId;
    private Long storeId;
    private BigDecimal investAmount;
    private BigDecimal investmentRatio;
    private String authStartDate;
    private String authEndDate;
    private Integer canViewRevenue;
    private Integer canViewChannel;
    private Integer canViewScore;
    private Integer canExport;
    private Integer status;
    private String createTime;
    private String updateTime;

    // 非数据库字段，用于展示门店名称
    @TableField(exist = false)
    private String storeName;
}
