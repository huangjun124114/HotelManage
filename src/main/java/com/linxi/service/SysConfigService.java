package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linxi.common.PageResult;
import com.linxi.entity.SysConfig;
import com.linxi.mapper.SysConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class SysConfigService extends ServiceImpl<SysConfigMapper, SysConfig> {

    public Page<SysConfig> page(int pageNo, int pageSize, String configKey) {
        Page<SysConfig> page = new Page<>(pageNo, pageSize);
        lambdaQuery()
            .like(StringUtils.hasText(configKey), SysConfig::getConfigKey, configKey)
            .orderByDesc(SysConfig::getCreateTime)
            .page(page);
        return page;
    }

    public PageResult<SysConfig> pageResult(int pageNo, int pageSize, String configKey) {
        Page<SysConfig> page = page(pageNo, pageSize, configKey);
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
