package com.linxi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDTO {

    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private Integer userType;
    private List<String> roles;
    private List<String> permissions;
}
