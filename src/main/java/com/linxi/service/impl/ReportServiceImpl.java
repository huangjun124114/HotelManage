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

    @Autowired
    private DimDateMapper dimDateMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DailyReportValueMapper valueMapper;

    @Override
    public Map<String, Object> dashboard(String date, String region, Long storeId, List<Long> storeIds) {
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

        // 门店过滤：优先storeIds多选，其次storeId单选，再次region
        Set<Long> filterStoreIds = null;
        if (storeIds != null && !storeIds.isEmpty()) {
            filterStoreIds = new HashSet<>(storeIds);
        } else if (storeId != null) {
            filterStoreIds = Collections.singleton(storeId);
        } else if (region != null && !region.isEmpty()) {
            allStores = allStores.stream()
                    .filter(s -> region.equals(s.getRegionName()))
                    .collect(Collectors.toList());
            filterStoreIds = allStores.stream().map(Store::getId).collect(Collectors.toSet());
        }

        if (filterStoreIds != null) {
            final Set<Long> finalFilter = filterStoreIds;
            summaries = summaries.stream()
                    .filter(s -> finalFilter.contains(s.getStoreId()))
                    .collect(Collectors.toList());
            allStores = allStores.stream()
                    .filter(s -> finalFilter.contains(s.getId()))
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

        // 均出租率：加权平均（按各自房量加权）= 总间夜 / 总可用房量
        BigDecimal totalOwnRooms = summaries.stream()
                .map(s -> s.getOwnRoomCount() != null ? s.getOwnRoomCount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgOccupancy = (totalOwnRooms.compareTo(BigDecimal.ZERO) > 0)
                ? totalRooms.divide(totalOwnRooms, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 均ADR：加权平均 = 总营收 / 总间夜
        BigDecimal avgADR = (totalRooms.compareTo(BigDecimal.ZERO) > 0)
                ? totalRevenue.divide(totalRooms, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 均RevPAR：加权平均 = 均出租率 * 均ADR
        BigDecimal avgRevPAR = avgOccupancy.multiply(avgADR).setScale(2, RoundingMode.HALF_UP);

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

        // 最近7日趋势 - 批量查询优化，消除N+1
        String startDate = LocalDate.parse(date).minusDays(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<DailyReportSummary> trendSummaries = summaryMapper.selectList(
                new LambdaQueryWrapper<DailyReportSummary>()
                        .ge(DailyReportSummary::getReportDate, startDate)
                        .le(DailyReportSummary::getReportDate, date)
        );
        // 按门店过滤
        if (filterStoreIds != null) {
            final Set<Long> finalFilter = filterStoreIds;
            trendSummaries = trendSummaries.stream()
                    .filter(s -> finalFilter.contains(s.getStoreId()))
                    .collect(Collectors.toList());
        }
        // 按日期分组
        Map<String, List<DailyReportSummary>> trendGrouped = trendSummaries.stream()
                .collect(Collectors.groupingBy(DailyReportSummary::getReportDate, LinkedHashMap::new, Collectors.toList()));

        // 确保每天都有数据点（补空）
        List<String> dateLabels = new ArrayList<>();
        List<BigDecimal> revenueTrend = new ArrayList<>();
        List<BigDecimal> occupancyTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String d = LocalDate.parse(date).minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            dateLabels.add(d);
            List<DailyReportSummary> dayList = trendGrouped.getOrDefault(d, Collections.emptyList());
            BigDecimal dayRevenue = dayList.stream()
                    .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueTrend.add(dayRevenue);
            BigDecimal dayRooms = dayList.stream()
                    .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayOwnRooms = dayList.stream()
                    .map(s -> s.getOwnRoomCount() != null ? s.getOwnRoomCount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayOcc = (dayOwnRooms.compareTo(BigDecimal.ZERO) > 0)
                    ? dayRooms.divide(dayOwnRooms, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            occupancyTrend.add(dayOcc);
        }

        // 未填报门店列表 - 补充店长姓名和电话
        Set<Long> filledStoreIds = summaries.stream().map(DailyReportSummary::getStoreId).collect(Collectors.toSet());
        // 批量查询店长信息
        Set<Long> managerIds = allStores.stream()
                .map(Store::getManagerUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> managerMap = new HashMap<>();
        if (!managerIds.isEmpty()) {
            List<SysUser> managers = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().in(SysUser::getId, managerIds)
            );
            for (SysUser u : managers) {
                managerMap.put(u.getId(), u);
            }
        }
        List<Map<String, Object>> unfilledStores = allStores.stream()
                .filter(s -> !filledStoreIds.contains(s.getId()))
                .map(s -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("storeName", s.getStoreName());
                    item.put("city", s.getCity());
                    String managerName = "";
                    String phone = "";
                    if (s.getManagerUserId() != null) {
                        SysUser manager = managerMap.get(s.getManagerUserId());
                        if (manager != null) {
                            managerName = manager.getRealName() != null ? manager.getRealName() : "";
                            phone = manager.getPhone() != null ? manager.getPhone() : "";
                        }
                    }
                    item.put("managerName", managerName);
                    item.put("phone", phone);
                    return item;
                })
                .collect(Collectors.toList());

        // 前端期望的排名格式
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

        // 返回结果 - 同时包含前端期望的字段名
        result.put("totalStores", totalStores);
        result.put("filledCount", filledCount);
        result.put("unfilledCount", unfilledCount);
        // 前端Dashboard.vue期望的字段名（兼容映射）
        result.put("shouldFill", totalStores);
        result.put("filled", filledCount);
        result.put("unfilled", unfilledCount);

        result.put("totalRevenue", totalRevenue);
        result.put("totalRooms", totalRooms);
        result.put("avgOccupancy", avgOccupancy.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
        result.put("avgAdr", avgADR);
        result.put("avgRevpar", avgRevPAR);
        // 前端期望的字段名（兼容映射）
        result.put("avgADR", avgADR);
        result.put("avgRevPAR", avgRevPAR);

        result.put("fillRate", fillRate);
        result.put("top10", top10);
        result.put("revenueRanking", revenueRanking);
        result.put("occupancyRanking", occupancyRanking);
        result.put("dateLabels", dateLabels);
        result.put("revenueTrend", revenueTrend);
        result.put("occupancyTrend", occupancyTrend);
        result.put("unfilledStores", unfilledStores);
        return result;
    }

    @Override
    public Map<String, Object> trendCompare(String date, String period, String metric, List<Long> storeIds) {
        Map<String, Object> result = new HashMap<>();

        if (date == null || date.isEmpty()) {
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (period == null || period.isEmpty()) {
            period = "day";
        }
        if (metric == null || metric.isEmpty()) {
            metric = "revenue";
        }

        // 获取基准日期的dim_date信息
        DimDate baseDim = dimDateMapper.selectById(date);
        if (baseDim == null) {
            result.put("labels", Collections.emptyList());
            result.put("current", Collections.emptyList());
            result.put("lastYear", Collections.emptyList());
            return result;
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> currentValues = new ArrayList<>();
        List<BigDecimal> lastYearValues = new ArrayList<>();

        // 填报率需要知道总门店数
        int totalActiveStores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1)
        ).size();
        // 门店多选时，只统计选中门店
        if (storeIds != null && !storeIds.isEmpty()) {
            totalActiveStores = storeIds.size();
        }

        switch (period) {
            case "week":
                buildWeekTrend(baseDim, storeIds, metric, labels, currentValues, lastYearValues, totalActiveStores);
                break;
            case "month":
                buildMonthTrend(baseDim, storeIds, metric, labels, currentValues, lastYearValues, totalActiveStores);
                break;
            case "day":
            default:
                buildDayTrend(date, storeIds, metric, labels, currentValues, lastYearValues, totalActiveStores);
                break;
        }

        result.put("period", period);
        result.put("metric", metric);
        result.put("labels", labels);
        result.put("current", currentValues);
        result.put("lastYear", lastYearValues);
        return result;
    }

    /**
     * 天趋势：基准日往前12天 + 去年同期
     */
    private void buildDayTrend(String date, List<Long> storeIds, String metric,
                               List<String> labels, List<BigDecimal> currentValues, List<BigDecimal> lastYearValues,
                               int totalActiveStores) {
        LocalDate baseDate = LocalDate.parse(date);
        String currentStart = baseDate.minusDays(11).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentEnd = date;
        String lastYearStart = baseDate.minusYears(1).minusDays(11).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String lastYearEnd = baseDate.minusYears(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 批量查询当期+去年同期
        Map<String, List<DailyReportSummary>> currentGrouped = queryAndGroup(currentStart, currentEnd, storeIds);
        Map<String, List<DailyReportSummary>> lastYearGrouped = queryAndGroup(lastYearStart, lastYearEnd, storeIds);

        for (int i = 11; i >= 0; i--) {
            LocalDate d = baseDate.minusDays(i);
            String ds = d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String label = d.getMonthValue() + "/" + d.getDayOfMonth();
            labels.add(label);
            currentValues.add(calcMetricOrFillRate(currentGrouped.getOrDefault(ds, Collections.emptyList()), metric, 1, totalActiveStores));

            LocalDate ld = d.minusYears(1);
            String lds = ld.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            lastYearValues.add(calcMetricOrFillRate(lastYearGrouped.getOrDefault(lds, Collections.emptyList()), metric, 1, totalActiveStores));
        }
    }

    /**
     * 周趋势：基准日所在周往前12周 + 去年同期（同ISO周号）
     */
    private void buildWeekTrend(DimDate baseDim, List<Long> storeIds, String metric,
                                List<String> labels, List<BigDecimal> currentValues, List<BigDecimal> lastYearValues,
                                int totalActiveStores) {
        // 获取基准日所在周的year_week
        String baseYearWeek = baseDim.getYearWeek();
        int baseIsoYear = Integer.parseInt(baseYearWeek.substring(0, 4));
        int baseIsoWeek = Integer.parseInt(baseYearWeek.substring(6));

        // 查出最近12周的year_week列表
        List<String> currentYearWeeks = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            int targetWeek = baseIsoWeek - i;
            int targetYear = baseIsoYear;
            while (targetWeek <= 0) {
                targetYear--;
                // 上一年最后一周通常是52或53
                targetWeek += getMaxIsoWeek(targetYear);
            }
            currentYearWeeks.add(String.format("%d-W%02d", targetYear, targetWeek));
        }

        // 去年同期：年份-1，周号相同
        List<String> lastYearYearWeeks = new ArrayList<>();
        for (String yw : currentYearWeeks) {
            int yr = Integer.parseInt(yw.substring(0, 4)) - 1;
            int wk = Integer.parseInt(yw.substring(6));
            lastYearYearWeeks.add(String.format("%d-W%02d", yr, wk));
        }

        // 批量查dim_date获取每个year_week对应的日期范围
        Set<String> allYearWeeks = new HashSet<>();
        allYearWeeks.addAll(currentYearWeeks);
        allYearWeeks.addAll(lastYearYearWeeks);

        List<DimDate> dimDates = dimDateMapper.selectList(
                new LambdaQueryWrapper<DimDate>().in(DimDate::getYearWeek, allYearWeeks)
        );
        Map<String, List<DimDate>> dimByYearWeek = dimDates.stream()
                .collect(Collectors.groupingBy(DimDate::getYearWeek));

        // 查daily_report_summary，按日期范围批量查询
        // 收集所有需要的日期
        Set<String> allDates = dimDates.stream().map(DimDate::getDateKey).collect(Collectors.toSet());
        Map<String, List<DailyReportSummary>> reportByDate = batchQueryReports(allDates, storeIds);

        // 按周汇总
        for (int i = 0; i < currentYearWeeks.size(); i++) {
            String yw = currentYearWeeks.get(i);
            String lyw = lastYearYearWeeks.get(i);

            // 标签：周起始日期
            List<DimDate> weekDims = dimByYearWeek.getOrDefault(yw, Collections.emptyList());
            String label = weekDims.isEmpty() ? yw :
                    weekDims.stream().map(DimDate::getDateKey).min(String::compareTo)
                            .map(d -> d.substring(5)).orElse(yw);
            labels.add(label);

            currentValues.add(calcWeeklyMetric(weekDims, reportByDate, metric, totalActiveStores));
            lastYearValues.add(calcWeeklyMetric(dimByYearWeek.getOrDefault(lyw, Collections.emptyList()), reportByDate, metric, totalActiveStores));
        }
    }

    /**
     * 月趋势：基准日所在月往前12月 + 去年同期
     */
    private void buildMonthTrend(DimDate baseDim, List<Long> storeIds, String metric,
                                 List<String> labels, List<BigDecimal> currentValues, List<BigDecimal> lastYearValues,
                                 int totalActiveStores) {
        int baseYear = baseDim.getTheYear();
        int baseMonth = baseDim.getTheMonth();

        List<String> currentYearMonths = new ArrayList<>();
        List<String> lastYearYearMonths = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            int m = baseMonth - i;
            int y = baseYear;
            while (m <= 0) {
                y--;
                m += 12;
            }
            currentYearMonths.add(String.format("%d-%02d", y, m));
            lastYearYearMonths.add(String.format("%d-%02d", y - 1, m));
        }

        // 查dim_date获取每个月的日期范围
        Set<String> allYearMonths = new HashSet<>();
        allYearMonths.addAll(currentYearMonths);
        allYearMonths.addAll(lastYearYearMonths);

        List<DimDate> dimDates = dimDateMapper.selectList(
                new LambdaQueryWrapper<DimDate>().in(DimDate::getYearMonth, allYearMonths)
        );
        Map<String, List<DimDate>> dimByYearMonth = dimDates.stream()
                .collect(Collectors.groupingBy(DimDate::getYearMonth));

        // 查daily_report_summary
        Set<String> allDates = dimDates.stream().map(DimDate::getDateKey).collect(Collectors.toSet());
        Map<String, List<DailyReportSummary>> reportByDate = batchQueryReports(allDates, storeIds);

        // 按月汇总
        for (int i = 0; i < currentYearMonths.size(); i++) {
            String ym = currentYearMonths.get(i);
            String lym = lastYearYearMonths.get(i);

            labels.add(ym);
            currentValues.add(calcPeriodMetric(dimByYearMonth.getOrDefault(ym, Collections.emptyList()), reportByDate, metric, totalActiveStores));
            lastYearValues.add(calcPeriodMetric(dimByYearMonth.getOrDefault(lym, Collections.emptyList()), reportByDate, metric, totalActiveStores));
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 批量查询日报数据并按日期分组
     */
    private Map<String, List<DailyReportSummary>> queryAndGroup(String startDate, String endDate, List<Long> storeIds) {
        LambdaQueryWrapper<DailyReportSummary> wrapper = new LambdaQueryWrapper<DailyReportSummary>()
                .ge(DailyReportSummary::getReportDate, startDate)
                .le(DailyReportSummary::getReportDate, endDate);
        if (storeIds != null && !storeIds.isEmpty()) {
            wrapper.in(DailyReportSummary::getStoreId, storeIds);
        }
        List<DailyReportSummary> summaries = summaryMapper.selectList(wrapper);
        return summaries.stream()
                .collect(Collectors.groupingBy(DailyReportSummary::getReportDate, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 按日期集合批量查询，返回按日期分组
     */
    private Map<String, List<DailyReportSummary>> batchQueryReports(Set<String> dates, List<Long> storeIds) {
        if (dates.isEmpty()) return new HashMap<>();
        LambdaQueryWrapper<DailyReportSummary> wrapper = new LambdaQueryWrapper<DailyReportSummary>()
                .in(DailyReportSummary::getReportDate, dates);
        if (storeIds != null && !storeIds.isEmpty()) {
            wrapper.in(DailyReportSummary::getStoreId, storeIds);
        }
        List<DailyReportSummary> summaries = summaryMapper.selectList(wrapper);
        return summaries.stream()
                .collect(Collectors.groupingBy(DailyReportSummary::getReportDate));
    }

    /**
     * 计算单日/单周期的指标值（加权平均）
     */
    private BigDecimal calcMetric(List<DailyReportSummary> summaries, String metric) {
        if (summaries == null || summaries.isEmpty()) return BigDecimal.ZERO;

        switch (metric) {
            case "revenue":
                return summaries.stream()
                        .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "occupancy":
                // 加权平均出租率 = 总间夜 / 总可用房量
                BigDecimal totalRooms = summaries.stream()
                        .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalOwnRooms = summaries.stream()
                        .map(s -> s.getOwnRoomCount() != null ? s.getOwnRoomCount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return (totalOwnRooms.compareTo(BigDecimal.ZERO) > 0)
                        ? totalRooms.divide(totalOwnRooms, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            case "adr":
                // 加权ADR = 总营收 / 总间夜
                BigDecimal rev = summaries.stream()
                        .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal rn = summaries.stream()
                        .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return (rn.compareTo(BigDecimal.ZERO) > 0)
                        ? rev.divide(rn, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            case "roomnights":
                return summaries.stream()
                        .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "revpar":
                // RevPAR = 加权出租率 * 加权ADR
                BigDecimal occ = calcMetric(summaries, "occupancy").divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                BigDecimal adr = calcMetric(summaries, "adr");
                return occ.multiply(adr).setScale(2, RoundingMode.HALF_UP);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * 计算周级别指标
     */
    private BigDecimal calcWeeklyMetric(List<DimDate> weekDims, Map<String, List<DailyReportSummary>> reportByDate,
                                        String metric, int totalActiveStores) {
        List<DailyReportSummary> weekSummaries = new ArrayList<>();
        int daysInPeriod = weekDims.size();
        for (DimDate dim : weekDims) {
            weekSummaries.addAll(reportByDate.getOrDefault(dim.getDateKey(), Collections.emptyList()));
        }
        return calcMetricOrFillRate(weekSummaries, metric, daysInPeriod, totalActiveStores);
    }

    /**
     * 计算月级别指标
     */
    private BigDecimal calcPeriodMetric(List<DimDate> periodDims, Map<String, List<DailyReportSummary>> reportByDate,
                                        String metric, int totalActiveStores) {
        List<DailyReportSummary> periodSummaries = new ArrayList<>();
        int daysInPeriod = periodDims.size();
        for (DimDate dim : periodDims) {
            periodSummaries.addAll(reportByDate.getOrDefault(dim.getDateKey(), Collections.emptyList()));
        }
        return calcMetricOrFillRate(periodSummaries, metric, daysInPeriod, totalActiveStores);
    }

    /**
     * 统一指标计算入口：普通指标走calcMetric，填报率走特殊逻辑
     * @param summaries 该日期/周期的日报汇总列表
     * @param metric 指标名
     * @param daysInPeriod 该周期天数
     * @param totalActiveStores 活跃门店数
     */
    private BigDecimal calcMetricOrFillRate(List<DailyReportSummary> summaries, String metric,
                                            int daysInPeriod, int totalActiveStores) {
        if ("fillrate".equals(metric)) {
            // 填报率 = 实际填报数 / 应填报数 * 100
            // 应填报数 = 活跃门店数 * 天数
            if (totalActiveStores <= 0 || daysInPeriod <= 0) return BigDecimal.ZERO;
            int shouldFill = totalActiveStores * daysInPeriod;
            // 去重：一天内一个门店可能有多条summary（不应出现，但防呆）
            long filledCount = summaries.size();
            return new BigDecimal(filledCount)
                    .divide(new BigDecimal(shouldFill), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }
        return calcMetric(summaries, metric);
    }

    /**
     * 获取指定年份的最大ISO周号（52或53）
     */
    private int getMaxIsoWeek(int year) {
        // 12月28日一定在该年最后一周
        LocalDate dec28 = LocalDate.of(year, 12, 28);
        return dec28.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
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
    public List<Map<String, Object>> trend(List<Long> storeIds, String startDate, String endDate, String metric, String period) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (period == null || period.isEmpty()) {
            period = "day";
        }

        LambdaQueryWrapper<DailyReportSummary> wrapper = new LambdaQueryWrapper<DailyReportSummary>()
                .ge(DailyReportSummary::getReportDate, startDate)
                .le(DailyReportSummary::getReportDate, endDate);
        if (storeIds != null && !storeIds.isEmpty()) {
            wrapper.in(DailyReportSummary::getStoreId, storeIds);
        }

        List<DailyReportSummary> summaries = summaryMapper.selectList(wrapper.orderByAsc(DailyReportSummary::getReportDate));

        if ("week".equals(period)) {
            // 按周汇总
            Map<String, List<DailyReportSummary>> weekGrouped = new LinkedHashMap<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (DailyReportSummary s : summaries) {
                LocalDate d = LocalDate.parse(s.getReportDate(), fmt);
                // ISO周：获取所在周的周一日期作为key
                LocalDate monday = d.minusDays(d.getDayOfWeek().getValue() - 1);
                String weekKey = monday.format(fmt);
                weekGrouped.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(s);
            }
            for (Map.Entry<String, List<DailyReportSummary>> entry : weekGrouped.entrySet()) {
                Map<String, Object> point = new HashMap<>();
                point.put("date", entry.getKey());
                List<DailyReportSummary> weekSummaries = entry.getValue();
                point.put("revenue", weekSummaries.stream()
                        .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                point.put("roomNights", weekSummaries.stream()
                        .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                point.put("count", weekSummaries.size());
                result.add(point);
            }
        } else if ("month".equals(period)) {
            // 按月汇总
            Map<String, List<DailyReportSummary>> monthGrouped = summaries.stream()
                    .collect(Collectors.groupingBy(
                            s -> s.getReportDate() != null && s.getReportDate().length() >= 7 ? s.getReportDate().substring(0, 7) : s.getReportDate(),
                            LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<DailyReportSummary>> entry : monthGrouped.entrySet()) {
                Map<String, Object> point = new HashMap<>();
                point.put("date", entry.getKey());
                List<DailyReportSummary> monthSummaries = entry.getValue();
                point.put("revenue", monthSummaries.stream()
                        .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                point.put("roomNights", monthSummaries.stream()
                        .map(s -> s.getRoomNights() != null ? s.getRoomNights() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                point.put("count", monthSummaries.size());
                result.add(point);
            }
        } else {
            // 按天汇总（默认）
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
                    // 加权平均出租率 = 总间夜 / 总可用房量
                    BigDecimal totalOwnRooms = daySummaries.stream()
                            .map(s -> s.getOwnRoomCount() != null ? s.getOwnRoomCount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    point.put("occupancyRate", totalOwnRooms.compareTo(BigDecimal.ZERO) > 0
                            ? dayRoomNights.divide(totalOwnRooms, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);
                    // 加权ADR = 总营收 / 总间夜
                    point.put("adr", dayRoomNights.compareTo(BigDecimal.ZERO) > 0
                            ? dayRevenue.divide(dayRoomNights, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);
                }

                point.put("count", daySummaries.size());
                result.add(point);
            }
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> channelAnalysis(List<Long> storeIds, String startDate, String endDate) {
        LambdaQueryWrapper<DailyReportChannel> wrapper = new LambdaQueryWrapper<DailyReportChannel>()
                .ge(DailyReportChannel::getReportDate, startDate)
                .le(DailyReportChannel::getReportDate, endDate);
        if (storeIds != null && !storeIds.isEmpty()) {
            wrapper.in(DailyReportChannel::getStoreId, storeIds);
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
    public List<Map<String, Object>> storeRanking(String startDate, String endDate, String metric) {
        // 评分排名走独立SQL查询（数据在EAV表）
        if ("score".equals(metric)) {
            List<Map<String, Object>> rawList = valueMapper.selectScoreRanking(startDate, endDate);
            // 添加排名
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < rawList.size(); i++) {
                Map<String, Object> item = rawList.get(i);
                item.put("rank", i + 1);
                // 确保value是BigDecimal
                Object val = item.get("value");
                if (val instanceof Number) {
                    item.put("value", new BigDecimal(val.toString()));
                }
                result.add(item);
            }
            return result;
        }

        // 查询日期范围内的日报数据
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<DailyReport>()
                .ge(DailyReport::getReportDate, startDate)
                .le(DailyReport::getReportDate, endDate);

        List<DailyReport> reports = reportMapper.selectList(wrapper);

        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1));
        Map<Long, String> storeNameMap = stores.stream()
                .collect(Collectors.toMap(Store::getId, Store::getStoreName, (a, b) -> a));

        // 按门店分组汇总
        Map<Long, List<DailyReport>> groupedByStore = reports.stream()
                .filter(r -> r.getStoreId() != null)
                .collect(Collectors.groupingBy(DailyReport::getStoreId));

        List<Map<String, Object>> result = groupedByStore.entrySet().stream()
                .map(entry -> {
                    Long sid = entry.getKey();
                    List<DailyReport> storeReports = entry.getValue();
                    Map<String, Object> item = new HashMap<>();
                    item.put("storeId", sid);
                    item.put("storeName", storeNameMap.getOrDefault(sid, storeReports.get(0).getStoreName() != null ? storeReports.get(0).getStoreName() : ""));

                    // 汇总计算指标值
                    BigDecimal value = BigDecimal.ZERO;
                    BigDecimal totalRevenue = storeReports.stream()
                            .map(r -> r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalRoomNights = storeReports.stream()
                            .map(r -> r.getRoomNights() != null ? r.getRoomNights() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int totalOwnRooms = storeReports.stream()
                            .mapToInt(r -> r.getOwnRoomCount() != null ? r.getOwnRoomCount() : 0)
                            .sum();

                    if ("revenue".equals(metric)) {
                        value = totalRevenue;
                    } else if ("occupancyRate".equals(metric) || "occupancy".equals(metric)) {
                        // 加权出租率 = 总间夜 / 总可用房量
                        value = totalOwnRooms > 0
                                ? totalRoomNights.divide(new BigDecimal(totalOwnRooms), 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                    } else if ("adr".equals(metric)) {
                        // 加权ADR = 总营收 / 总间夜
                        value = totalRoomNights.compareTo(BigDecimal.ZERO) > 0
                                ? totalRevenue.divide(totalRoomNights, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                    } else if ("revpar".equals(metric)) {
                        // 加权RevPAR = 加权出租率 * 加权ADR
                        BigDecimal occ = totalOwnRooms > 0
                                ? totalRoomNights.divide(new BigDecimal(totalOwnRooms), 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        BigDecimal adr = totalRoomNights.compareTo(BigDecimal.ZERO) > 0
                                ? totalRevenue.divide(totalRoomNights, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        value = occ.multiply(adr).setScale(2, RoundingMode.HALF_UP);
                    } else if ("rooms".equals(metric)) {
                        value = totalRoomNights;
                    } else {
                        value = totalRevenue;
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

    @Override
    public List<Map<String, Object>> monthlyDetail(String month, List<Long> storeIds) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 查询该月所有日报数据
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getReportMonth, month);
        if (storeIds != null && !storeIds.isEmpty()) {
            wrapper.in(DailyReport::getStoreId, storeIds);
        }
        List<DailyReport> reports = reportMapper.selectList(wrapper);

        // 获取门店名称
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1));
        Map<Long, String> storeNameMap = stores.stream()
                .collect(Collectors.toMap(Store::getId, Store::getStoreName, (a, b) -> a));

        // 按门店分组
        Map<Long, List<DailyReport>> groupedByStore = reports.stream()
                .filter(r -> r.getStoreId() != null)
                .collect(Collectors.groupingBy(DailyReport::getStoreId, LinkedHashMap::new, Collectors.toList()));

        // 汇总变量（用于合计行）
        BigDecimal grandTotalRevenue = BigDecimal.ZERO;
        BigDecimal grandTotalRoomNights = BigDecimal.ZERO;
        int grandTotalOwnRooms = 0;
        BigDecimal grandTotalDailyRoomFee = BigDecimal.ZERO;
        BigDecimal grandTotalHourlyRoomFee = BigDecimal.ZERO;
        BigDecimal grandTotalOtherFee = BigDecimal.ZERO;
        BigDecimal grandTotalDepositAmount = BigDecimal.ZERO;
        BigDecimal grandTotalWalkinRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalCtripRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalLyRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalQunarRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalZhixingRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalExternalRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalMeituanHotelRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalFliggyRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalDouyinRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalXiaozhuRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalTujiaRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalMeituanHomestayRoomNights = BigDecimal.ZERO;
        BigDecimal grandTotalJialiRoomNights = BigDecimal.ZERO;

        for (Map.Entry<Long, List<DailyReport>> entry : groupedByStore.entrySet()) {
            Long sid = entry.getKey();
            List<DailyReport> storeReports = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("storeId", sid);
            item.put("storeName", storeNameMap.getOrDefault(sid, storeReports.get(0).getStoreName() != null ? storeReports.get(0).getStoreName() : ""));
            item.put("reportCount", storeReports.size());

            // 汇总字段
            BigDecimal totalRevenue = storeReports.stream()
                    .map(r -> r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalRoomNights = storeReports.stream()
                    .map(r -> r.getRoomNights() != null ? r.getRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int totalOwnRooms = storeReports.stream()
                    .mapToInt(r -> r.getOwnRoomCount() != null ? r.getOwnRoomCount() : 0)
                    .sum();
            BigDecimal totalDailyRoomFee = storeReports.stream()
                    .map(r -> r.getDailyRoomFee() != null ? r.getDailyRoomFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalHourlyRoomFee = storeReports.stream()
                    .map(r -> r.getHourlyRoomFee() != null ? r.getHourlyRoomFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalOtherFee = storeReports.stream()
                    .map(r -> r.getOtherFee() != null ? r.getOtherFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalDepositAmount = storeReports.stream()
                    .map(r -> r.getDepositAmount() != null ? r.getDepositAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 渠道间夜汇总
            BigDecimal totalWalkinRoomNights = storeReports.stream()
                    .map(r -> r.getWalkinRoomNights() != null ? r.getWalkinRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCtripRoomNights = storeReports.stream()
                    .map(r -> r.getCtripRoomNights() != null ? r.getCtripRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalLyRoomNights = storeReports.stream()
                    .map(r -> r.getLyRoomNights() != null ? r.getLyRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalQunarRoomNights = storeReports.stream()
                    .map(r -> r.getQunarRoomNights() != null ? r.getQunarRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalZhixingRoomNights = storeReports.stream()
                    .map(r -> r.getZhixingRoomNights() != null ? r.getZhixingRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExternalRoomNights = storeReports.stream()
                    .map(r -> r.getExternalRoomNights() != null ? r.getExternalRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalMeituanHotelRoomNights = storeReports.stream()
                    .map(r -> r.getMeituanHotelRoomNights() != null ? r.getMeituanHotelRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalFliggyRoomNights = storeReports.stream()
                    .map(r -> r.getFliggyRoomNights() != null ? r.getFliggyRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalDouyinRoomNights = storeReports.stream()
                    .map(r -> r.getDouyinRoomNights() != null ? r.getDouyinRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalXiaozhuRoomNights = storeReports.stream()
                    .map(r -> r.getXiaozhuRoomNights() != null ? r.getXiaozhuRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalTujiaRoomNights = storeReports.stream()
                    .map(r -> r.getTujiaRoomNights() != null ? r.getTujiaRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalMeituanHomestayRoomNights = storeReports.stream()
                    .map(r -> r.getMeituanHomestayRoomNights() != null ? r.getMeituanHomestayRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalJialiRoomNights = storeReports.stream()
                    .map(r -> r.getJialiRoomNights() != null ? r.getJialiRoomNights() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 累加到总计
            grandTotalRevenue = grandTotalRevenue.add(totalRevenue);
            grandTotalRoomNights = grandTotalRoomNights.add(totalRoomNights);
            grandTotalOwnRooms += totalOwnRooms;
            grandTotalDailyRoomFee = grandTotalDailyRoomFee.add(totalDailyRoomFee);
            grandTotalHourlyRoomFee = grandTotalHourlyRoomFee.add(totalHourlyRoomFee);
            grandTotalOtherFee = grandTotalOtherFee.add(totalOtherFee);
            grandTotalDepositAmount = grandTotalDepositAmount.add(totalDepositAmount);
            grandTotalWalkinRoomNights = grandTotalWalkinRoomNights.add(totalWalkinRoomNights);
            grandTotalCtripRoomNights = grandTotalCtripRoomNights.add(totalCtripRoomNights);
            grandTotalLyRoomNights = grandTotalLyRoomNights.add(totalLyRoomNights);
            grandTotalQunarRoomNights = grandTotalQunarRoomNights.add(totalQunarRoomNights);
            grandTotalZhixingRoomNights = grandTotalZhixingRoomNights.add(totalZhixingRoomNights);
            grandTotalExternalRoomNights = grandTotalExternalRoomNights.add(totalExternalRoomNights);
            grandTotalMeituanHotelRoomNights = grandTotalMeituanHotelRoomNights.add(totalMeituanHotelRoomNights);
            grandTotalFliggyRoomNights = grandTotalFliggyRoomNights.add(totalFliggyRoomNights);
            grandTotalDouyinRoomNights = grandTotalDouyinRoomNights.add(totalDouyinRoomNights);
            grandTotalXiaozhuRoomNights = grandTotalXiaozhuRoomNights.add(totalXiaozhuRoomNights);
            grandTotalTujiaRoomNights = grandTotalTujiaRoomNights.add(totalTujiaRoomNights);
            grandTotalMeituanHomestayRoomNights = grandTotalMeituanHomestayRoomNights.add(totalMeituanHomestayRoomNights);
            grandTotalJialiRoomNights = grandTotalJialiRoomNights.add(totalJialiRoomNights);

            // 加权计算经营指标
            // 加权出租率 = 总间夜 / 总可用房量
            BigDecimal occupancyRate = totalOwnRooms > 0
                    ? totalRoomNights.divide(new BigDecimal(totalOwnRooms), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            // 加权ADR = 总营收 / 总间夜
            BigDecimal adr = totalRoomNights.compareTo(BigDecimal.ZERO) > 0
                    ? totalRevenue.divide(totalRoomNights, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            // 加权RevPAR = 加权出租率 * 加权ADR
            BigDecimal revpar = occupancyRate.multiply(adr).setScale(2, RoundingMode.HALF_UP);

            // 设置字段值
            item.put("ownRoomCount", totalOwnRooms);
            item.put("roomNights", totalRoomNights);
            item.put("walkinRoomNights", totalWalkinRoomNights);
            item.put("ctripRoomNights", totalCtripRoomNights);
            item.put("lyRoomNights", totalLyRoomNights);
            item.put("qunarRoomNights", totalQunarRoomNights);
            item.put("zhixingRoomNights", totalZhixingRoomNights);
            item.put("externalRoomNights", totalExternalRoomNights);
            item.put("meituanHotelRoomNights", totalMeituanHotelRoomNights);
            item.put("fliggyRoomNights", totalFliggyRoomNights);
            item.put("douyinRoomNights", totalDouyinRoomNights);
            item.put("xiaozhuRoomNights", totalXiaozhuRoomNights);
            item.put("tujiaRoomNights", totalTujiaRoomNights);
            item.put("meituanHomestayRoomNights", totalMeituanHomestayRoomNights);
            item.put("jialiRoomNights", totalJialiRoomNights);
            item.put("occupancyRate", occupancyRate);
            item.put("adr", adr);
            item.put("revpar", revpar);
            item.put("dailyRoomFee", totalDailyRoomFee);
            item.put("hourlyRoomFee", totalHourlyRoomFee);
            item.put("otherFee", totalOtherFee);
            item.put("totalRevenue", totalRevenue);
            item.put("depositAmount", totalDepositAmount);

            result.add(item);
        }

        // 添加合计行
        BigDecimal grandOccupancyRate = grandTotalOwnRooms > 0
                ? grandTotalRoomNights.divide(new BigDecimal(grandTotalOwnRooms), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal grandAdr = grandTotalRoomNights.compareTo(BigDecimal.ZERO) > 0
                ? grandTotalRevenue.divide(grandTotalRoomNights, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal grandRevpar = grandOccupancyRate.multiply(grandAdr).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> totalRow = new HashMap<>();
        totalRow.put("storeId", null);
        totalRow.put("storeName", "合计");
        totalRow.put("reportCount", reports.size());
        totalRow.put("ownRoomCount", grandTotalOwnRooms);
        totalRow.put("roomNights", grandTotalRoomNights);
        totalRow.put("walkinRoomNights", grandTotalWalkinRoomNights);
        totalRow.put("ctripRoomNights", grandTotalCtripRoomNights);
        totalRow.put("lyRoomNights", grandTotalLyRoomNights);
        totalRow.put("qunarRoomNights", grandTotalQunarRoomNights);
        totalRow.put("zhixingRoomNights", grandTotalZhixingRoomNights);
        totalRow.put("externalRoomNights", grandTotalExternalRoomNights);
        totalRow.put("meituanHotelRoomNights", grandTotalMeituanHotelRoomNights);
        totalRow.put("fliggyRoomNights", grandTotalFliggyRoomNights);
        totalRow.put("douyinRoomNights", grandTotalDouyinRoomNights);
        totalRow.put("xiaozhuRoomNights", grandTotalXiaozhuRoomNights);
        totalRow.put("tujiaRoomNights", grandTotalTujiaRoomNights);
        totalRow.put("meituanHomestayRoomNights", grandTotalMeituanHomestayRoomNights);
        totalRow.put("jialiRoomNights", grandTotalJialiRoomNights);
        totalRow.put("occupancyRate", grandOccupancyRate);
        totalRow.put("adr", grandAdr);
        totalRow.put("revpar", grandRevpar);
        totalRow.put("dailyRoomFee", grandTotalDailyRoomFee);
        totalRow.put("hourlyRoomFee", grandTotalHourlyRoomFee);
        totalRow.put("otherFee", grandTotalOtherFee);
        totalRow.put("totalRevenue", grandTotalRevenue);
        totalRow.put("depositAmount", grandTotalDepositAmount);

        result.add(totalRow);

        return result;
    }
}
