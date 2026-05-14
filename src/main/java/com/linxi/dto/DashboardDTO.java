package com.linxi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private BigDecimal fillRate;
    private Integer totalStores;
    private Integer filledCount;
    private BigDecimal totalRevenue;
    private BigDecimal avgOccupancyRate;
    private BigDecimal avgAdr;
    private BigDecimal avgRevpar;
    private List<StoreRankingItem> top10;
    private List<TrendItem> trend7Days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreRankingItem {
        private Long storeId;
        private String storeName;
        private BigDecimal value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private BigDecimal revenue;
        private BigDecimal occupancyRate;
    }
}
