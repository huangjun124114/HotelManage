package com.linxi.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DailyReportQueryDTO extends PageQueryDTO {

    private Long storeId;
    private String startDate;
    private String endDate;
    private Integer status;
}
