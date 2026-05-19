package com.linxi.service;

import com.linxi.entity.SysMenu;

import java.util.List;

public interface MenuService {

    /**
     * 获取所有菜单树（管理页面用，包含停用菜单）
     */
    List<SysMenu> getAllMenuTree();

    /**
     * 获取启用的菜单树（旧方法兼容）
     */
    List<SysMenu> getMenuTree();

    /**
     * 获取用户菜单（根据角色，过滤停用菜单）
     */
    List<SysMenu> getUserMenus(Long userId);

    /**
     * 获取当前登录用户ID
     */
    Long getCurrentUserId();

    /**
     * 新增菜单
     */
    void save(SysMenu menu);

    /**
     * 更新菜单
     */
    void update(SysMenu menu);

    /**
     * 删除菜单
     */
    void delete(Long id);
}
