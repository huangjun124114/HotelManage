package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.dto.UserQueryDTO;
import com.linxi.entity.SysUser;

import java.util.List;

public interface UserService {

    SysUser getById(Long id);

    Page<SysUser> page(UserQueryDTO query);

    boolean save(SysUser user);

    boolean update(SysUser user);

    boolean delete(Long id);

    boolean resetPassword(Long id);

    boolean updateStatus(Long id, Integer status);

    boolean assignRoles(Long userId, List<Long> roleIds);

    boolean assignStores(Long userId, List<Long> storeIds);
}
