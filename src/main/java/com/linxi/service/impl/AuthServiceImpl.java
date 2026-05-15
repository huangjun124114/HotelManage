package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linxi.common.BusinessException;
import com.linxi.dto.LoginDTO;
import com.linxi.dto.LoginResultDTO;
import com.linxi.entity.SysConfig;
import com.linxi.entity.SysMenu;
import com.linxi.entity.SysRole;
import com.linxi.entity.SysRoleMenu;
import com.linxi.entity.SysUser;
import com.linxi.entity.SysUserRole;
import com.linxi.mapper.SysConfigMapper;
import com.linxi.mapper.SysMenuMapper;
import com.linxi.mapper.SysRoleMapper;
import com.linxi.mapper.SysRoleMenuMapper;
import com.linxi.mapper.SysUserMapper;
import com.linxi.mapper.SysUserRoleMapper;
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
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

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

        // 加载用户角色和权限
        List<String> roles = loadUserRoles(user.getId());
        List<String> permissions = loadUserPermissions(user.getId());

        // 生成Token（带角色权限）
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRealName(), roles, permissions);

        return LoginResultDTO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .userType(user.getUserType())
                .roles(roles)
                .permissions(permissions)
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

        // 加载用户角色和权限
        List<String> roles = loadUserRoles(user.getId());
        List<String> permissions = loadUserPermissions(user.getId());

        return LoginResultDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .userType(user.getUserType())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    /**
     * 加载用户角色列表
     */
    private List<String> loadUserRoles(Long userId) {
        log.info("加载用户角色 userId={}", userId);
        try {
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
            );
            log.info("用户角色记录数: {}", userRoles.size());
            if (userRoles.isEmpty()) {
                // 如果没有关联角色，根据userType添加默认角色
                List<String> defaultRoles = getDefaultRoles(userId);
                log.info("使用默认角色: {}", defaultRoles);
                return defaultRoles;
            }
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            log.info("角色IDs: {}", roleIds);
            List<SysRole> roles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)
            );
            List<String> result = roles.stream()
                    .map(r -> "ROLE_" + r.getRoleCode())
                    .collect(Collectors.toList());
            log.info("加载到的角色: {}", result);
            return result;
        } catch (Exception e) {
            log.error("加载用户角色失败: {}", e.getMessage(), e);
            return getDefaultRoles(userId);
        }
    }

    /**
     * 根据userType获取默认角色
     */
    private List<String> getDefaultRoles(Long userId) {
        List<String> defaultRoles = new ArrayList<>();
        try {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null && user.getUserType() != null) {
                switch (user.getUserType()) {
                    case 1: // 超级管理员
                        defaultRoles.add("ROLE_SUPER_ADMIN");
                        break;
                    case 2: // 门店经理
                        defaultRoles.add("ROLE_STORE_MANAGER");
                        break;
                    case 3: // 门店员工
                        defaultRoles.add("ROLE_STORE_STAFF");
                        break;
                    default:
                        defaultRoles.add("ROLE_USER");
                }
            }
        } catch (Exception e) {
            defaultRoles.add("ROLE_USER");
        }
        return defaultRoles;
    }

    /**
     * 加载用户权限列表
     */
    private List<String> loadUserPermissions(Long userId) {
        try {
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
            );
            if (userRoles.isEmpty()) {
                return getDefaultPermissions(userId);
            }
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());

            // 通过sys_role_menu获取菜单ID列表
            Set<Long> allMenuIds = new HashSet<>();
            for (Long roleId : roleIds) {
                List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId)
                );
                roleMenus.forEach(rm -> allMenuIds.add(rm.getMenuId()));
            }

            // 获取所有菜单权限码
            if (!allMenuIds.isEmpty()) {
                List<SysMenu> allMenus = sysMenuMapper.selectList(
                        new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, allMenuIds)
                );
                return allMenus.stream()
                        .map(SysMenu::getMenuCode)
                        .filter(code -> code != null && !code.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
            }
            return getDefaultPermissions(userId);
        } catch (Exception e) {
            log.warn("加载用户权限失败: {}", e.getMessage());
            return getDefaultPermissions(userId);
        }
    }

    /**
     * 根据userType获取默认权限
     */
    private List<String> getDefaultPermissions(Long userId) {
        try {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null && user.getUserType() != null) {
                switch (user.getUserType()) {
                    case 1: // 超级管理员 - 全部权限
                        List<String> adminPerms = new ArrayList<>();
                        adminPerms.add("system:user");
                        adminPerms.add("system:role");
                        adminPerms.add("system:menu");
                        adminPerms.add("report:fill");
                        adminPerms.add("report:audit");
                        adminPerms.add("report:view");
                        return adminPerms;
                    case 2: // 门店经理
                        List<String> managerPerms = new ArrayList<>();
                        managerPerms.add("report:fill");
                        managerPerms.add("report:audit");
                        managerPerms.add("report:view");
                        return managerPerms;
                    case 3: // 门店员工
                        List<String> staffPerms = new ArrayList<>();
                        staffPerms.add("report:fill");
                        staffPerms.add("report:view");
                        return staffPerms;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
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
