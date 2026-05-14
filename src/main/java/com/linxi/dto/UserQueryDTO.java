package com.linxi.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQueryDTO {

    private String username;
    private String realName;
    private String phone;
    private Integer userType;
    private Integer status;
}
