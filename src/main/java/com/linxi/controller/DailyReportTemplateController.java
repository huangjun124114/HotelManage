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

    @GetMapping("/{id}")
    public Result<DailyReportTemplate> getById(@PathVariable Long id) {
        return Result.success(dailyReportTemplateMapper.selectById(id));
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

    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:template', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "日报模板", type = "CREATE", description = "新增日报模板")
    public Result<Void> save(@RequestBody DailyReportTemplate template) {
        template.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        template.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (template.getStatus() == null) template.setStatus(1);
        dailyReportTemplateMapper.insert(template);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:template', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "日报模板", type = "UPDATE", description = "编辑日报模板")
    public Result<Void> update(@PathVariable Long id, @RequestBody DailyReportTemplate template) {
        template.setId(id);
        template.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        dailyReportTemplateMapper.updateById(template);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:template', 'ROLE_SUPER_ADMIN')")
    @OperationLog(module = "日报模板", type = "DELETE", description = "删除日报模板")
    public Result<Void> delete(@PathVariable Long id) {
        // 删除模板下的字段
        dailyReportFieldMapper.delete(
                new LambdaQueryWrapper<DailyReportField>().eq(DailyReportField::getTemplateId, id)
        );
        dailyReportTemplateMapper.deleteById(id);
        return Result.success();
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
