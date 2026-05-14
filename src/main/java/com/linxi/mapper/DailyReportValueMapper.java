package com.linxi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linxi.entity.DailyReportValue;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DailyReportValueMapper extends BaseMapper<DailyReportValue> {
    /**
     * 批量插入或更新日报值（SQLite 用 INSERT OR REPLACE）
     */
    int batchSaveOrUpdate(List<DailyReportValue> list);
}
