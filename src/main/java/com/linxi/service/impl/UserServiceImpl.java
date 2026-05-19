package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.dto.UserQueryDTO;
import com.linxi.entity.SysUser;
import com.linxi.entity.SysUserRole;
import com.linxi.entity.SysUserStore;
import com.linxi.entity.Store;
import com.linxi.entity.SysRole;
import com.linxi.mapper.SysUserMapper;
import com.linxi.mapper.SysUserRoleMapper;
import com.linxi.mapper.SysUserStoreMapper;
import com.linxi.mapper.StoreMapper;
import com.linxi.mapper.SysRoleMapper;
import com.linxi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

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
    private StoreMapper storeMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

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
        // 填充门店/角色信息
        List<SysUser> singleList = new ArrayList<>();
        singleList.add(user);
        fillStoreInfo(singleList);
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

        // 填充门店关联信息
        fillStoreInfo(result.getRecords());

        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    /**
     * 填充用户的多门店关联信息和角色信息
     */
    private void fillStoreInfo(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());

        // ===== 门店信息 =====
        LambdaQueryWrapper<SysUserStore> storeWrapper = new LambdaQueryWrapper<>();
        storeWrapper.in(SysUserStore::getUserId, userIds);
        List<SysUserStore> userStores = sysUserStoreMapper.selectList(storeWrapper);

        // 获取所有关联的门店ID
        List<Long> storeIds = userStores.stream()
                .map(SysUserStore::getStoreId).distinct().collect(Collectors.toList());

        Map<Long, Store> storeMap = new HashMap<>();
        if (!storeIds.isEmpty()) {
            List<Store> stores = storeMapper.selectList(
                    new LambdaQueryWrapper<Store>().in(Store::getId, storeIds));
            storeMap = stores.stream().collect(Collectors.toMap(Store::getId, s -> s, (a, b) -> a));
        }

        // 用户→门店列表映射
        Map<Long, List<SysUserStore>> userStoreListMap = userStores.stream()
                .collect(Collectors.groupingBy(SysUserStore::getUserId));

        // ===== 角色信息 =====
        LambdaQueryWrapper<SysUserRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysUserRole::getUserId, userIds);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(roleWrapper);

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());

        Map<Long, SysRole> roleMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            roleMap = roles.stream().collect(Collectors.toMap(SysRole::getId, r -> r, (a, b) -> a));
        }

        Map<Long, List<SysUserRole>> userRoleListMap = userRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId));

        // 填充每个用户
        for (SysUser user : users) {
            // 门店信息
            List<SysUserStore> usList = userStoreListMap.getOrDefault(user.getId(), Collections.emptyList());
            List<Long> uStoreIds = new ArrayList<>();
            List<String> uStoreNames = new ArrayList<>();
            for (SysUserStore us : usList) {
                Store store = storeMap.get(us.getStoreId());
                if (store != null) {
                    uStoreIds.add(store.getId());
                    uStoreNames.add(store.getStoreName());
                }
            }
            user.setStoreIds(uStoreIds);
            user.setStoreNames(uStoreNames);
            // 兼容旧字段：取第一个门店
            if (!uStoreIds.isEmpty()) {
                user.setStoreId(uStoreIds.get(0));
                user.setStoreName(uStoreNames.get(0));
            }

            // 角色信息
            List<SysUserRole> urList = userRoleListMap.getOrDefault(user.getId(), Collections.emptyList());
            List<Long> uRoleIds = new ArrayList<>();
            List<String> uRoleNames = new ArrayList<>();
            for (SysUserRole ur : urList) {
                SysRole role = roleMap.get(ur.getRoleId());
                if (role != null) {
                    uRoleIds.add(role.getId());
                    uRoleNames.add(role.getRoleName());
                }
            }
            user.setRoleIds(uRoleIds);
            user.setRoleNames(uRoleNames);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysUser user) {
        // 检查用户名是否存在
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername())
        );
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }

        // 检查phone唯一性（仅当phone非空时）
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            SysUser existPhone = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, user.getPhone())
            );
            if (existPhone != null) {
                throw new BusinessException("手机号已存在");
            }
        }

        // 保存门店和角色ID，插入后使用
        List<Long> storeIds = user.getStoreIds();
        List<Long> roleIds = user.getRoleIds();

        user.setPassword(passwordEncoder.encode(user.getPassword() != null ? user.getPassword() : "123456"));
        user.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        user.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getUserType() == null) {
            user.setUserType(1);
        }
        boolean result = sysUserMapper.insert(user) > 0;

        // 保存门店关联
        if (storeIds != null && !storeIds.isEmpty()) {
            for (Long storeId : storeIds) {
                SysUserStore userStore = new SysUserStore();
                userStore.setUserId(user.getId());
                userStore.setStoreId(storeId);
                userStore.setPermissionType(1);
                userStore.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                sysUserStoreMapper.insert(userStore);
            }
        }

        // 保存角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRole.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                sysUserRoleMapper.insert(userRole);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SysUser user) {
        if (user.getId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        SysUser existing = sysUserMapper.selectById(user.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查phone唯一性（仅当phone非空且与原值不同时）
        if (user.getPhone() != null && !user.getPhone().isEmpty()
                && !user.getPhone().equals(existing.getPhone())) {
            SysUser existPhone = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getPhone, user.getPhone())
                            .ne(SysUser::getId, user.getId())
            );
            if (existPhone != null) {
                throw new BusinessException("手机号已存在");
            }
        }

        // 保存门店和角色ID
        List<Long> storeIds = user.getStoreIds();
        List<Long> roleIds = user.getRoleIds();

        user.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        // 不更新密码
        user.setPassword(null);
        boolean result = sysUserMapper.updateById(user) > 0;

        // 更新门店关联
        if (storeIds != null) {
            assignStores(user.getId(), storeIds);
        }

        // 更新角色关联
        if (roleIds != null) {
            assignRoles(user.getId(), roleIds);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 先清理子表关联记录
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
        );
        sysUserStoreMapper.delete(
                new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, id)
        );
        // 逻辑删除用户
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
