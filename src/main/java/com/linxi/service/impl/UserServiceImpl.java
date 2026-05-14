package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.dto.UserQueryDTO;
import com.linxi.entity.SysUser;
import com.linxi.entity.SysUserRole;
import com.linxi.entity.SysUserStore;
import com.linxi.mapper.SysUserMapper;
import com.linxi.mapper.SysUserRoleMapper;
import com.linxi.mapper.SysUserStoreMapper;
import com.linxi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysUserStoreMapper sysUserStoreMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 隐藏密码
        user.setPassword(null);
        return user;
    }

    @Override
    public Page<SysUser> page(UserQueryDTO query) {
        Page<SysUser> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName())
                .like(StringUtils.hasText(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(query.getUserType() != null, SysUser::getUserType, query.getUserType())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    @Override
    public boolean save(SysUser user) {
        // 检查用户名是否存在
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername())
        );
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        user.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        return sysUserMapper.insert(user) > 0;
    }

    @Override
    public boolean update(SysUser user) {
        user.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        // 不更新密码
        user.setPassword(null);
        return sysUserMapper.updateById(user) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return sysUserMapper.deleteById(id) > 0;
    }

    @Override
    public boolean resetPassword(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        user.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return sysUserMapper.updateById(user) > 0;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return sysUserMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, List<Long> roleIds) {
        // 删除原有关联
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        // 保存新关联
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                sysUserRoleMapper.insert(userRole);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignStores(Long userId, List<Long> storeIds) {
        // 删除原有关联
        sysUserStoreMapper.delete(
                new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, userId)
        );
        // 保存新关联
        if (storeIds != null && !storeIds.isEmpty()) {
            for (Long storeId : storeIds) {
                SysUserStore userStore = new SysUserStore();
                userStore.setUserId(userId);
                userStore.setStoreId(storeId);
                userStore.setPermissionType(1);
                userStore.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                sysUserStoreMapper.insert(userStore);
            }
        }
        return true;
    }
}
