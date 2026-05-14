package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.entity.SysMenu;
import com.linxi.entity.SysRole;
import com.linxi.entity.SysRoleMenu;
import com.linxi.mapper.SysMenuMapper;
import com.linxi.mapper.SysRoleMapper;
import com.linxi.mapper.SysRoleMenuMapper;
import com.linxi.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Override
    public SysRole getById(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    @Override
    public Page<SysRole> page(Integer pageNo, Integer pageSize) {
        Page<SysRole> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysRole::getRoleType);
        return sysRoleMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysRole> listAll() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1).orderByAsc(SysRole::getRoleType);
        return sysRoleMapper.selectList(wrapper);
    }

    @Override
    public boolean save(SysRole role) {
        role.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        role.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return sysRoleMapper.insert(role) > 0;
    }

    @Override
    public boolean update(SysRole role) {
        role.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return sysRoleMapper.updateById(role) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return sysRoleMapper.deleteById(id) > 0;
    }

    @Override
    public List<SysMenu> getRoleMenus(Long roleId) {
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId)
        );
        List<Long> menuIds = new ArrayList<>();
        for (SysRoleMenu rm : roleMenus) {
            menuIds.add(rm.getMenuId());
        }
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        return sysMenuMapper.selectBatchIds(menuIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRoleMenus(Long roleId, List<Long> menuIds) {
        // 删除原有关联
        sysRoleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId)
        );
        // 保存新关联
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenu.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                sysRoleMenuMapper.insert(roleMenu);
            }
        }
        return true;
    }
}
