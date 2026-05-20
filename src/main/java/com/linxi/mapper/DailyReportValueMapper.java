package com.linxi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linxi.entity.DailyReportValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DailyReportValueMapper extends BaseMapper<DailyReportValue> {
    /**
     * 批量插入或更新日报值（SQLite 用 INSERT OR REPLACE）
     */
    int batchSaveOrUpdate(List<DailyReportValue> list);

    /**
     * 评分排名查询：各渠道评分算术平均（排除0和null）
     */
    List<Map<String, Object>> selectScoreRanking(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
