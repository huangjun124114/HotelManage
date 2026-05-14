package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("store")
public class Store {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String storeCode;
    private String storeName;
    private String shortName;
    private String brand;
    private String regionName;
    private String city;
    private String address;
    private Integer ownRoomCount;
    private Long managerUserId;
    private String contactPhone;
    private String openDate;
    private Integer status;
    private String remark;
    @TableLogic
    private Integer deleted;
    private String createTime;
    private Long createBy;
    private String updateTime;
    private Long updateBy;
}
