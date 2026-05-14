package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleCode;
    private String roleName;
    private Integer roleType;
    private Integer status;
    private String remark;
    @TableLogic
    private Integer deleted;
    private String createTime;
    private String updateTime;
}
