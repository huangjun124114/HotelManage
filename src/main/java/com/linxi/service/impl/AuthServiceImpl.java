package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linxi.common.BusinessException;
import com.linxi.dto.LoginDTO;
import com.linxi.dto.LoginResultDTO;
import com.linxi.entity.SysConfig;
import com.linxi.entity.SysUser;
import com.linxi.mapper.SysConfigMapper;
import com.linxi.mapper.SysUserMapper;
import com.linxi.security.JwtTokenUtil;
import com.linxi.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /** 登录失败计数（内存暂存） */
    private final Map<String, LoginFailRecord> loginFailMap = new ConcurrentHashMap<>();

    private static class LoginFailRecord {
        int count;
        long lockUntil;
    }

    @Override
    public LoginResultDTO login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();

        // 检查是否被锁定
        LoginFailRecord record = loginFailMap.get(username);
        if (record != null && record.lockUntil > System.currentTimeMillis()) {
            throw new BusinessException("账号已被锁定，请稍后再试");
        }

        // 查询用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            recordFail(username);
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            recordFail(username);
            throw new BusinessException("用户名或密码错误");
        }

        // 登录成功，清除失败记录
        loginFailMap.remove(username);

        // 更新最后登录时间
        user.setLastLoginTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        sysUserMapper.updateById(user);

        // 生成Token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRealName());

        return LoginResultDTO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .userType(user.getUserType())
                .roles(new ArrayList<>())
                .permissions(new ArrayList<>())
                .build();
    }

    @Override
    public LoginResultDTO getUserInfo(String username) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return LoginResultDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .userType(user.getUserType())
                .roles(new ArrayList<>())
                .permissions(new ArrayList<>())
                .build();
    }

    private void recordFail(String username) {
        LoginFailRecord record = loginFailMap.computeIfAbsent(username, k -> new LoginFailRecord());
        record.count++;

        int maxFail = 5;
        int lockMinutes = 10;

        try {
            SysConfig maxFailConfig = sysConfigMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, "login.max_fail_count")
            );
            if (maxFailConfig != null) {
                maxFail = Integer.parseInt(maxFailConfig.getConfigValue());
            }

            SysConfig lockConfig = sysConfigMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, "login.lock_minutes")
            );
            if (lockConfig != null) {
                lockMinutes = Integer.parseInt(lockConfig.getConfigValue());
            }
        } catch (Exception e) {
            log.warn("读取登录锁定配置失败，使用默认值");
        }

        if (record.count >= maxFail) {
            record.lockUntil = System.currentTimeMillis() + lockMinutes * 60 * 1000L;
            log.warn("用户 {} 登录失败{}次，锁定{}分钟", username, record.count, lockMinutes);
        }
    }
}
