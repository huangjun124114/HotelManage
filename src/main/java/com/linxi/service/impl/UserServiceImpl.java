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
import com.linxi.mapper.SysUserMapper;
import com.linxi.mapper.SysUserRoleMapper;
import com.linxi.mapper.SysUserStoreMapper;
import com.linxi.mapper.StoreMapper;
import com.linxi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
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

        // 填充门店关联信息
        fillStoreInfo(result.getRecords());

        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    /**
     * 填充用户的门店关联信息（门店名称、门店编码）
     */
    private void fillStoreInfo(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        // 获取所有用户ID
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());

        // 查询这些用户的门店关联关系
        LambdaQueryWrapper<SysUserStore> storeWrapper = new LambdaQueryWrapper<>();
        storeWrapper.in(SysUserStore::getUserId, userIds);
        List<SysUserStore> userStores = sysUserStoreMapper.selectList(storeWrapper);

        if (userStores.isEmpty()) {
            return;
        }

        // 获取所有关联的门店ID
        List<Long> storeIds = userStores.stream()
                .map(SysUserStore::getStoreId)
                .distinct()
                .collect(Collectors.toList());

        // 查询门店信息，构建ID到门店的映射
        LambdaQueryWrapper<Store> storeQueryWrapper = new LambdaQueryWrapper<>();
        storeQueryWrapper.in(Store::getId, storeIds);
        List<Store> stores = storeMapper.selectList(storeQueryWrapper);
        Map<Long, Store> storeMap = stores.stream()
                .collect(Collectors.toMap(Store::getId, s -> s, (a, b) -> a));

        // 为每个用户设置门店信息（取第一个关联的门店）
        Map<Long, SysUserStore> userStoreMap = userStores.stream()
                .collect(Collectors.toMap(SysUserStore::getUserId, us -> us, (a, b) -> a));

        for (SysUser user : users) {
            SysUserStore userStore = userStoreMap.get(user.getId());
            if (userStore != null) {
                Store store = storeMap.get(userStore.getStoreId());
                if (store != null) {
                    user.setStoreId(store.getId());
                    user.setStoreName(store.getStoreName());
                    user.setStoreCode(store.getStoreCode());
                }
            }
        }
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
