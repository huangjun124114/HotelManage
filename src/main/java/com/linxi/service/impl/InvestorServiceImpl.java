package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.dto.InvestorQueryDTO;
import com.linxi.entity.Investor;
import com.linxi.entity.InvestorStore;
import com.linxi.entity.SysUser;
import com.linxi.entity.SysUserRole;
import com.linxi.mapper.InvestorMapper;
import com.linxi.mapper.InvestorStoreMapper;
import com.linxi.mapper.SysUserMapper;
import com.linxi.mapper.SysUserRoleMapper;
import com.linxi.service.InvestorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class InvestorServiceImpl implements InvestorService {

    @Autowired
    private InvestorMapper investorMapper;

    @Autowired
    private InvestorStoreMapper investorStoreMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 投资者角色ID固定为6
    private static final Long INVESTOR_ROLE_ID = 6L;
    // 投资者默认密码
    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public Investor getById(Long id) {
        Investor investor = investorMapper.selectById(id);
        if (investor != null) {
            // 查询关联的门店
            List<InvestorStore> stores = investorStoreMapper.selectList(
                    new LambdaQueryWrapper<InvestorStore>()
                            .eq(InvestorStore::getInvestorId, id)
            );
            investor.setStoreList(stores);
        }
        return investor;
    }

    @Override
    public Page<Investor> page(InvestorQueryDTO query) {
        Page<Investor> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<Investor> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getInvestorName() != null, Investor::getInvestorName, query.getInvestorName())
                .like(query.getPhone() != null, Investor::getPhone, query.getPhone())
                .eq(query.getStatus() != null, Investor::getStatus, query.getStatus())
                .orderByDesc(Investor::getId);
        return investorMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Investor> listAll() {
        return investorMapper.selectList(
                new LambdaQueryWrapper<Investor>()
                        .orderByDesc(Investor::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Investor investor) {
        if (investor.getInvestorName() == null || investor.getInvestorName().isEmpty()) {
            throw new BusinessException("投资人名称不能为空");
        }

        // 检查phone唯一性（仅当phone非空时）
        if (investor.getPhone() != null && !investor.getPhone().isEmpty()) {
            Investor existPhone = investorMapper.selectOne(
                    new LambdaQueryWrapper<Investor>().eq(Investor::getPhone, investor.getPhone())
            );
            if (existPhone != null) {
                throw new BusinessException("投资人手机号已存在");
            }
        }

        investor.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        investor.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (investor.getStatus() == null) {
            investor.setStatus(1);
        }

        // 自动创建投资人系统账户
        SysUser user = createInvestorUser(investor);
        investor.setUserId(user.getId());
        investor.setGeneratedPassword(DEFAULT_PASSWORD);

        return investorMapper.insert(investor) > 0;
    }

    /**
     * 为投资人创建系统用户账户
     */
    private SysUser createInvestorUser(Investor investor) {
        String username = investor.getPhone();
        if (username == null || username.isEmpty()) {
            // 如果手机号为空，使用investor_前缀+时间戳生成用户名
            username = "investor_" + System.currentTimeMillis();
        }

        // 检查用户名是否已存在
        SysUser existUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (existUser != null) {
            username = username + "_" + System.currentTimeMillis();
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setRealName(investor.getInvestorName());
        user.setPhone(investor.getPhone());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setUserType(3); // 3 = 投资人
        user.setStatus(1);
        user.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        user.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        sysUserMapper.insert(user);

        // 分配INVESTOR角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(INVESTOR_ROLE_ID);
        userRole.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        sysUserRoleMapper.insert(userRole);

        log.info("为投资人[{}]创建系统账户成功，用户名：{}，用户ID：{}", investor.getInvestorName(), username, user.getId());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Investor investor) {
        if (investor.getId() == null) {
            throw new BusinessException("投资人ID不能为空");
        }
        Investor existing = investorMapper.selectById(investor.getId());
        if (existing == null) {
            throw new BusinessException("投资人不存在");
        }

        // 检查phone唯一性（仅当phone非空且与原值不同时）
        if (investor.getPhone() != null && !investor.getPhone().isEmpty()
                && !investor.getPhone().equals(existing.getPhone())) {
            Investor existPhone = investorMapper.selectOne(
                    new LambdaQueryWrapper<Investor>()
                            .eq(Investor::getPhone, investor.getPhone())
                            .ne(Investor::getId, investor.getId())
            );
            if (existPhone != null) {
                throw new BusinessException("投资人手机号已存在");
            }
        }

        investor.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        // 不允许通过此接口修改createTime和createBy
        investor.setCreateTime(null);
        investor.setCreateBy(null);
        return investorMapper.updateById(investor) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        if (id == null) {
            throw new BusinessException("投资人ID不能为空");
        }
        Investor investor = investorMapper.selectById(id);
        if (investor != null && investor.getUserId() != null) {
            // 删除关联的系统用户
            sysUserMapper.deleteById(investor.getUserId());
            // 删除用户角色关联
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, investor.getUserId()));
        }
        // 删除关联的门店关系
        investorStoreMapper.delete(new LambdaQueryWrapper<InvestorStore>()
                .eq(InvestorStore::getInvestorId, id));
        return investorMapper.deleteById(id) > 0;
    }
}
