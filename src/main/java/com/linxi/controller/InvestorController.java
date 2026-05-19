package com.linxi.controller;

import com.linxi.common.BusinessException;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.dto.InvestorQueryDTO;
import com.linxi.entity.Investor;
import com.linxi.entity.InvestorStore;
import com.linxi.mapper.InvestorStoreMapper;
import com.linxi.service.InvestorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/investors")
public class InvestorController {

    @Autowired
    private InvestorService investorService;

    @Autowired
    private InvestorStoreMapper investorStoreMapper;

    /**
     * 分页查询投资人
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<Investor>> list(InvestorQueryDTO query) {
        Page<Investor> page = investorService.page(query);
        return Result.success(PageResult.of(
                page.getTotal(),
                page.getPages(),
                page.getCurrent(),
                page.getSize(),
                page.getRecords()
        ));
    }

    /**
     * 查询所有投资人（不分页）
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public Result<List<Investor>> listAll() {
        return Result.success(investorService.listAll());
    }

    /**
     * 根据ID查询投资人
     */
    @GetMapping("/{id}")
    public Result<Investor> getById(@PathVariable Long id) {
        return Result.success(investorService.getById(id));
    }

    /**
     * 新增投资人（自动创建系统账户）
     */
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Investor investor) {
        investorService.save(investor);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", investor.getId());
        result.put("userId", investor.getUserId());
        result.put("username", investor.getPhone());
        result.put("password", investor.getGeneratedPassword());
        return Result.success(result);
    }

    /**
     * 修改投资人
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Investor investor) {
        investor.setId(id);
        investorService.update(investor);
        return Result.success();
    }

    /**
     * 删除投资人
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        investorService.delete(id);
        return Result.success();
    }

    /**
     * 查询投资人的门店关联列表
     */
    @GetMapping("/{id}/stores")
    public Result<List<InvestorStore>> getInvestorStores(@PathVariable Long id) {
        List<InvestorStore> stores = investorStoreMapper.selectList(
                new LambdaQueryWrapper<InvestorStore>()
                        .eq(InvestorStore::getInvestorId, id)
        );
        return Result.success(stores);
    }

    /**
     * 保存投资人的门店关联
     */
    @PutMapping("/{id}/stores")
    public Result<Void> saveInvestorStores(@PathVariable Long id, @RequestBody List<InvestorStore> stores) {
        // 删除旧的关联
        investorStoreMapper.delete(new LambdaQueryWrapper<InvestorStore>()
                .eq(InvestorStore::getInvestorId, id));

        // 保存新的关联
        String now = cn.hutool.core.date.DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        for (InvestorStore store : stores) {
            store.setInvestorId(id);
            store.setCreateTime(now);
            store.setUpdateTime(now);
            if (store.getStatus() == null) {
                store.setStatus(1);
            }
            investorStoreMapper.insert(store);
        }
        return Result.success();
    }

    /**
     * 删除单个门店关联
     */
    @DeleteMapping("/{investorId}/stores/{storeId}")
    public Result<Void> deleteInvestorStore(@PathVariable Long investorId, @PathVariable Long storeId) {
        investorStoreMapper.delete(new LambdaQueryWrapper<InvestorStore>()
                .eq(InvestorStore::getInvestorId, investorId)
                .eq(InvestorStore::getStoreId, storeId));
        return Result.success();
    }
}
