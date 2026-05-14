package com.linxi.controller;

import com.linxi.common.Result;
import com.linxi.dto.LoginDTO;
import com.linxi.dto.LoginResultDTO;
import com.linxi.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginResultDTO result = authService.login(loginDTO);
        return Result.success("登录成功", result);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功");
    }

    @GetMapping("/userinfo")
    public Result<LoginResultDTO> userInfo(Principal principal) {
        if (principal == null) {
            return Result.error(401, "未登录");
        }
        LoginResultDTO userInfo = authService.getUserInfo(principal.getName());
        return Result.success(userInfo);
    }
}
