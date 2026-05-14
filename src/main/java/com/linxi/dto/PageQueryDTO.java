package com.linxi.dto;

import lombok.Data;

@Data
public class PageQueryDTO {

    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
