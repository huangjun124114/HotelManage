package com.linxi.service;

import com.linxi.entity.InvestorStore;

/**
 * 投资关系服务接口
 */
public interface InvestorRelationService {

    /**
     * 更新投资关系记录
     * 支持更新投资金额、持股比例、授权开始日期、授权结束日期、门店；
     * 如果 storeId 变更，同步更新 sys_user_store
     *
     * @param relation 携带更新字段的 InvestorStore 实体（id 必填）
     */
    void update(InvestorStore relation);
}
