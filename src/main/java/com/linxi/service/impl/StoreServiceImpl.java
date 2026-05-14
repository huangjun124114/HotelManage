package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.dto.StoreQueryDTO;
import com.linxi.entity.Store;
import com.linxi.mapper.StoreMapper;
import com.linxi.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class StoreServiceImpl implements StoreService {

    @Autowired
    private StoreMapper storeMapper;

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
        return storeMapper.deleteById(id) > 0;
    }
}
