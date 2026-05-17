package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.dto.InvestorQueryDTO;
import com.linxi.entity.Investor;
import com.linxi.entity.InvestorStore;
import com.linxi.mapper.InvestorMapper;
import com.linxi.mapper.InvestorStoreMapper;
import com.linxi.service.InvestorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
        investor.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        investor.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (investor.getStatus() == null) {
            investor.setStatus(1);
        }
        if (investor.getUserId() == null) {
            investor.setUserId(1L); // 默认设置为系统管理员
        }
        return investorMapper.insert(investor) > 0;
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
        // 删除关联的门店关系
        investorStoreMapper.delete(new LambdaQueryWrapper<InvestorStore>()
                .eq(InvestorStore::getInvestorId, id));
        return investorMapper.deleteById(id) > 0;
    }
}
