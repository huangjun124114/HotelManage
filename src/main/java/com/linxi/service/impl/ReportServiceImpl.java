package com.linxi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linxi.entity.*;
import com.linxi.mapper.*;
import com.linxi.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private DailyReportSummaryMapper summaryMapper;

    @Autowired
    private DailyReportMapper reportMapper;

    @Autowired
    private DailyReportChannelMapper channelMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Override
    public Map<String, Object> dashboard(String date, String region, Long storeId) {
        Map<String, Object> result = new HashMap<>();

        if (date == null || date.isEmpty()) {
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        // 查询当天所有摘要数据
        List<DailyReportSummary> summaries = summaryMapper.selectList(
                new LambdaQueryWrapper<DailyReportSummary>()
                        .eq(DailyReportSummary::getReportDate, date)
        );

        // 查询所有启用门店
        List<Store> allStores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1)
        );

        if (storeId != null) {
            summaries = summaries.stream()
                    .filter(s -> s.getStoreId().equals(storeId))
                    .collect(Collectors.toList());
            allStores = allStores.stream()
                    .filter(s -> s.getId().equals(storeId))
                    .collect(Collectors.toList());
        } else if (region != null && !region.isEmpty()) {
            allStores = allStores.stream()
                    .filter(s -> region.equals(s.getRegionName()))
                    .collect(Collectors.toList());
            List<Long> storeIds = allStores.stream().map(Store::getId).collect(Collectors.toList());
            summaries = summaries.stream()
                    .filter(s -> storeIds.contains(s.getStoreId()))
                    .collect(Collectors.toList());
        }

        int totalStores = allStores.size();
        int filledCount = summaries.size();
        BigDecimal fillRate = totalStores > 0
                ? new BigDecimal(filledCount).divide(new BigDecimal(totalStores), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 汇总指标
        BigDecimal totalRevenue = summaries.stream()
                .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRooms = summaries.stream()
                .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgOccupancy = summaries.isEmpty() ? BigDecimal.ZERO
                : summaries.stream()
                .map(s -> s.getOccupancyRate() != null ? s.getOccupancyRate() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(summaries.size()), 4, RoundingMode.HALF_UP);

        BigDecimal avgAdr = summaries.isEmpty() ? BigDecimal.ZERO
                : summaries.stream()
                .map(s -> s.getAdr() != null ? s.getAdr() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(summaries.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgRevpar = summaries.isEmpty() ? BigDecimal.ZERO
                : summaries.stream()
                .map(s -> s.getRevpar() != null ? s.getRevpar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(summaries.size()), 2, RoundingMode.HALF_UP);

        int unfilledCount = totalStores - filledCount;

        // 门店排名Top10（按总营收）
        Map<Long, Store> storeMap = allStores.stream()
                .collect(Collectors.toMap(Store::getId, s -> s, (a, b) -> a));
        List<Map<String, Object>> top10 = summaries.stream()
                .sorted((a, b) -> {
                    BigDecimal va = a.getTotalRevenue() != null ? a.getTotalRevenue() : BigDecimal.ZERO;
                    BigDecimal vb = b.getTotalRevenue() != null ? b.getTotalRevenue() : BigDecimal.ZERO;
                    return vb.compareTo(va);
                })
                .limit(10)
                .map(s -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("storeId", s.getStoreId());
                    Store store = storeMap.get(s.getStoreId());
                    item.put("storeName", store != null ? store.getStoreName() : "");
                    item.put("revenue", s.getTotalRevenue());
                    item.put("occupancyRate", s.getOccupancyRate());
                    item.put("adr", s.getAdr());
                    return item;
                })
                .collect(Collectors.toList());

        // 最近7日趋势
        List<Map<String, Object>> trend7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String trendDate = LocalDate.parse(date).minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<DailyReportSummary> daySummaries = summaryMapper.selectList(
                    new LambdaQueryWrapper<DailyReportSummary>().eq(DailyReportSummary::getReportDate, trendDate)
            );

            BigDecimal dayRevenue = daySummaries.stream()
                    .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayOcc = daySummaries.isEmpty() ? BigDecimal.ZERO
                    : daySummaries.stream()
                    .map(s -> s.getOccupancyRate() != null ? s.getOccupancyRate() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(Math.max(daySummaries.size(), 1)), 4, RoundingMode.HALF_UP);

            Map<String, Object> trend = new HashMap<>();
            trend.put("date", trendDate);
            trend.put("revenue", dayRevenue);
            trend.put("occupancyRate", dayOcc);
            trend7Days.add(trend);
        }

        // 未填报门店列表
        Set<Long> filledStoreIds = summaries.stream().map(DailyReportSummary::getStoreId).collect(Collectors.toSet());
        List<Map<String, Object>> unfilledStores = allStores.stream()
                .filter(s -> !filledStoreIds.contains(s.getId()))
                .map(s -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("storeName", s.getStoreName());
                    item.put("city", s.getCity());
                    item.put("managerName", "");
                    return item;
                })
                .collect(Collectors.toList());

        // 前端期望的排名格式（营收排名 + 出租率排名）
        List<Map<String, Object>> revenueRanking = top10.stream().map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("storeName", item.get("storeName"));
            m.put("revenue", item.get("revenue"));
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> occupancyRanking = top10.stream()
                .sorted((a, b) -> {
                    BigDecimal oa = (BigDecimal) a.getOrDefault("occupancyRate", BigDecimal.ZERO);
                    BigDecimal ob = (BigDecimal) b.getOrDefault("occupancyRate", BigDecimal.ZERO);
                    return ob.compareTo(oa);
                })
                .map(item -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("storeName", item.get("storeName"));
                    BigDecimal occ = (BigDecimal) item.getOrDefault("occupancyRate", BigDecimal.ZERO);
                    m.put("occupancy", occ.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
                    return m;
                })
                .collect(Collectors.toList());

        // 前端期望的趋势格式（平铺数组）
        List<String> dateLabels = new ArrayList<>();
        List<BigDecimal> revenueTrend = new ArrayList<>();
        List<BigDecimal> occupancyTrend = new ArrayList<>();
        for (Map<String, Object> t : trend7Days) {
            dateLabels.add((String) t.get("date"));
            revenueTrend.add((BigDecimal) t.get("revenue"));
            BigDecimal occ = (BigDecimal) t.getOrDefault("occupancyRate", BigDecimal.ZERO);
            occupancyTrend.add(occ.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
        }

        result.put("totalStores", totalStores);
        result.put("filledCount", filledCount);
        result.put("unfilledCount", unfilledCount);
        result.put("totalRevenue", totalRevenue);
        result.put("totalRooms", totalRooms);
        result.put("avgOccupancy", avgOccupancy.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
        result.put("avgAdr", avgAdr);
        result.put("avgRevpar", avgRevpar);
        result.put("fillRate", fillRate);
        result.put("top10", top10);
        result.put("trend7Days", trend7Days);
        // 前端Home.vue期望的字段
        result.put("revenueRanking", revenueRanking);
        result.put("occupancyRanking", occupancyRanking);
        result.put("dateLabels", dateLabels);
        result.put("revenueTrend", revenueTrend);
        result.put("occupancyTrend", occupancyTrend);
        result.put("unfilledStores", unfilledStores);
        return result;
    }

    @Override
    public Map<String, Object> monthlyReport(Long storeId, String month) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<DailyReportSummary> wrapper = new LambdaQueryWrapper<DailyReportSummary>()
                .eq(DailyReportSummary::getReportMonth, month);
        if (storeId != null) {
            wrapper.eq(DailyReportSummary::getStoreId, storeId);
        }

        List<DailyReportSummary> summaries = summaryMapper.selectList(wrapper);

        BigDecimal totalRevenue = summaries.stream()
                .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRoomNights = summaries.stream()
                .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDailyFee = summaries.stream()
                .map(s -> s.getDailyRoomFee() != null ? s.getDailyRoomFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalHourlyFee = summaries.stream()
                .map(s -> s.getHourlyRoomFee() != null ? s.getHourlyRoomFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOtherFee = summaries.stream()
                .map(s -> s.getOtherFee() != null ? s.getOtherFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.put("month", month);
        result.put("reportCount", summaries.size());
        result.put("totalRevenue", totalRevenue);
        result.put("totalRoomNights", totalRoomNights);
        result.put("totalDailyFee", totalDailyFee);
        result.put("totalHourlyFee", totalHourlyFee);
        result.put("totalOtherFee", totalOtherFee);

        // 日均数据
        if (!summaries.isEmpty()) {
            result.put("avgDailyRevenue", totalRevenue.divide(new BigDecimal(summaries.size()), 2, RoundingMode.HALF_UP));
            result.put("avgDailyRoomNights", totalRoomNights.divide(new BigDecimal(summaries.size()), 1, RoundingMode.HALF_UP));
            result.put("avgOccupancyRate", summaries.stream()
                    .map(s -> s.getOccupancyRate() != null ? s.getOccupancyRate() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(summaries.size()), 4, RoundingMode.HALF_UP));
            result.put("avgAdr", summaries.stream()
                    .map(s -> s.getAdr() != null ? s.getAdr() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(summaries.size()), 2, RoundingMode.HALF_UP));
            result.put("avgRevpar", summaries.stream()
                    .map(s -> s.getRevpar() != null ? s.getRevpar() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(summaries.size()), 2, RoundingMode.HALF_UP));
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> trend(Long storeId, String startDate, String endDate, String metric) {
        List<Map<String, Object>> result = new ArrayList<>();

        LambdaQueryWrapper<DailyReportSummary> wrapper = new LambdaQueryWrapper<DailyReportSummary>()
                .ge(DailyReportSummary::getReportDate, startDate)
                .le(DailyReportSummary::getReportDate, endDate);
        if (storeId != null) {
            wrapper.eq(DailyReportSummary::getStoreId, storeId);
        }

        List<DailyReportSummary> summaries = summaryMapper.selectList(wrapper.orderByAsc(DailyReportSummary::getReportDate));

        // 按日期分组
        Map<String, List<DailyReportSummary>> grouped = summaries.stream()
                .collect(Collectors.groupingBy(DailyReportSummary::getReportDate, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<DailyReportSummary>> entry : grouped.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", entry.getKey());
            List<DailyReportSummary> daySummaries = entry.getValue();

            BigDecimal dayRevenue = daySummaries.stream()
                    .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayRoomNights = daySummaries.stream()
                    .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            point.put("revenue", dayRevenue);
            point.put("roomNights", dayRoomNights);

            if (!daySummaries.isEmpty()) {
                point.put("occupancyRate", daySummaries.stream()
                        .map(s -> s.getOccupancyRate() != null ? s.getOccupancyRate() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(daySummaries.size()), 4, RoundingMode.HALF_UP));
                point.put("adr", daySummaries.stream()
                        .map(s -> s.getAdr() != null ? s.getAdr() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(daySummaries.size()), 2, RoundingMode.HALF_UP));
            }

            point.put("count", daySummaries.size());
            result.add(point);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> channelAnalysis(Long storeId, String startDate, String endDate) {
        LambdaQueryWrapper<DailyReportChannel> wrapper = new LambdaQueryWrapper<DailyReportChannel>()
                .ge(DailyReportChannel::getReportDate, startDate)
                .le(DailyReportChannel::getReportDate, endDate);
        if (storeId != null) {
            wrapper.eq(DailyReportChannel::getStoreId, storeId);
        }

        List<DailyReportChannel> channels = channelMapper.selectList(wrapper);

        // 按渠道汇总
        Map<String, BigDecimal> channelSum = new LinkedHashMap<>();
        Map<String, String> channelNameMap = new LinkedHashMap<>();

        for (DailyReportChannel ch : channels) {
            channelSum.merge(ch.getChannelCode(), ch.getRoomNights() != null ? ch.getRoomNights() : BigDecimal.ZERO, BigDecimal::add);
            channelNameMap.put(ch.getChannelCode(), ch.getChannelName());
        }

        BigDecimal total = channelSum.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : channelSum.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("channelCode", entry.getKey());
            item.put("channelName", channelNameMap.get(entry.getKey()));
            item.put("roomNights", entry.getValue());
            item.put("ratio", total.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().divide(total, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            result.add(item);
        }

        result.sort((a, b) -> {
            BigDecimal va = (BigDecimal) a.get("roomNights");
            BigDecimal vb = (BigDecimal) b.get("roomNights");
            return vb.compareTo(va);
        });

        return result;
    }

    @Override
    public List<Map<String, Object>> storeRanking(String date, String metric) {
        // 查询新表 daily_report（不再用已废弃的 daily_report_summary）
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getReportDate, date);

        List<DailyReport> reports = reportMapper.selectList(wrapper);

        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1));
        Map<Long, String> storeNameMap = stores.stream()
                .collect(Collectors.toMap(Store::getId, Store::getStoreName, (a, b) -> a));

        List<Map<String, Object>> result = reports.stream()
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("storeId", r.getStoreId());
                    item.put("storeName", r.getStoreName() != null ? r.getStoreName() : storeNameMap.getOrDefault(r.getStoreId(), ""));

                    BigDecimal value = BigDecimal.ZERO;
                    if ("revenue".equals(metric)) {
                        value = r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO;
                    } else if ("occupancyRate".equals(metric) || "occupancy".equals(metric)) {
                        value = r.getOccupancyRate() != null ? r.getOccupancyRate() : BigDecimal.ZERO;
                    } else if ("adr".equals(metric)) {
                        value = r.getAdr() != null ? r.getAdr() : BigDecimal.ZERO;
                    } else if ("revpar".equals(metric)) {
                        value = r.getRevpar() != null ? r.getRevpar() : BigDecimal.ZERO;
                    } else if ("rooms".equals(metric)) {
                        value = r.getRoomNights() != null ? r.getRoomNights() : BigDecimal.ZERO;
                    } else {
                        value = r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO;
                    }
                    item.put("value", value);
                    return item;
                })
                .sorted((a, b) -> {
                    BigDecimal va = (BigDecimal) a.get("value");
                    BigDecimal vb = (BigDecimal) b.get("value");
                    return vb.compareTo(va);
                })
                .collect(Collectors.toList());

        // 添加排名
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank", i + 1);
        }

        return result;
    }
}
