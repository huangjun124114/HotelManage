package com.linxi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.PageResult;
import com.linxi.common.Result;
import com.linxi.entity.ExtensionFieldDefinition;
import com.linxi.service.ExtensionFieldDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/extension-field-definitions")
public class ExtensionFieldDefinitionController {

    @Autowired
    private ExtensionFieldDefinitionService extensionFieldDefinitionService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:config', 'ROLE_SUPER_ADMIN')")
    public Result<PageResult<ExtensionFieldDefinition>> list(@RequestParam Map<String, Object> params) {
        Page<ExtensionFieldDefinition> page = extensionFieldDefinitionService.page(params);
        PageResult<ExtensionFieldDefinition> pageResult = PageResult.of(
                page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords()
        );
        return Result.success(pageResult);
    }

    @GetMapping("/by-table-type/{tableType}")
    public Result<List<ExtensionFieldDefinition>> getByTableType(@PathVariable String tableType) {
        return Result.success(extensionFieldDefinitionService.getByTableType(tableType));
    }

    @GetMapping("/enabled/{tableType}")
    public Result<List<ExtensionFieldDefinition>> getEnabledFields(@PathVariable String tableType) {
        return Result.success(extensionFieldDefinitionService.getEnabledFields(tableType));
    }

    @GetMapping("/{id}")
    public Result<ExtensionFieldDefinition> getById(@PathVariable Long id) {
        return Result.success(extensionFieldDefinitionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:config', 'ROLE_SUPER_ADMIN')")
    public Result<Void> add(@RequestBody ExtensionFieldDefinition definition) {
        extensionFieldDefinitionService.add(definition);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:config', 'ROLE_SUPER_ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody ExtensionFieldDefinition definition) {
        definition.setId(id);
        extensionFieldDefinitionService.update(definition);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:config', 'ROLE_SUPER_ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        extensionFieldDefinitionService.delete(id);
        return Result.success();
    }

    @PutMapping("/batch-status")
    @PreAuthorize("hasAnyAuthority('system:config', 'ROLE_SUPER_ADMIN')")
    public Result<Void> batchUpdateStatus(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        Integer status = Integer.parseInt(params.get("status").toString());
        extensionFieldDefinitionService.batchUpdateStatus(ids, status);
        return Result.success();
    }
}
