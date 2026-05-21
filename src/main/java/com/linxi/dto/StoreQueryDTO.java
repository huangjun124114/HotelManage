package com.linxi.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StoreQueryDTO extends PageQueryDTO {

    private String storeName;
    private String storeCode;
    private String city;
    private Integer status;
    /** 数据范围过滤：允许查询的门店ID列表（null表示不限） */
    private List<Long> storeIds;
}
