package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linxi.common.PageResult;
import com.linxi.entity.SysOperationLog;
import com.linxi.mapper.SysOperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class SysOperationLogService extends ServiceImpl<SysOperationLogMapper, SysOperationLog> {

    public Page<SysOperationLog> page(int pageNo, int pageSize, String operator, String module, String startDate, String endDate) {
        Page<SysOperationLog> page = new Page<>(pageNo, pageSize);
        lambdaQuery()
            .like(StringUtils.hasText(operator), SysOperationLog::getUsername, operator)
            .like(StringUtils.hasText(module), SysOperationLog::getModuleName, module)
            .ge(StringUtils.hasText(startDate), SysOperationLog::getCreateTime, startDate + " 00:00:00")
            .le(StringUtils.hasText(endDate), SysOperationLog::getCreateTime, endDate + " 23:59:59")
            .orderByDesc(SysOperationLog::getCreateTime)
            .page(page);
        return page;
    }

    public PageResult<SysOperationLog> pageResult(int pageNo, int pageSize, String operator, String module, String startDate, String endDate) {
        Page<SysOperationLog> page = page(pageNo, pageSize, operator, module, startDate, endDate);
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
