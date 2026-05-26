package com.linxi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linxi.entity.DailyChannel;
import com.linxi.mapper.DailyChannelMapper;
import com.linxi.service.DailyChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class DailyChannelServiceImpl implements DailyChannelService {

    @Autowired
    private DailyChannelMapper dailyChannelMapper;

    @Override
    public void saveOrUpdate(Long reportId, Long storeId, String reportDate, Map<String, Object> values) {
        if (reportId == null) {
            return;
        }
        DailyChannel channel = getByReportId(reportId);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (channel == null) {
            channel = new DailyChannel();
            channel.setDailyReportId(reportId);
            channel.setStoreId(storeId);
            channel.setReportDate(java.time.LocalDate.parse(reportDate));  // 设置reportDate
            channel.setCreateTime(now);
        }

        // 安全处理values为null的情况
        if (values != null) {
            // 设置评分字段
            channel.setCtripScore(toDecimal(values.get("ctrip_score")));
            channel.setLyScore(toDecimal(values.get("ly_score")));
            channel.setQunarScore(toDecimal(values.get("qunar_score")));
            channel.setZhixingScore(toDecimal(values.get("zhixing_score")));
            channel.setMeituanScore(toDecimal(values.get("meituan_score")));
            channel.setFliggyScore(toDecimal(values.get("fliggy_score")));

            // 设置好评数字段
            channel.setCtripGoodReviewCount(toInteger(values.get("ctrip_good_review_count")));
            channel.setMeituanGoodReviewCount(toInteger(values.get("meituan_good_review_count")));
            channel.setLyGoodReviewCount(toInteger(values.get("ly_good_review_count")));
            channel.setQunarGoodReviewCount(toInteger(values.get("qunar_good_review_count")));
            channel.setZhixingGoodReviewCount(toInteger(values.get("zhixing_good_review_count")));
            channel.setFliggyGoodReviewCount(toInteger(values.get("fliggy_good_review_count")));
        }

        channel.setUpdateTime(now);

        if (channel.getId() == null) {
            dailyChannelMapper.insert(channel);
        } else {
            dailyChannelMapper.updateById(channel);
        }
    }

    @Override
    public DailyChannel getByReportId(Long reportId) {
        return dailyChannelMapper.selectOne(
                new LambdaQueryWrapper<DailyChannel>()
                        .eq(DailyChannel::getDailyReportId, reportId)
        );
    }


    private BigDecimal toDecimal(Object val) {
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object val) {
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.toString().split("\\.")[0]);
        } catch (Exception e) {
            return 0;
        }
    }
}
