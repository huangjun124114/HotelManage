package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("extension_field_definition")
public class ExtensionFieldDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fieldCode;
    private String fieldName;
    private String tableType;
    private String fieldType;
    private String defaultValue;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer maxLength;
    private String options;
    private Integer required;
    private Integer visible;
    private Integer readonly;
    private Integer sortNo;
    private Integer status;
    private String remark;
    private String createTime;
    private String updateTime;
}
