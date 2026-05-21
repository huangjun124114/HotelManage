package com.linxi.security;

import com.linxi.entity.SysUser;
import com.linxi.entity.SysUserStore;
import com.linxi.entity.SysUserRole;
import com.linxi.mapper.SysUserMapper;
import com.linxi.mapper.SysUserStoreMapper;
import com.linxi.mapper.SysUserRoleMapper;
import com.linxi.mapper.SysRoleMapper;
import com.linxi.entity.SysRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 安全上下文工具类 - 获取当前用户信息和数据范围
 */
@Slf4j
@Component
public class SecurityUtils {

    private static SysUserMapper sysUserMapper;
    private static SysUserStoreMapper sysUserStoreMapper;
    private static SysUserRoleMapper sysUserRoleMapper;
    private static SysRoleMapper sysRoleMapper;

    @Autowired
    public void setSysUserMapper(SysUserMapper mapper) {
        SecurityUtils.sysUserMapper = mapper;
    }

    @Autowired
    public void setSysUserStoreMapper(SysUserStoreMapper mapper) {
        SecurityUtils.sysUserStoreMapper = mapper;
    }

    @Autowired
    public void setSysUserRoleMapper(SysUserRoleMapper mapper) {
        SecurityUtils.sysUserRoleMapper = mapper;
    }

    @Autowired
    public void setSysRoleMapper(SysRoleMapper mapper) {
        SecurityUtils.sysRoleMapper = mapper;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            String username = ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername();
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
            );
            return user != null ? user.getId() : null;
        }
        return null;
    }

    /**
     * 获取当前登录用户
     */
    public static SysUser getCurrentUser() {
        Long userId = getCurrentUserId();
        if (userId != null) {
            return sysUserMapper.selectById(userId);
        }
        return null;
    }

    /**
     * 获取当前用户的角色代码列表
     */
    public static List<String> getCurrentUserRoleCodes() {
        Long userId = getCurrentUserId();
        if (userId == null) return new ArrayList<>();

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) return new ArrayList<>();

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)
        );
        return roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
    }

    /**
     * 当前用户是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        return getCurrentUserRoleCodes().contains("SUPER_ADMIN");
    }

    /**
     * 当前用户是否为总部人员（CEO或SUPER_ADMIN）
     */
    public static boolean isHQUser() {
        List<String> roles = getCurrentUserRoleCodes();
        return roles.contains("SUPER_ADMIN") || roles.contains("CEO");
    }

    /**
     * 当前用户是否为投资人
     */
    public static boolean isInvestor() {
        return getCurrentUserRoleCodes().contains("INVESTOR");
    }

    /**
     * 当前用户是否为门店人员（店长或员工）
     */
    public static boolean isStoreUser() {
        List<String> roles = getCurrentUserRoleCodes();
        return roles.contains("STORE_MANAGER") || roles.contains("STORE_STAFF");
    }

    /**
     * 获取当前用户可访问的门店ID列表
     * - SUPER_ADMIN/CEO: 返回null（表示全部门店，无需过滤）
     * - 门店人员: 返回绑定的门店ID
     * - 投资人: 返回投资的门店ID
     */
    public static List<Long> getCurrentUserStoreIds() {
        if (isHQUser()) {
            return null; // 总部人员可看所有门店
        }

        Long userId = getCurrentUserId();
        if (userId == null) return new ArrayList<>();

        // 从sys_user_store获取绑定的门店
        List<SysUserStore> userStores = sysUserStoreMapper.selectList(
                new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, userId)
        );

        if (!userStores.isEmpty()) {
            return userStores.stream().map(SysUserStore::getStoreId).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * 获取当前用户可访问的门店ID列表（空列表表示无权限）
     * 与getCurrentUserStoreIds的区别：这个方法对总部人员也返回所有门店ID
     */
    public static List<Long> getAccessibleStoreIds() {
        List<Long> storeIds = getCurrentUserStoreIds();
        if (storeIds == null) {
            // 总部人员 - 返回null表示全部
            return null;
        }
        return storeIds;
    }
}
