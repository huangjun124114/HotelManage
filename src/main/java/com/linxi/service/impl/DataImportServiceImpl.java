package com.linxi.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linxi.common.BusinessException;
import com.linxi.entity.*;
import com.linxi.mapper.*;
import com.linxi.service.DataImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class DataImportServiceImpl implements DataImportService {

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private DailyReportMapper dailyReportMapper;

    @Autowired
    private DailyReportValueMapper dailyReportValueMapper;

    @Autowired
    private DailyReportSummaryMapper dailyReportSummaryMapper;

    @Autowired
    private DailyReportFieldMapper dailyReportFieldMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(MultipartFile file) throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);

        EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {

            private List<String> headers = null;
            private final Map<String, String> fieldMapping = new LinkedHashMap<>();
            private boolean headerParsed = false;

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                if (!headerParsed) {
                    // 跳过表头行，假设第一行为标题，第二行为表头
                    if (headers == null) {
                        headers = new ArrayList<>(data.values());
                        return;
                    }
                    parseHeaders(data);
                    headerParsed = true;
                    return;
                }
                importRow(data);
                successCount.incrementAndGet();
            }

            private void parseHeaders(Map<Integer, String> headerRow) {
                // 列索引到字段映射
                Map<String, Integer> colIndex = new HashMap<>();
                for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
                    colIndex.put(entry.getValue(), entry.getKey());
                }

                // 基础字段映射
                String[] fieldPairs = {
                        "日期,report_date",
                        "门店,store_name",
                        "自有房量,own_room_count",
                        "维修房,repair_room_count",
                        "钟点房数量,hourly_room_count",
                        "间夜数,room_nights",
                        "散客,walkin_room_nights",
                        "携程,ctrip_room_nights",
                        "同程艺龙,ly_room_nights",
                        "去哪儿,qunar_room_nights",
                        "智行,zhixing_room_nights",
                        "外网,external_room_nights",
                        "美团酒店,meituan_hotel_room_nights",
                        "飞猪,fliggy_room_nights",
                        "抖音,douyin_room_nights",
                        "小猪,xiaozhu_room_nights",
                        "途家,tujia_room_nights",
                        "美团民宿,meituan_homestay_room_nights",
                        "红色加力,jiali_room_nights",
                        "日租房房费,daily_room_fee",
                        "钟点房费用,hourly_room_fee",
                        "杂费,other_fee",
                        "当日总营收,total_revenue",
                        "押金,deposit_amount",
                        "携程扫码,ctrip_scan_count",
                        "美团扫码,meituan_scan_count",
                        "携程好评数,ctrip_good_review_count",
                        "美团好评数,meituan_good_review_count",
                        "同程好评数,ly_good_review_count",
                        "去哪好评数,qunar_good_review_count",
                        "智行好评数,zhixing_good_review_count",
                        "飞猪好评数,fliggy_good_review_count",
                        "携程评分,ctrip_score",
                        "同程艺龙评分,ly_score",
                        "去哪评分,qunar_score",
                        "智行评分,zhixing_score",
                        "美团评分,meituan_score",
                        "飞猪评分,fliggy_score",
                };

                for (String pair : fieldPairs) {
                    String[] parts = pair.split(",");
                    Integer idx = colIndex.get(parts[0]);
                    if (idx != null) {
                        fieldMapping.put(parts[1], String.valueOf(idx));
                    }
                }
            }

            private void importRow(Map<Integer, String> row) {
                String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");

                // 获取门店名称
                String storeName = getValue(row, "store_name", fieldMapping);
                String reportDate = getValue(row, "report_date", fieldMapping);

                if (storeName == null || reportDate == null) {
                    log.warn("跳过无效行：缺少门店名称或日期");
                    return;
                }

                // 匹配门店
                Store store = storeMapper.selectOne(
                        new LambdaQueryWrapper<Store>()
                                .eq(Store::getStoreName, storeName)
                                .or()
                                .like(Store::getShortName, storeName)
                );
                if (store == null) {
                    log.warn("门店不存在: {}", storeName);
                    return;
                }

                // 查找或创建日报
                DailyReport report = dailyReportMapper.selectOne(
                        new LambdaQueryWrapper<DailyReport>()
                                .eq(DailyReport::getStoreId, store.getId())
                                .eq(DailyReport::getReportDate, reportDate)
                );

                Long templateId = 1L;

                if (report == null) {
                    report = new DailyReport();
                    report.setStoreId(store.getId());
                    report.setReportDate(reportDate);
                    report.setReportMonth(reportDate.substring(0, 7));
                    report.setTemplateId(templateId);
                    report.setStatus(1);
                    report.setCreateTime(now);
                    report.setUpdateTime(now);
                    dailyReportMapper.insert(report);
                }

                // 获取字段定义
                List<DailyReportField> fields = dailyReportFieldMapper.selectList(
                        new LambdaQueryWrapper<DailyReportField>()
                                .eq(DailyReportField::getTemplateId, templateId)
                                .eq(DailyReportField::getStatus, 1)
                );

                // 构建字段值
                Map<String, Object> values = new HashMap<>();
                for (DailyReportField field : fields) {
                    String indexStr = fieldMapping.get(field.getFieldCode());
                    if (indexStr != null) {
                        String cellValue = row.get(Integer.parseInt(indexStr));
                        if (cellValue != null && !cellValue.isEmpty()) {
                            values.put(field.getFieldCode(), cellValue);
                        }
                    }
                }

                // 保存字段值（复用DailyReportServiceImpl的逻辑简化版）
                List<DailyReportValue> valueList = new ArrayList<>();
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    DailyReportValue val = new DailyReportValue();
                    val.setReportId(report.getId());
                    val.setFieldCode(entry.getKey());
                    val.setValueText(entry.getValue().toString());
                    val.setCreateTime(now);
                    val.setUpdateTime(now);
                    valueList.add(val);
                }
                if (!valueList.isEmpty()) {
                    dailyReportValueMapper.batchSaveOrUpdate(valueList);
                }

                // 写入摘要
                DailyReportSummary summary = dailyReportSummaryMapper.selectOne(
                        new LambdaQueryWrapper<DailyReportSummary>().eq(DailyReportSummary::getReportId, report.getId())
                );
                if (summary == null) {
                    summary = new DailyReportSummary();
                    summary.setReportId(report.getId());
                    summary.setStoreId(report.getStoreId());
                    summary.setReportDate(reportDate);
                    summary.setReportMonth(reportDate.substring(0, 7));
                    summary.setCreateTime(now);
                }
                summary.setUpdateTime(now);
                summary.setTotalRevenue(toBigDecimal(values.get("total_revenue")));
                summary.setDailyRoomFee(toBigDecimal(values.get("daily_room_fee")));
                summary.setHourlyRoomFee(toBigDecimal(values.get("hourly_room_fee")));
                summary.setOtherFee(toBigDecimal(values.get("other_fee")));
                summary.setRoomNights(toBigDecimal(values.get("room_nights")));
                summary.setOwnRoomCount(toBigDecimal(values.get("own_room_count")));

                if (summary.getId() == null) {
                    dailyReportSummaryMapper.insert(summary);
                } else {
                    dailyReportSummaryMapper.updateById(summary);
                }

                log.info("导入成功: 门店={}, 日期={}", storeName, reportDate);
            }

            private String getValue(Map<Integer, String> row, String fieldCode, Map<String, String> mapping) {
                String indexStr = mapping.get(fieldCode);
                if (indexStr == null) return null;
                return row.get(Integer.parseInt(indexStr));
            }

            private BigDecimal toBigDecimal(Object val) {
                if (val == null) return BigDecimal.ZERO;
                try {
                    return new BigDecimal(val.toString());
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("Excel导入完成，共导入{}条", successCount.get());
            }
        }).sheet().doRead();

        return successCount.get();
    }
}
