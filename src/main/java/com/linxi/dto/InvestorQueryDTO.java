package com.linxi.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InvestorQueryDTO extends PageQueryDTO {

    private String investorName;
    private String phone;
    private Integer status;
}
