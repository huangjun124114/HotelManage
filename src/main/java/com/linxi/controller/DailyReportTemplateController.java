package com.linxi.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.annotation.OperationLog;
import com.linxi.entity.DailyReportField;
import com.linxi.entity.DailyReportTemplate;
import com.linxi.mapper.DailyReportFieldMapper;
import com.linxi.mapper.DailyReportTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/templates")
public class DailyReportTemplateController {

    @Autowired
    private DailyReportTemplateMapper dailyReportTemplateMapper;

    @Autowired
    private DailyReportFieldMapper dailyReportFieldMapper;

    @GetMapping
    public Result<PageResult<DailyReportTemplate>> list(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<DailyReportTemplate> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<DailyReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DailyReportTemplate::getIsDefault);
        Page<DailyReportTemplate> result = dailyReportTemplateMapper.selectPage(page, wrapper);
        PageResult<DailyReportTemplate> pageResult = PageResult.of(
                result.getTotal(), result.getPages(), result.getCurrent(), result.getSize(), result.getRecords()
        );
        return Result.success(pageResult);
    }
    @GetMapping("/{id}/fields")
    public Result<List<DailyReportField>> getFields(@PathVariable Long id) {
        List<DailyReportField> fields = dailyReportFieldMapper.selectList(
                new LambdaQueryWrapper<DailyReportField>()
                        .eq(DailyReportField::getTemplateId, id)
                        .eq(DailyReportField::getStatus, 1)
                        .orderByAsc(DailyReportField::getSortNo)
        );
        return Result.success(fields);
    }
    @PutMapping("/fields/{fieldId}")
    @PreAuthorize("hasAnyAuthority('system:template', 'ROLE_SUPER_ADMIN')")
    public Result<Void> updateField(@PathVariable Long fieldId, @RequestBody DailyReportField field) {
        field.setId(fieldId);
        field.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        dailyReportFieldMapper.updateById(field);
        return Result.success();
    }
}
