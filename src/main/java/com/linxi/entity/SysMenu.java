package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuCode;
    private Integer menuType;
    private String path;
    private String component;
    private String permissionCode;
    private Integer sortNo;
    private Integer visible;
    private Integer status;
    private String createTime;
    private String updateTime;

    @TableField(exist = false)
    private List<SysMenu> children;
}
