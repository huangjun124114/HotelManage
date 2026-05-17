package com.linxi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.entity.ExtensionFieldDefinition;
import com.linxi.mapper.ExtensionFieldDefinitionMapper;
import com.linxi.service.ExtensionFieldDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExtensionFieldDefinitionServiceImpl implements ExtensionFieldDefinitionService {

    @Autowired
    private ExtensionFieldDefinitionMapper extensionFieldDefinitionMapper;

    @Override
    public Page<ExtensionFieldDefinition> page(java.util.Map<String, Object> params) {
        int pageNum = params.get("pageNum") != null ? Integer.parseInt(params.get("pageNum").toString()) : 1;
        int pageSize = params.get("pageSize") != null ? Integer.parseInt(params.get("pageSize").toString()) : 10;
        String tableType = params.get("tableType") != null ? params.get("tableType").toString() : null;

        Page<ExtensionFieldDefinition> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExtensionFieldDefinition> wrapper = new LambdaQueryWrapper<>();

        if (tableType != null && !tableType.isEmpty()) {
            wrapper.eq(ExtensionFieldDefinition::getTableType, tableType);
        }

        wrapper.orderByAsc(ExtensionFieldDefinition::getSortNo);
        return extensionFieldDefinitionMapper.selectPage(page, wrapper);
    }

    @Override
    public List<ExtensionFieldDefinition> getByTableType(String tableType) {
        return extensionFieldDefinitionMapper.selectList(
                new LambdaQueryWrapper<ExtensionFieldDefinition>()
                        .eq(ExtensionFieldDefinition::getTableType, tableType)
                        .orderByAsc(ExtensionFieldDefinition::getSortNo)
        );
    }

    @Override
    public List<ExtensionFieldDefinition> getEnabledFields(String tableType) {
        return extensionFieldDefinitionMapper.selectList(
                new LambdaQueryWrapper<ExtensionFieldDefinition>()
                        .eq(ExtensionFieldDefinition::getTableType, tableType)
                        .eq(ExtensionFieldDefinition::getStatus, 1)
                        .eq(ExtensionFieldDefinition::getVisible, 1)
                        .orderByAsc(ExtensionFieldDefinition::getSortNo)
        );
    }

    @Override
    public ExtensionFieldDefinition getById(Long id) {
        return extensionFieldDefinitionMapper.selectById(id);
    }

    @Override
    public void add(ExtensionFieldDefinition definition) {
        definition.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        extensionFieldDefinitionMapper.insert(definition);
    }

    @Override
    public void update(ExtensionFieldDefinition definition) {
        definition.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        extensionFieldDefinitionMapper.updateById(definition);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        extensionFieldDefinitionMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        for (Long id : ids) {
            ExtensionFieldDefinition definition = extensionFieldDefinitionMapper.selectById(id);
            if (definition != null) {
                definition.setStatus(status);
                definition.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                extensionFieldDefinitionMapper.updateById(definition);
            }
        }
    }
}
