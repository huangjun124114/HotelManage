package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.dto.DailyReportQueryDTO;
import com.linxi.dto.DailyReportSaveDTO;
import com.linxi.entity.*;
import com.linxi.mapper.*;
import com.linxi.service.DailyReportService;
import com.linxi.utils.FormulaCalculator;
import com.linxi.utils.WeekDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DailyReportServiceImpl implements DailyReportService {

    @Autowired
    private DailyReportMapper dailyReportMapper;

    @Autowired
    private DailyReportValueMapper dailyReportValueMapper;

    @Autowired
    private DailyReportSummaryMapper dailyReportSummaryMapper;

    @Autowired
    private DailyReportChannelMapper dailyReportChannelMapper;

    @Autowired
    private DailyReportPlatformMapper dailyReportPlatformMapper;

    @Autowired
    private DailyReportFieldMapper dailyReportFieldMapper;

    @Autowired
    private DailyReportTemplateMapper dailyReportTemplateMapper;

    @Autowired
    private StoreMapper storeMapper;

    /** 渠道映射：fieldCode -> channelCode, channelName */
    private static final Map<String, String[]> CHANNEL_MAP = new LinkedHashMap<>();

    /** 平台映射：评分fieldCode -> platformCode, scoreFieldCode, scanFieldCode, goodReviewFieldCode */
    private static final Map<String, PlatformConfig> PLATFORM_MAP = new LinkedHashMap<>();

    /** 摘要字段映射 */
    private static final List<String> SUMMARY_FIELDS = Arrays.asList(
            "own_room_count", "repair_room_count", "hourly_room_count", "room_nights",
            "occupancy_rate", "adr", "revpar", "daily_room_fee", "hourly_room_fee",
            "other_fee", "total_revenue", "deposit_amount"
    );

    static {
        CHANNEL_MAP.put("walkin_room_nights", new String[]{"walkin", "散客"});
        CHANNEL_MAP.put("ctrip_room_nights", new String[]{"ctrip", "携程"});
        CHANNEL_MAP.put("ly_room_nights", new String[]{"ly", "同程艺龙"});
        CHANNEL_MAP.put("qunar_room_nights", new String[]{"qunar", "去哪儿"});
        CHANNEL_MAP.put("zhixing_room_nights", new String[]{"zhixing", "智行"});
        CHANNEL_MAP.put("external_room_nights", new String[]{"external", "外网"});
        CHANNEL_MAP.put("meituan_hotel_room_nights", new String[]{"meituan_hotel", "美团酒店"});
        CHANNEL_MAP.put("fliggy_room_nights", new String[]{"fliggy", "飞猪"});
        CHANNEL_MAP.put("douyin_room_nights", new String[]{"douyin", "抖音"});
        CHANNEL_MAP.put("xiaozhu_room_nights", new String[]{"xiaozhu", "小猪"});
        CHANNEL_MAP.put("tujia_room_nights", new String[]{"tujia", "途家"});
        CHANNEL_MAP.put("meituan_homestay_room_nights", new String[]{"meituan_homestay", "美团民宿"});
        CHANNEL_MAP.put("jiali_room_nights", new String[]{"jiali", "红色加力/加力"});

        PLATFORM_MAP.put("ctrip_score", new PlatformConfig("ctrip", "携程", "ctrip_score", "ctrip_scan_count", "ctrip_good_review_count"));
        PLATFORM_MAP.put("ly_score", new PlatformConfig("ly", "同程艺龙", "ly_score", null, "ly_good_review_count"));
        PLATFORM_MAP.put("qunar_score", new PlatformConfig("qunar", "去哪", "qunar_score", null, "qunar_good_review_count"));
        PLATFORM_MAP.put("zhixing_score", new PlatformConfig("zhixing", "智行", "zhixing_score", null, "zhixing_good_review_count"));
        PLATFORM_MAP.put("meituan_score", new PlatformConfig("meituan", "美团", "meituan_score", "meituan_scan_count", "meituan_good_review_count"));
        PLATFORM_MAP.put("fliggy_score", new PlatformConfig("fliggy", "飞猪", "fliggy_score", null, "fliggy_good_review_count"));
    }

    private static class PlatformConfig {
        String platformCode;
        String platformName;
        String scoreFieldCode;
        String scanFieldCode;
        String goodReviewFieldCode;

        PlatformConfig(String code, String name, String scoreCode, String scanCode, String reviewCode) {
            this.platformCode = code;
            this.platformName = name;
            this.scoreFieldCode = scoreCode;
            this.scanFieldCode = scanCode;
            this.goodReviewFieldCode = reviewCode;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getTodayReport(Long storeId) {
        String today = DateUtil.today();
        return getOrCreateDraft(storeId, today);
    }

    @Override
    public Map<String, Object> getDetail(Long storeId, String reportDate) {
        return getOrCreateDraft(storeId, reportDate);
    }

    private Map<String, Object> getOrCreateDraft(Long storeId, String reportDate) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException("门店不存在");
        }

        // 查找该门店适用的模板
        DailyReportTemplate template = getTemplateByStore(storeId);
        if (template == null) {
            throw new BusinessException("该门店未配置日报模板");
        }

        // 查询是否已有日报
        DailyReport report = dailyReportMapper.selectOne(
                new LambdaQueryWrapper<DailyReport>()
                        .eq(DailyReport::getStoreId, storeId)
                        .eq(DailyReport::getReportDate, reportDate)
        );

        if (report == null) {
            // 创建草稿
            report = new DailyReport();
            report.setStoreId(storeId);
            report.setReportDate(reportDate);
            report.setReportMonth(reportDate.substring(0, 7));
            report.setWeekDay(WeekDayUtil.getWeekDay(reportDate));
            report.setTemplateId(template.getId());
            report.setStatus(0); // 草稿
            report.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            report.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            dailyReportMapper.insert(report);
        }

        // 查询字段配置
        List<DailyReportField> fields = dailyReportFieldMapper.selectList(
                new LambdaQueryWrapper<DailyReportField>()
                        .eq(DailyReportField::getTemplateId, template.getId())
                        .eq(DailyReportField::getStatus, 1)
                        .orderByAsc(DailyReportField::getSortNo)
        );

        // 查询已有字段值
        List<DailyReportValue> values = dailyReportValueMapper.selectList(
                new LambdaQueryWrapper<DailyReportValue>()
                        .eq(DailyReportValue::getReportId, report.getId())
        );
        Map<String, Object> valueMap = new HashMap<>();
        for (DailyReportValue v : values) {
            if ("number".equals(v.getFieldType())) {
                valueMap.put(v.getFieldCode(), v.getValueNumber());
            } else {
                valueMap.put(v.getFieldCode(), v.getValueText());
            }
        }

        // 填充默认值
        for (DailyReportField field : fields) {
            if (!valueMap.containsKey(field.getFieldCode()) && field.getDefaultValue() != null) {
                valueMap.put(field.getFieldCode(), field.getDefaultValue());
            }
            // 预填门店自有房量
            if ("own_room_count".equals(field.getFieldCode()) && !valueMap.containsKey("own_room_count")) {
                valueMap.put("own_room_count", store.getOwnRoomCount() != null ? store.getOwnRoomCount() : 0);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("report", report);
        result.put("fields", fields);
        result.put("values", valueMap);
        return result;
    }

    private DailyReportTemplate getTemplateByStore(Long storeId) {
        List<DailyReportTemplate> templates = dailyReportTemplateMapper.selectList(
                new LambdaQueryWrapper<DailyReportTemplate>()
                        .eq(DailyReportTemplate::getStatus, 1)
                        .orderByDesc(DailyReportTemplate::getIsDefault)
        );
        if (!templates.isEmpty()) {
            return templates.get(0);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDraft(DailyReportSaveDTO dto) {
        DailyReport report = getReport(dto);
        report.setStatus(0);
        report.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        dailyReportMapper.updateById(report);

        // 保存字段值
        if (dto.getValues() != null) {
            saveFieldValues(report.getId(), dto.getTemplateId(), dto.getValues());
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(DailyReportSaveDTO dto) {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");

        DailyReport report = getReport(dto);

        // 校验必填字段
        List<DailyReportField> fields = getTemplateFields(report.getTemplateId());
        Map<String, Object> values = dto.getValues() != null ? dto.getValues() : new HashMap<>();
        for (DailyReportField field : fields) {
            if (field.getRequired() != null && field.getRequired() == 1) {
                Object val = values.get(field.getFieldCode());
                if (val == null || "".equals(val.toString())) {
                    throw new BusinessException("字段【" + field.getFieldName() + "】不能为空");
                }
            }
        }

        // 自动计算公式字段
        Map<String, BigDecimal> numValues = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            try {
                numValues.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
            } catch (Exception ignored) {
            }
        }

        for (DailyReportField field : fields) {
            if (field.getFormula() != null && !field.getFormula().isEmpty()) {
                BigDecimal result = FormulaCalculator.calculate(field.getFormula(), numValues);
                if (field.getDecimalScale() != null) {
                    result = result.setScale(field.getDecimalScale(), RoundingMode.HALF_UP);
                }
                values.put(field.getFieldCode(), result);
                numValues.put(field.getFieldCode(), result);
            }
        }

        // 更新主表
        report.setStatus(1); // 已提交
        report.setSubmitTime(now);
        report.setUpdateTime(now);
        dailyReportMapper.updateById(report);

        // 保存字段值（EAV弹性域）
        saveFieldValues(report.getId(), dto.getTemplateId(), values);

        // 写入摘要（核心指标冗余）
        saveSummary(report, values);

        // 写入渠道数据
        saveChannels(report, values);

        // 写入平台评价数据
        savePlatforms(report, values);

        return true;
    }

    private DailyReport getReport(DailyReportSaveDTO dto) {
        DailyReport report;
        if (dto.getReportId() != null) {
            report = dailyReportMapper.selectById(dto.getReportId());
            if (report == null) {
                throw new BusinessException("日报不存在");
            }
        } else {
            report = dailyReportMapper.selectOne(
                    new LambdaQueryWrapper<DailyReport>()
                            .eq(DailyReport::getStoreId, dto.getStoreId())
                            .eq(DailyReport::getReportDate, dto.getReportDate())
            );
            if (report == null) {
                throw new BusinessException("日报不存在，请先打开创建草稿");
            }
        }
        return report;
    }

    private void saveFieldValues(Long reportId, Long templateId, Map<String, Object> values) {
        List<DailyReportField> fields = getTemplateFields(templateId);
        List<DailyReportValue> list = new ArrayList<>();
        String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");

        for (DailyReportField field : fields) {
            String fieldCode = field.getFieldCode();
            if (!values.containsKey(fieldCode)) {
                continue;
            }

            DailyReportValue val = new DailyReportValue();
            val.setReportId(reportId);
            val.setFieldId(field.getId());
            val.setFieldCode(fieldCode);
            val.setFieldName(field.getFieldName());
            val.setFieldType(field.getFieldType());
            val.setSortNo(field.getSortNo());
            val.setCreateTime(now);
            val.setUpdateTime(now);

            Object v = values.get(fieldCode);
            if (v != null) {
                if ("number".equals(field.getFieldType())) {
                    val.setValueNumber(new BigDecimal(v.toString()));
                } else {
                    val.setValueText(v.toString());
                }
            }

            list.add(val);
        }

        if (!list.isEmpty()) {
            dailyReportValueMapper.batchSaveOrUpdate(list);
        }
    }

    private void saveSummary(DailyReport report, Map<String, Object> values) {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");

        DailyReportSummary summary = dailyReportSummaryMapper.selectOne(
                new LambdaQueryWrapper<DailyReportSummary>().eq(DailyReportSummary::getReportId, report.getId())
        );

        if (summary == null) {
            summary = new DailyReportSummary();
            summary.setReportId(report.getId());
            summary.setStoreId(report.getStoreId());
            summary.setReportDate(report.getReportDate());
            summary.setReportMonth(report.getReportMonth());
            summary.setCreateTime(now);
        }
        summary.setUpdateTime(now);

        for (String fieldCode : SUMMARY_FIELDS) {
            Object val = values.get(fieldCode);
            BigDecimal numVal = toBigDecimal(val);
            if (numVal == null) numVal = BigDecimal.ZERO;

            switch (fieldCode) {
                case "own_room_count": summary.setOwnRoomCount(numVal); break;
                case "repair_room_count": summary.setRepairRoomCount(numVal); break;
                case "hourly_room_count": summary.setHourlyRoomCount(numVal); break;
                case "room_nights": summary.setRoomNights(numVal); break;
                case "occupancy_rate": summary.setOccupancyRate(numVal); break;
                case "adr": summary.setAdr(numVal); break;
                case "revpar": summary.setRevpar(numVal); break;
                case "daily_room_fee": summary.setDailyRoomFee(numVal); break;
                case "hourly_room_fee": summary.setHourlyRoomFee(numVal); break;
                case "other_fee": summary.setOtherFee(numVal); break;
                case "total_revenue": summary.setTotalRevenue(numVal); break;
                case "deposit_amount": summary.setDepositAmount(numVal); break;
            }
        }

        if (summary.getId() == null) {
            dailyReportSummaryMapper.insert(summary);
        } else {
            dailyReportSummaryMapper.updateById(summary);
        }
    }

    private void saveChannels(DailyReport report, Map<String, Object> values) {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");

        for (Map.Entry<String, String[]> entry : CHANNEL_MAP.entrySet()) {
            String fieldCode = entry.getKey();
            String[] channelInfo = entry.getValue();

            Object val = values.get(fieldCode);
            BigDecimal nights = toBigDecimal(val);
            if (nights == null) nights = BigDecimal.ZERO;

            DailyReportChannel channel = dailyReportChannelMapper.selectOne(
                    new LambdaQueryWrapper<DailyReportChannel>()
                            .eq(DailyReportChannel::getReportId, report.getId())
                            .eq(DailyReportChannel::getChannelCode, channelInfo[0])
            );

            if (channel == null) {
                channel = new DailyReportChannel();
                channel.setReportId(report.getId());
                channel.setStoreId(report.getStoreId());
                channel.setReportDate(report.getReportDate());
                channel.setChannelCode(channelInfo[0]);
                channel.setChannelName(channelInfo[1]);
                channel.setCreateTime(now);
            }
            channel.setRoomNights(nights);
            channel.setUpdateTime(now);

            if (channel.getId() == null) {
                dailyReportChannelMapper.insert(channel);
            } else {
                dailyReportChannelMapper.updateById(channel);
            }
        }
    }

    private void savePlatforms(DailyReport report, Map<String, Object> values) {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");

        for (Map.Entry<String, PlatformConfig> entry : PLATFORM_MAP.entrySet()) {
            PlatformConfig cfg = entry.getValue();

            BigDecimal score = toBigDecimal(values.get(cfg.scoreFieldCode));
            Integer scanCount = toInteger(values.get(cfg.scanFieldCode));
            Integer goodReviewCount = toInteger(values.get(cfg.goodReviewFieldCode));

            DailyReportPlatform platform = dailyReportPlatformMapper.selectOne(
                    new LambdaQueryWrapper<DailyReportPlatform>()
                            .eq(DailyReportPlatform::getReportId, report.getId())
                            .eq(DailyReportPlatform::getPlatformCode, cfg.platformCode)
            );

            if (platform == null) {
                platform = new DailyReportPlatform();
                platform.setReportId(report.getId());
                platform.setStoreId(report.getStoreId());
                platform.setReportDate(report.getReportDate());
                platform.setPlatformCode(cfg.platformCode);
                platform.setPlatformName(cfg.platformName);
                platform.setCreateTime(now);
            }
            platform.setScore(score);
            platform.setScanCount(scanCount != null ? scanCount : 0);
            platform.setGoodReviewCount(goodReviewCount != null ? goodReviewCount : 0);
            platform.setUpdateTime(now);

            if (platform.getId() == null) {
                dailyReportPlatformMapper.insert(platform);
            } else {
                dailyReportPlatformMapper.updateById(platform);
            }
        }
    }

    @Override
    public boolean lock(Long reportId) {
        DailyReport report = dailyReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException("日报不存在");
        if (report.getStatus() != 1) throw new BusinessException("只能锁定已提交的日报");

        report.setStatus(2); // 锁定
        report.setLockTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        report.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return dailyReportMapper.updateById(report) > 0;
    }

    @Override
    public boolean unlock(Long reportId) {
        DailyReport report = dailyReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException("日报不存在");
        if (report.getStatus() != 2) throw new BusinessException("只能解锁已锁定的日报");

        report.setStatus(1);
        report.setLockTime(null);
        report.setLockUserId(null);
        report.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return dailyReportMapper.updateById(report) > 0;
    }

    @Override
    public boolean reject(Long reportId, String reason) {
        DailyReport report = dailyReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException("日报不存在");
        if (report.getStatus() == 2) throw new BusinessException("日报已锁定，请先解锁");

        report.setStatus(3); // 退回
        report.setRejectReason(reason);
        report.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        return dailyReportMapper.updateById(report) > 0;
    }

    @Override
    public Page<DailyReport> query(DailyReportQueryDTO query) {
        Page<DailyReport> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getStoreId() != null, DailyReport::getStoreId, query.getStoreId())
                .ge(query.getStartDate() != null, DailyReport::getReportDate, query.getStartDate())
                .le(query.getEndDate() != null, DailyReport::getReportDate, query.getEndDate())
                .eq(query.getStatus() != null, DailyReport::getStatus, query.getStatus())
                .orderByDesc(DailyReport::getReportDate);
        return dailyReportMapper.selectPage(page, wrapper);
    }

    @Override
    public List<DailyReportField> getTemplateFields(Long templateId) {
        return dailyReportFieldMapper.selectList(
                new LambdaQueryWrapper<DailyReportField>()
                        .eq(DailyReportField::getTemplateId, templateId)
                        .eq(DailyReportField::getStatus, 1)
                        .orderByAsc(DailyReportField::getSortNo)
        );
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString()).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> getUnfilledStats(String startDate, String endDate) {
        // 获取所有门店
        List<Store> stores = storeMapper.selectList(null);
        // 获取已填报的日期范围
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(DailyReport::getReportDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(DailyReport::getReportDate, endDate);
        }
        List<DailyReport> filledReports = dailyReportMapper.selectList(wrapper);
        // 计算未填报
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Store store : stores) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("storeId", store.getId());
            item.put("storeName", store.getStoreName());
            item.put("filled", filledReports.stream().filter(r -> r.getStoreId().equals(store.getId())).count());
            item.put("status", "已填报");
            result.add(item);
        }
        return result;
    }
}
