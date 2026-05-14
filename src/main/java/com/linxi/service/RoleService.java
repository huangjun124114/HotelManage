package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.entity.SysMenu;
import com.linxi.entity.SysRole;

import java.util.List;

public interface RoleService {

    SysRole getById(Long id);

    Page<SysRole> page(Integer pageNo, Integer pageSize);

    List<SysRole> listAll();

    boolean save(SysRole role);

    boolean update(SysRole role);

    boolean delete(Long id);

    List<SysMenu> getRoleMenus(Long roleId);

    boolean saveRoleMenus(Long roleId, List<Long> menuIds);
}
