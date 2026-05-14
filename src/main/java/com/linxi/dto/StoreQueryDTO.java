package com.linxi.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StoreQueryDTO extends PageQueryDTO {

    private String storeName;
    private String storeCode;
    private String city;
    private Integer status;
}
