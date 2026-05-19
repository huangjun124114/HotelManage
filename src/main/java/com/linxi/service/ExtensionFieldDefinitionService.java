package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.entity.ExtensionFieldDefinition;

import java.util.List;
import java.util.Map;

public interface ExtensionFieldDefinitionService {

    /**
     * 分页查询弹性域定义
     */
    Page<ExtensionFieldDefinition> page(Map<String, Object> params);

    /**
     * 根据tableType查询
     */
    List<ExtensionFieldDefinition> getByTableType(String tableType);

    /**
     * 获取启用的字段定义
     */
    List<ExtensionFieldDefinition> getEnabledFields(String tableType);

    /**
     * 根据ID查询
     */
    ExtensionFieldDefinition getById(Long id);

    /**
     * 新增
     */
    void add(ExtensionFieldDefinition definition);

    /**
     * 更新
     */
    void update(ExtensionFieldDefinition definition);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 批量更新状态
     */
    void batchUpdateStatus(List<Long> ids, Integer status);
}
