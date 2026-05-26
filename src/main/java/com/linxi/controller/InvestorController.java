package com.linxi.controller;

import com.linxi.common.BusinessException;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.dto.InvestorQueryDTO;
import com.linxi.entity.Investor;
import com.linxi.service.InvestorService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/investors")
public class InvestorController {

    @Autowired
    private InvestorService investorService;

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
     * 新增投资人（自动创建系统账户）
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:user', 'ROLE_SUPER_ADMIN', 'ROLE_CEO')")
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
    @PreAuthorize("hasAnyAuthority('system:user', 'ROLE_SUPER_ADMIN', 'ROLE_CEO')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Investor investor) {
        investor.setId(id);
        investorService.update(investor);
        return Result.success();
    }
}
