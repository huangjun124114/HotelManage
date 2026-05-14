package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private String configType;
    private String remark;
    @TableLogic
    private Integer deleted;
    private String createTime;
    private Long createBy;
    private String updateTime;
    private Long updateBy;
}
