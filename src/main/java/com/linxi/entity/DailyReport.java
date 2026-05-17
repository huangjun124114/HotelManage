package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("daily_report")
public class DailyReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;
    private String storeName;
    private String storeCode;
    private LocalDate reportDate;
    private String reportMonth;
    private String weekDay;
    private Long templateId;

    // 房量字段
    private Integer ownRoomCount;
    private Integer hourlyRoomCount;
    private Integer repairRoomCount;

    // 间夜数字段
    private BigDecimal roomNights;
    private BigDecimal walkinRoomNights;
    private BigDecimal ctripRoomNights;
    private BigDecimal lyRoomNights;
    private BigDecimal qunarRoomNights;
    private BigDecimal zhixingRoomNights;
    private BigDecimal externalRoomNights;
    private BigDecimal meituanHotelRoomNights;
    private BigDecimal fliggyRoomNights;
    private BigDecimal douyinRoomNights;
    private BigDecimal xiaozhuRoomNights;
    private BigDecimal tujiaRoomNights;
    private BigDecimal meituanHomestayRoomNights;
    private BigDecimal jialiRoomNights;

    // 经营指标（计算字段）
    private BigDecimal occupancyRate;
    private BigDecimal adr;
    private BigDecimal revpar;

    // 收入字段
    private BigDecimal dailyRoomFee;
    private BigDecimal hourlyRoomFee;
    private BigDecimal otherFee;
    private BigDecimal totalRevenue;
    private BigDecimal depositAmount;

    // 扫码字段
    private Integer ctripScanCount;
    private Integer meituanScanCount;

    // 预留扩展字段（10个）
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;
    private String ext6;
    private String ext7;
    private String ext8;
    private String ext9;
    private String ext10;

    // 元数据字段
    private Integer status;
    private Integer abnormalFlag;
    private String abnormalMessage;
    private Long submitUserId;
    private String submitTime;
    private Long lockUserId;
    private String lockTime;
    private String rejectReason;
    private String remark;
    @TableLogic
    private Integer deleted;
    private String createTime;
    private Long createBy;
    private String updateTime;
    private Long updateBy;
}
