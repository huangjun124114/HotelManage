package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("investor")
public class Investor {

    @TableId(type = IdType.AUTO)
    private Long id;
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
}
