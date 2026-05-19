package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.BusinessException;
import com.linxi.dto.DailyReportQueryDTO;
import com.linxi.dto.DailyReportSaveDTO;
import com.linxi.entity.*;
import com.linxi.mapper.*;
import com.linxi.service.DailyChannelService;
import com.linxi.service.DailyExtensionService;
import com.linxi.service.DailyReportService;
import com.linxi.utils.FormulaCalculator;
import com.linxi.utils.WeekDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private DailyReportFieldMapper dailyReportFieldMapper;

    @Autowired
    private DailyReportTemplateMapper dailyReportTemplateMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private DailyChannelService dailyChannelService;

    @Autowired
    private DailyExtensionService dailyExtensionService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getTodayReport(Long storeId, String reportDate) {
        String date = (reportDate != null && !reportDate.isEmpty()) ? reportDate : DateUtil.today();
        return getOrCreateDraft(storeId, date);
    }

    @Override
    public Map<String, Object> getDetail(Long storeId, String reportDate) {
        return getReportDetail(storeId, reportDate);
    }

    private Map<String, Object> getReportDetail(Long storeId, String reportDate) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException("门店不存在");
        }

        // 查找该门店适用的模板
        DailyReportTemplate template = getTemplateByStore(storeId);
        if (template == null) {
            throw new BusinessException("该门店未配置日报模板");
        }

        // 查询是否已有日报（编辑场景，不自动创建）
        DailyReport report = dailyReportMapper.selectOne(
                new LambdaQueryWrapper<DailyReport>()
                        .eq(DailyReport::getStoreId, storeId)
                        .eq(DailyReport::getReportDate, LocalDate.parse(reportDate))
        );

        if (report == null) {
            throw new BusinessException("日报不存在");
        }

        // 补充门店信息
        report.setStoreName(store.getStoreName());
        report.setStoreCode(store.getStoreCode());

        // 查询字段配置
        List<DailyReportField> fields = dailyReportFieldMapper.selectList(
                new LambdaQueryWrapper<DailyReportField>()
                        .eq(DailyReportField::getTemplateId, template.getId())
                        .eq(DailyReportField::getStatus, 1)
                        .orderByAsc(DailyReportField::getSortNo)
        );

        // 构建值Map - 先从report对象获取（列式存储）
        Map<String, Object> valueMap = buildValueMapFromReport(report);

        // 补充渠道评分数据
        Map<String, Object> channelMap = buildValueMapFromChannel(report.getId());
        valueMap.putAll(channelMap);

        // 补充弹性域数据
        Map<String, Object> extensionMap = buildValueMapFromExtension(report.getId());
        valueMap.putAll(extensionMap);

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
                        .eq(DailyReport::getReportDate, LocalDate.parse(reportDate))
        );

        if (report == null) {
            // 创建草稿
            report = new DailyReport();
            report.setStoreId(storeId);
            report.setStoreName(store.getStoreName());
            report.setStoreCode(store.getStoreCode());
            report.setReportDate(LocalDate.parse(reportDate));
            report.setReportMonth(reportDate.substring(0, 7));
            report.setWeekDay(WeekDayUtil.getWeekDay(reportDate));
            report.setTemplateId(template.getId());
            report.setStatus(0); // 草稿
            report.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            report.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            dailyReportMapper.insert(report);
        } else {
            // 补充门店信息
            report.setStoreName(store.getStoreName());
            report.setStoreCode(store.getStoreCode());
        }

        // 查询字段配置
        List<DailyReportField> fields = dailyReportFieldMapper.selectList(
                new LambdaQueryWrapper<DailyReportField>()
                        .eq(DailyReportField::getTemplateId, template.getId())
                        .eq(DailyReportField::getStatus, 1)
                        .orderByAsc(DailyReportField::getSortNo)
        );

        // 构建值Map - 先从report对象获取（列式存储）
        Map<String, Object> valueMap = buildValueMapFromReport(report);

        // 补充渠道评分数据
        Map<String, Object> channelMap = buildValueMapFromChannel(report.getId());
        valueMap.putAll(channelMap);

        // 补充弹性域数据
        Map<String, Object> extensionMap = buildValueMapFromExtension(report.getId());
        valueMap.putAll(extensionMap);

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

    /**
     * 从report对象构建值Map（列式存储）
     */
    private Map<String, Object> buildValueMapFromReport(DailyReport report) {
        Map<String, Object> valueMap = new HashMap<>();

        // 房量字段
        if (report.getOwnRoomCount() != null) valueMap.put("own_room_count", report.getOwnRoomCount());
        if (report.getHourlyRoomCount() != null) valueMap.put("hourly_room_count", report.getHourlyRoomCount());
        if (report.getRepairRoomCount() != null) valueMap.put("repair_room_count", report.getRepairRoomCount());

        // 间夜数字段
        if (report.getRoomNights() != null) valueMap.put("room_nights", report.getRoomNights());
        if (report.getWalkinRoomNights() != null) valueMap.put("walkin_room_nights", report.getWalkinRoomNights());
        if (report.getCtripRoomNights() != null) valueMap.put("ctrip_room_nights", report.getCtripRoomNights());
        if (report.getLyRoomNights() != null) valueMap.put("ly_room_nights", report.getLyRoomNights());
        if (report.getQunarRoomNights() != null) valueMap.put("qunar_room_nights", report.getQunarRoomNights());
        if (report.getZhixingRoomNights() != null) valueMap.put("zhixing_room_nights", report.getZhixingRoomNights());
        if (report.getExternalRoomNights() != null) valueMap.put("external_room_nights", report.getExternalRoomNights());
        if (report.getMeituanHotelRoomNights() != null) valueMap.put("meituan_hotel_room_nights", report.getMeituanHotelRoomNights());
        if (report.getFliggyRoomNights() != null) valueMap.put("fliggy_room_nights", report.getFliggyRoomNights());
        if (report.getDouyinRoomNights() != null) valueMap.put("douyin_room_nights", report.getDouyinRoomNights());
        if (report.getXiaozhuRoomNights() != null) valueMap.put("xiaozhu_room_nights", report.getXiaozhuRoomNights());
        if (report.getTujiaRoomNights() != null) valueMap.put("tujia_room_nights", report.getTujiaRoomNights());
        if (report.getMeituanHomestayRoomNights() != null) valueMap.put("meituan_homestay_room_nights", report.getMeituanHomestayRoomNights());
        if (report.getJialiRoomNights() != null) valueMap.put("jiali_room_nights", report.getJialiRoomNights());

        // 经营指标
        if (report.getOccupancyRate() != null) valueMap.put("occupancy_rate", report.getOccupancyRate());
        if (report.getAdr() != null) valueMap.put("adr", report.getAdr());
        if (report.getRevpar() != null) valueMap.put("revpar", report.getRevpar());

        // 收入字段
        if (report.getDailyRoomFee() != null) valueMap.put("daily_room_fee", report.getDailyRoomFee());
        if (report.getHourlyRoomFee() != null) valueMap.put("hourly_room_fee", report.getHourlyRoomFee());
        if (report.getOtherFee() != null) valueMap.put("other_fee", report.getOtherFee());
        if (report.getTotalRevenue() != null) valueMap.put("total_revenue", report.getTotalRevenue());
        if (report.getDepositAmount() != null) valueMap.put("deposit_amount", report.getDepositAmount());

        // 扫码字段
        if (report.getCtripScanCount() != null) valueMap.put("ctrip_scan_count", report.getCtripScanCount());
        if (report.getMeituanScanCount() != null) valueMap.put("meituan_scan_count", report.getMeituanScanCount());

        return valueMap;
    }

    /**
     * 从渠道表构建值Map
     */
    private Map<String, Object> buildValueMapFromChannel(Long reportId) {
        Map<String, Object> valueMap = new HashMap<>();
        if (reportId == null) return valueMap;

        DailyChannel channel = dailyChannelService.getByReportId(reportId);
        if (channel == null) return valueMap;

        // 评分字段
        if (channel.getCtripScore() != null) valueMap.put("ctrip_score", channel.getCtripScore());
        if (channel.getLyScore() != null) valueMap.put("ly_score", channel.getLyScore());
        if (channel.getQunarScore() != null) valueMap.put("qunar_score", channel.getQunarScore());
        if (channel.getZhixingScore() != null) valueMap.put("zhixing_score", channel.getZhixingScore());
        if (channel.getMeituanScore() != null) valueMap.put("meituan_score", channel.getMeituanScore());
        if (channel.getFliggyScore() != null) valueMap.put("fliggy_score", channel.getFliggyScore());

        // 好评数字段
        if (channel.getCtripGoodReviewCount() != null) valueMap.put("ctrip_good_review_count", channel.getCtripGoodReviewCount());
        if (channel.getMeituanGoodReviewCount() != null) valueMap.put("meituan_good_review_count", channel.getMeituanGoodReviewCount());
        if (channel.getLyGoodReviewCount() != null) valueMap.put("ly_good_review_count", channel.getLyGoodReviewCount());
        if (channel.getQunarGoodReviewCount() != null) valueMap.put("qunar_good_review_count", channel.getQunarGoodReviewCount());
        if (channel.getZhixingGoodReviewCount() != null) valueMap.put("zhixing_good_review_count", channel.getZhixingGoodReviewCount());
        if (channel.getFliggyGoodReviewCount() != null) valueMap.put("fliggy_good_review_count", channel.getFliggyGoodReviewCount());

        return valueMap;
    }

    /**
     * 从弹性域表构建值Map
     */
    private Map<String, Object> buildValueMapFromExtension(Long reportId) {
        Map<String, Object> valueMap = new HashMap<>();
        if (reportId == null) return valueMap;

        DailyExtension extension = dailyExtensionService.getByReportId(reportId);
        if (extension == null) return valueMap;

        // 30个弹性域字段
        for (int i = 1; i <= 30; i++) {
            String fieldCode = "ext_field_" + i;
            try {
                java.lang.reflect.Method getter = extension.getClass().getMethod("getExtField" + i);
                Object val = getter.invoke(extension);
                if (val != null) {
                    valueMap.put(fieldCode, val);
                }
            } catch (Exception ignored) {
            }
        }

        return valueMap;
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
        DailyReport report = getOrCreateReportFromDTO(dto);
        Map<String, Object> values = normalizeKeys(dto.getValues());
        updateReportFields(report, values);
        // 已退回的日报保存草稿后状态保持为已退回(3)，其余情况设为草稿(0)
        if (report.getStatus() == null || report.getStatus() != 3) {
            report.setStatus(0);
        }
        report.setUpdateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        dailyReportMapper.updateById(report);

        // 保存渠道和弹性域数据
        dailyChannelService.saveOrUpdate(report.getId(), dto.getStoreId(), dto.getReportDate(), values);
        dailyExtensionService.saveOrUpdate(report.getId(), dto.getStoreId(), dto.getReportDate(), values);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(DailyReportSaveDTO dto) {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        DailyReport report = getOrCreateReportFromDTO(dto);

        // 校验必填字段
        List<DailyReportField> fields = getTemplateFields(report.getTemplateId());
        Map<String, Object> values = normalizeKeys(dto.getValues() != null ? dto.getValues() : new HashMap<>());
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

        // 更新主表字段
        updateReportFields(report, values);
        report.setStatus(1); // 已提交
        report.setSubmitTime(now);
        report.setUpdateTime(now);
        dailyReportMapper.updateById(report);

        // 保存渠道和弹性域数据
        dailyChannelService.saveOrUpdate(report.getId(), dto.getStoreId(), dto.getReportDate(), values);
        dailyExtensionService.saveOrUpdate(report.getId(), dto.getStoreId(), dto.getReportDate(), values);

        return true;
    }

    /**
     * 从DTO获取或创建日报，并将values更新到report对象
     */
    private DailyReport getOrCreateReportFromDTO(DailyReportSaveDTO dto) {
        Store store = storeMapper.selectById(dto.getStoreId());
        if (store == null) {
            throw new BusinessException("门店不存在");
        }

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
                            .eq(DailyReport::getReportDate, LocalDate.parse(dto.getReportDate()))
            );
            if (report == null) {
                // 创建新日报
                report = new DailyReport();
                report.setStoreId(dto.getStoreId());
                report.setStoreName(store.getStoreName());
                report.setStoreCode(store.getStoreCode());
                report.setReportDate(LocalDate.parse(dto.getReportDate()));
                report.setReportMonth(dto.getReportDate().substring(0, 7));
                report.setWeekDay(WeekDayUtil.getWeekDay(dto.getReportDate()));
                report.setTemplateId(dto.getTemplateId());
                report.setStatus(0);
                report.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                dailyReportMapper.insert(report);
            }
        }
        return report;
    }

    /**
     * 驼峰转下划线，统一key格式
     */
    private Map<String, Object> normalizeKeys(Map<String, Object> values) {
        if (values == null) return null;
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            String snakeKey = key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            normalized.put(snakeKey, entry.getValue());
        }
        return normalized;
    }

    /**
     * 将values更新到report对象的列式字段
     */
    private void updateReportFields(DailyReport report, Map<String, Object> values) {
        if (values == null) return;

        // 房量字段
        if (values.containsKey("own_room_count")) report.setOwnRoomCount(toInteger(values.get("own_room_count")));
        if (values.containsKey("hourly_room_count")) report.setHourlyRoomCount(toInteger(values.get("hourly_room_count")));
        if (values.containsKey("repair_room_count")) report.setRepairRoomCount(toInteger(values.get("repair_room_count")));

        // 间夜数字段
        if (values.containsKey("room_nights")) report.setRoomNights(toDecimal(values.get("room_nights")));
        if (values.containsKey("walkin_room_nights")) report.setWalkinRoomNights(toDecimal(values.get("walkin_room_nights")));
        if (values.containsKey("ctrip_room_nights")) report.setCtripRoomNights(toDecimal(values.get("ctrip_room_nights")));
        if (values.containsKey("ly_room_nights")) report.setLyRoomNights(toDecimal(values.get("ly_room_nights")));
        if (values.containsKey("qunar_room_nights")) report.setQunarRoomNights(toDecimal(values.get("qunar_room_nights")));
        if (values.containsKey("zhixing_room_nights")) report.setZhixingRoomNights(toDecimal(values.get("zhixing_room_nights")));
        if (values.containsKey("external_room_nights")) report.setExternalRoomNights(toDecimal(values.get("external_room_nights")));
        if (values.containsKey("meituan_hotel_room_nights")) report.setMeituanHotelRoomNights(toDecimal(values.get("meituan_hotel_room_nights")));
        if (values.containsKey("fliggy_room_nights")) report.setFliggyRoomNights(toDecimal(values.get("fliggy_room_nights")));
        if (values.containsKey("douyin_room_nights")) report.setDouyinRoomNights(toDecimal(values.get("douyin_room_nights")));
        if (values.containsKey("xiaozhu_room_nights")) report.setXiaozhuRoomNights(toDecimal(values.get("xiaozhu_room_nights")));
        if (values.containsKey("tujia_room_nights")) report.setTujiaRoomNights(toDecimal(values.get("tujia_room_nights")));
        if (values.containsKey("meituan_homestay_room_nights")) report.setMeituanHomestayRoomNights(toDecimal(values.get("meituan_homestay_room_nights")));
        if (values.containsKey("jiali_room_nights")) report.setJialiRoomNights(toDecimal(values.get("jiali_room_nights")));

        // 经营指标
        if (values.containsKey("occupancy_rate")) report.setOccupancyRate(toDecimal(values.get("occupancy_rate")));
        if (values.containsKey("adr")) report.setAdr(toDecimal(values.get("adr")));
        if (values.containsKey("revpar")) report.setRevpar(toDecimal(values.get("revpar")));

        // 收入字段
        if (values.containsKey("daily_room_fee")) report.setDailyRoomFee(toDecimal(values.get("daily_room_fee")));
        if (values.containsKey("hourly_room_fee")) report.setHourlyRoomFee(toDecimal(values.get("hourly_room_fee")));
        if (values.containsKey("other_fee")) report.setOtherFee(toDecimal(values.get("other_fee")));
        if (values.containsKey("total_revenue")) report.setTotalRevenue(toDecimal(values.get("total_revenue")));
        if (values.containsKey("deposit_amount")) report.setDepositAmount(toDecimal(values.get("deposit_amount")));

        // 扫码字段
        if (values.containsKey("ctrip_scan_count")) report.setCtripScanCount(toInteger(values.get("ctrip_scan_count")));
        if (values.containsKey("meituan_scan_count")) report.setMeituanScanCount(toInteger(values.get("meituan_scan_count")));
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
        if (val == null) return null;
        try {
            String str = val.toString();
            if (str.contains(".")) {
                str = str.split("\\.")[0];
            }
            return Integer.parseInt(str);
        } catch (Exception e) {
            return null;
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
        Page<DailyReport> page = new Page<>(query.getPageNo() != null ? query.getPageNo() : 1, query.getPageSize() != null ? query.getPageSize() : 10);
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<>();
        if (query.getStoreId() != null) {
            wrapper.eq(DailyReport::getStoreId, query.getStoreId());
        }
        if (query.getStartDate() != null && !query.getStartDate().isEmpty()) {
            wrapper.ge(DailyReport::getReportDate, LocalDate.parse(query.getStartDate()));
        }
        if (query.getEndDate() != null && !query.getEndDate().isEmpty()) {
            wrapper.le(DailyReport::getReportDate, LocalDate.parse(query.getEndDate()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(DailyReport::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(DailyReport::getReportDate);
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

    @Override
    public List<Map<String, Object>> getUnfilledStats(String startDate, String endDate) {
        // 1. 获取所有活跃门店
        List<Store> stores = storeMapper.selectList(
            new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1)
        );

        // 2. 如果没有传日期，默认查今天
        if (startDate == null || startDate.isEmpty()) startDate = DateUtil.today();
        if (endDate == null || endDate.isEmpty()) endDate = startDate;

        // 3. 查询日期范围内已填报的门店
        List<DailyReport> reports = dailyReportMapper.selectList(
            new LambdaQueryWrapper<DailyReport>()
                .ge(DailyReport::getReportDate, LocalDate.parse(startDate))
                .le(DailyReport::getReportDate, LocalDate.parse(endDate))
                .in(DailyReport::getStatus, Arrays.asList(1, 2))
        );

        // 4. 找出未填报的门店
        Set<Long> filledStoreIds = reports.stream()
            .map(DailyReport::getStoreId)
            .collect(Collectors.toSet());

        // 5. 批量查询所有门店的最近填报日期（一次查询，避免N+1）
        Map<Long, String> lastReportDateMap = new HashMap<>();
        List<DailyReport> allSubmitted = dailyReportMapper.selectList(
            new LambdaQueryWrapper<DailyReport>()
                .in(DailyReport::getStatus, Arrays.asList(1, 2))
                .orderByDesc(DailyReport::getReportDate)
        );
        for (DailyReport r : allSubmitted) {
            lastReportDateMap.putIfAbsent(r.getStoreId(), r.getReportDate().toString());
        }

        // 6. 批量查询店长信息
        Set<Long> managerIds = stores.stream()
            .map(Store::getManagerUserId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, SysUser> managerMap = new HashMap<>();
        if (!managerIds.isEmpty()) {
            List<SysUser> managers = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().in(SysUser::getId, managerIds)
            );
            for (SysUser m : managers) {
                managerMap.put(m.getId(), m);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Store store : stores) {
            if (!filledStoreIds.contains(store.getId())) {
                Map<String, Object> item = new HashMap<>();
                item.put("storeId", store.getId());
                item.put("storeName", store.getStoreName());
                item.put("city", store.getCity());
                item.put("region", store.getRegionName());
                item.put("lastReportDate", lastReportDateMap.getOrDefault(store.getId(), "-"));
                // 填充店长信息
                if (store.getManagerUserId() != null) {
                    SysUser manager = managerMap.get(store.getManagerUserId());
                    if (manager != null) {
                        item.put("managerName", manager.getRealName());
                        item.put("managerPhone", manager.getPhone());
                    }
                }
                result.add(item);
            }
        }
        return result;
    }
}
