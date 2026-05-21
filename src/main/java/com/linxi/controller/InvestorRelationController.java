package com.linxi.controller;

import com.linxi.common.BusinessException;
import com.linxi.common.Result;
import com.linxi.entity.Investor;
import com.linxi.entity.InvestorStore;
import com.linxi.entity.Store;
import com.linxi.entity.SysUserStore;
import com.linxi.mapper.InvestorMapper;
import com.linxi.mapper.InvestorStoreMapper;
import com.linxi.mapper.StoreMapper;
import com.linxi.mapper.SysUserStoreMapper;
import com.linxi.service.InvestorRelationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 投资人门店关系管理
 */
@Slf4j
@RestController
@RequestMapping("/api/investor-relations")
public class InvestorRelationController {

    @Autowired
    private InvestorStoreMapper investorStoreMapper;

    @Autowired
    private InvestorMapper investorMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private SysUserStoreMapper sysUserStoreMapper;

    @Autowired
    private InvestorRelationService investorRelationService;

    /**
     * 查询投资人的门店关系列表（含门店名称）
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<List<InvestorStore>> list(@RequestParam Long investorId) {
        List<InvestorStore> stores = investorStoreMapper.selectList(
                new LambdaQueryWrapper<InvestorStore>()
                        .eq(InvestorStore::getInvestorId, investorId)
                        .orderByDesc(InvestorStore::getCreateTime)
        );

        // 填充门店名称
        if (!stores.isEmpty()) {
            List<Long> storeIds = stores.stream()
                    .map(InvestorStore::getStoreId)
                    .distinct()
                    .collect(Collectors.toList());
            List<Store> storeList = storeMapper.selectList(
                    new LambdaQueryWrapper<Store>().in(Store::getId, storeIds)
            );
            Map<Long, String> storeNameMap = storeList.stream()
                    .collect(Collectors.toMap(Store::getId, Store::getStoreName, (a, b) -> a));

            for (InvestorStore store : stores) {
                store.setStoreName(storeNameMap.getOrDefault(store.getStoreId(), ""));
            }
        }
        return Result.success(stores);
    }

    /**
     * 创建投资关系
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> save(@RequestBody Map<String, Object> params) {
        Long investorId = Long.valueOf(params.get("investorId").toString());
        Long storeId = Long.valueOf(params.get("storeId").toString());

        // 校验门店是否处于禁用状态
        Store store = storeMapper.selectById(storeId);
        if (store != null && Integer.valueOf(0).equals(store.getStatus())) {
            throw new BusinessException("该门店已禁用，不能新增投资记录");
        }

        // 检查是否已存在该关系
        Long existCount = investorStoreMapper.selectCount(
                new LambdaQueryWrapper<InvestorStore>()
                        .eq(InvestorStore::getInvestorId, investorId)
                        .eq(InvestorStore::getStoreId, storeId)
        );
        if (existCount > 0) {
            return Result.error("该投资人已关联此门店");
        }

        InvestorStore relation = new InvestorStore();
        relation.setInvestorId(investorId);
        relation.setStoreId(storeId);

        // 投资金额
        if (params.get("investAmount") != null) {
            relation.setInvestAmount(new BigDecimal(params.get("investAmount").toString()));
        }
        // 持股比例
        if (params.get("shareRatio") != null) {
            relation.setInvestmentRatio(new BigDecimal(params.get("shareRatio").toString()));
        }
        // 投资日期
        if (params.get("investDate") != null) {
            relation.setAuthStartDate(params.get("investDate").toString());
        }
        // 撤资日期
        if (params.get("withdrawDate") != null) {
            relation.setAuthEndDate(params.get("withdrawDate").toString());
        }

        relation.setStatus(1);
        String now = cn.hutool.core.date.DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        relation.setCreateTime(now);
        relation.setUpdateTime(now);

        investorStoreMapper.insert(relation);

        // 同步添加到sys_user_store，让投资人用户能看到该门店数据
        Investor investor = investorMapper.selectById(investorId);
        if (investor != null && investor.getUserId() != null) {
            SysUserStore userStore = new SysUserStore();
            userStore.setUserId(investor.getUserId());
            userStore.setStoreId(storeId);
            userStore.setPermissionType(1); // 只读权限
            userStore.setCreateTime(now);
            sysUserStoreMapper.insert(userStore);
        }

        return Result.success();
    }

    /**
     * 编辑投资关系
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        InvestorStore relation = new InvestorStore();
        relation.setId(id);

        if (params.get("investAmount") != null) {
            relation.setInvestAmount(new BigDecimal(params.get("investAmount").toString()));
        }
        if (params.get("investmentRatio") != null) {
            relation.setInvestmentRatio(new BigDecimal(params.get("investmentRatio").toString()));
        } else if (params.get("shareRatio") != null) {
            relation.setInvestmentRatio(new BigDecimal(params.get("shareRatio").toString()));
        }
        if (params.get("authStartDate") != null) {
            relation.setAuthStartDate(params.get("authStartDate").toString());
        } else if (params.get("investDate") != null) {
            relation.setAuthStartDate(params.get("investDate").toString());
        }
        if (params.get("authEndDate") != null) {
            relation.setAuthEndDate(params.get("authEndDate").toString());
        } else if (params.get("withdrawDate") != null) {
            relation.setAuthEndDate(params.get("withdrawDate").toString());
        }
        if (params.get("storeId") != null) {
            relation.setStoreId(Long.valueOf(params.get("storeId").toString()));
        }

        investorRelationService.update(relation);
        return Result.success();
    }

    /**
     * 删除投资关系
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id) {
        InvestorStore relation = investorStoreMapper.selectById(id);
        if (relation == null) {
            return Result.success();
        }

        investorStoreMapper.deleteById(id);

        // 同步删除sys_user_store中的关联
        Investor investor = investorMapper.selectById(relation.getInvestorId());
        if (investor != null && investor.getUserId() != null) {
            sysUserStoreMapper.delete(new LambdaQueryWrapper<SysUserStore>()
                    .eq(SysUserStore::getUserId, investor.getUserId())
                    .eq(SysUserStore::getStoreId, relation.getStoreId()));
        }

        return Result.success();
    }
}
