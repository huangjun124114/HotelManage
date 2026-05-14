package com.linxi.service;

import com.linxi.entity.SysMenu;

import java.util.List;

public interface MenuService {

    /**
     * 获取菜单树
     */
    List<SysMenu> getMenuTree();

    /**
     * 获取用户菜单（根据角色）
     */
    List<SysMenu> getUserMenus(Long userId);
}
