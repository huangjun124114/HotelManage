package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String moduleName;
    private String operationType;
    private String businessId;
    private String requestUrl;
    private String requestMethod;
    private String requestParam;
    private String beforeData;
    private String afterData;
    private String ipAddress;
    private Integer resultStatus;
    private String errorMessage;
    private String createTime;
}
