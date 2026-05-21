package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.dto.StoreQueryDTO;
import com.linxi.entity.DailyReport;
import com.linxi.entity.InvestorStore;
import com.linxi.entity.Store;
import com.linxi.entity.SysUserStore;
import com.linxi.mapper.DailyReportMapper;
import com.linxi.mapper.InvestorStoreMapper;
import com.linxi.mapper.StoreMapper;
import com.linxi.mapper.SysUserStoreMapper;
import com.linxi.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StoreServiceImpl implements StoreService {

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private InvestorStoreMapper investorStoreMapper;

    @Autowired
    private DailyReportMapper dailyReportMapper;

    @Autowired
    private SysUserStoreMapper sysUserStoreMapper;

    @Override
    public Store getById(Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new BusinessException("门店不存在");
        }
        return store;
    }

    @Override
    public Page<Store> page(StoreQueryDTO query) {
        Page<Store> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getStoreName()), Store::getStoreName, query.getStoreName())
                .like(StringUtils.hasText(query.getStoreCode()), Store::getStoreCode, query.getStoreCode())
                .like(StringUtils.hasText(query.getCity()), Store::getCity, query.getCity())
                .eq(query.getStatus() != null, Store::getStatus, query.getStatus())
                .in(query.getStoreIds() != null && !query.getStoreIds().isEmpty(), Store::getId, query.getStoreIds())
                .orderByDesc(Store::getCreateTime);
        return storeMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Store> listAll() {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Store::getStatus, 1)
                .orderByAsc(Store::getStoreCode);
        return storeMapper.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> getOptions() {
        List<Store> stores = listAll();
        return stores.stream().map(store -> {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("label", store.getStoreName());
            option.put("value", store.getId());
            return option;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean save(Store store) {
        store.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        store.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return storeMapper.insert(store) > 0;
    }

    @Override
    public boolean update(Store store) {
        store.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return storeMapper.updateById(store) > 0;
    }

    @Override
    public boolean delete(Long id) {
        // 校验 investor_store 是否有关联记录
        Long investorCount = investorStoreMapper.selectCount(
                new LambdaQueryWrapper<InvestorStore>().eq(InvestorStore::getStoreId, id)
        );
        if (investorCount > 0) {
            throw new BusinessException("此门店存在投资记录/日报记录/团队记录，不能删除，但可以禁用");
        }

        // 校验 daily_report 是否有关联记录
        Long dailyReportCount = dailyReportMapper.selectCount(
                new LambdaQueryWrapper<DailyReport>().eq(DailyReport::getStoreId, id)
        );
        if (dailyReportCount > 0) {
            throw new BusinessException("此门店存在投资记录/日报记录/团队记录，不能删除，但可以禁用");
        }

        // 校验 sys_user_store 是否有该门店的用户关联（即团队成员记录）
        Long userStoreCount = sysUserStoreMapper.selectCount(
                new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getStoreId, id)
        );
        if (userStoreCount > 0) {
            throw new BusinessException("此门店存在投资记录/日报记录/团队记录，不能删除，但可以禁用");
        }

        return storeMapper.deleteById(id) > 0;
    }
}
