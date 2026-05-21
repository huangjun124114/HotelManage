package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linxi.common.BusinessException;
import com.linxi.entity.Investor;
import com.linxi.entity.InvestorStore;
import com.linxi.entity.SysUserStore;
import com.linxi.mapper.InvestorMapper;
import com.linxi.mapper.InvestorStoreMapper;
import com.linxi.mapper.SysUserStoreMapper;
import com.linxi.service.InvestorRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 投资关系服务实现
 */
@Slf4j
@Service
public class InvestorRelationServiceImpl implements InvestorRelationService {

    @Autowired
    private InvestorStoreMapper investorStoreMapper;

    @Autowired
    private InvestorMapper investorMapper;

    @Autowired
    private SysUserStoreMapper sysUserStoreMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InvestorStore relation) {
        // 1. 检查记录是否存在
        InvestorStore existing = investorStoreMapper.selectById(relation.getId());
        if (existing == null) {
            throw new BusinessException("投资关系记录不存在");
        }

        Long oldStoreId = existing.getStoreId();
        Long newStoreId = relation.getStoreId();

        // 2. 更新可修改字段：investAmount、investmentRatio、authStartDate、authEndDate
        if (relation.getInvestAmount() != null) {
            existing.setInvestAmount(relation.getInvestAmount());
        }
        if (relation.getInvestmentRatio() != null) {
            existing.setInvestmentRatio(relation.getInvestmentRatio());
        }
        if (relation.getAuthStartDate() != null) {
            existing.setAuthStartDate(relation.getAuthStartDate());
        }
        if (relation.getAuthEndDate() != null) {
            existing.setAuthEndDate(relation.getAuthEndDate());
        }

        String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        existing.setUpdateTime(now);

        // 3. 如果 storeId 变更，同步更新 sys_user_store
        boolean storeChanged = newStoreId != null && !newStoreId.equals(oldStoreId);
        if (storeChanged) {
            existing.setStoreId(newStoreId);
        }

        investorStoreMapper.updateById(existing);

        if (storeChanged) {
            Investor investor = investorMapper.selectById(existing.getInvestorId());
            if (investor != null && investor.getUserId() != null) {
                // 删除旧门店关联
                sysUserStoreMapper.delete(new LambdaQueryWrapper<SysUserStore>()
                        .eq(SysUserStore::getUserId, investor.getUserId())
                        .eq(SysUserStore::getStoreId, oldStoreId));
                // 新增新门店关联（避免重复插入）
                Long existCount = sysUserStoreMapper.selectCount(
                        new LambdaQueryWrapper<SysUserStore>()
                                .eq(SysUserStore::getUserId, investor.getUserId())
                                .eq(SysUserStore::getStoreId, newStoreId)
                );
                if (existCount == 0) {
                    SysUserStore userStore = new SysUserStore();
                    userStore.setUserId(investor.getUserId());
                    userStore.setStoreId(newStoreId);
                    userStore.setPermissionType(1); // 只读权限
                    userStore.setCreateTime(now);
                    sysUserStoreMapper.insert(userStore);
                }
            }
        }
    }
}
