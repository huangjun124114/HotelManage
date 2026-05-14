package com.linxi.service;

import com.linxi.dto.LoginDTO;
import com.linxi.dto.LoginResultDTO;

public interface AuthService {

    /**
     * 用户登录
     */
    LoginResultDTO login(LoginDTO loginDTO);

    /**
     * 获取当前登录用户信息
     */
    LoginResultDTO getUserInfo(String username);
}
