package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String realName;
    private String password;
    private String phone;
    private String email;
    private Integer userType;
    private Integer status;
    private String lastLoginTime;
    private String lastLoginIp;
    private Integer passwordResetRequired;
    private String remark;
    @TableLogic
    private Integer deleted;
    private String createTime;
    private Long createBy;
    private String updateTime;
    private Long updateBy;

    // 关联字段（非数据库字段，用于展示）
    @TableField(exist = false)
    private Long storeId;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String storeCode;
}
