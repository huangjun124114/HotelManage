package com.linxi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.entity.DailyReportTemplate;
import com.linxi.mapper.DailyReportTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/templates")
public class DailyReportTemplateController {

    @Autowired
    private DailyReportTemplateMapper dailyReportTemplateMapper;

    @GetMapping
    public Result<PageResult<DailyReportTemplate>> list(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<DailyReportTemplate> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<DailyReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyReportTemplate::getStatus, 1);
        wrapper.orderByDesc(DailyReportTemplate::getIsDefault);
        Page<DailyReportTemplate> result = dailyReportTemplateMapper.selectPage(page, wrapper);
        PageResult<DailyReportTemplate> pageResult = PageResult.of(
                result.getTotal(), result.getPages(), result.getCurrent(), result.getSize(), result.getRecords()
        );
        return Result.success(pageResult);
    }
}
