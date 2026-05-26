package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linxi.entity.SysMenu;
import com.linxi.entity.SysRoleMenu;
import com.linxi.entity.SysUserRole;
import com.linxi.entity.SysUser;
import com.linxi.mapper.SysMenuMapper;
import com.linxi.mapper.SysRoleMenuMapper;
import com.linxi.mapper.SysUserRoleMapper;
import com.linxi.mapper.SysUserMapper;
import com.linxi.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public List<SysMenu> getAllMenuTree() {
        // 管理页面：显示所有菜单（不过滤状态）
        List<SysMenu> allMenus = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSortNo)
        );
        return buildTree(allMenus, 0L);
    }


    @Override
    public List<SysMenu> getUserMenus(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        // 查询用户角色
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        // 查询角色菜单
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds)
        );
        if (roleMenus.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());

        // 查询菜单（过滤停用的和不可见的）
        List<SysMenu> menus = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, menuIds)
                        .eq(SysMenu::getVisible, 1)
                        .eq(SysMenu::getStatus, 1)
                        .orderByAsc(SysMenu::getSortNo)
        );

        return buildTree(menus, 0L);
    }

    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            // 从UserDetails获取username，再查数据库获取ID
            String username = ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername();
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
            );
            return user != null ? user.getId() : null;
        }
        return null;
    }

    @Override
    public void save(SysMenu menu) {
        menu.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        menu.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (menu.getStatus() == null) menu.setStatus(1);
        if (menu.getVisible() == null) menu.setVisible(1);
        if (menu.getSortNo() == null) menu.setSortNo(0);
        sysMenuMapper.insert(menu);
    }

    @Override
    public void update(SysMenu menu) {
        menu.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        sysMenuMapper.updateById(menu);
    }

    @Override
    public void delete(Long id) {
        // 先删除子菜单
        sysMenuMapper.delete(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)
        );
        // 删除角色菜单关联
        sysRoleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id)
        );
        // 删除菜单本身
        sysMenuMapper.deleteById(id);
    }

    private List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId() != null && menu.getParentId().equals(parentId)) {
                List<SysMenu> children = buildTree(menus, menu.getId());
                if (!children.isEmpty()) {
                    menu.setChildren(children);
                }
                tree.add(menu);
            }
        }
        return tree;
    }
}
