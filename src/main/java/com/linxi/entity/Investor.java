package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@TableName("investor")
public class Investor {

    @TableId(type = IdType.AUTO)
    private Long id;
    @JsonProperty("name")
    private String investorName;
    private String phone;
    private String email;
    private Long userId;
    private Integer status;
    private String remark;
    @TableLogic
    private Integer deleted;
    private String createTime;
    private Long createBy;
    private String updateTime;
    private Long updateBy;

    // 非数据库字段，存储关联的门店列表
    @TableField(exist = false)
    private List<InvestorStore> storeList;
}
