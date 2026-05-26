package com.linxi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linxi.entity.DailyExtension;
import com.linxi.mapper.DailyExtensionMapper;
import com.linxi.service.DailyExtensionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class DailyExtensionServiceImpl implements DailyExtensionService {

    @Autowired
    private DailyExtensionMapper dailyExtensionMapper;

    @Override
    public void saveOrUpdate(Long reportId, Long storeId, String reportDate, Map<String, Object> values) {
        if (reportId == null) {
            return;
        }
        DailyExtension extension = getByReportId(reportId);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (extension == null) {
            extension = new DailyExtension();
            extension.setDailyReportId(reportId);
            extension.setStoreId(storeId);
            extension.setReportDate(java.time.LocalDate.parse(reportDate));  // 设置reportDate
            extension.setCreateTime(now);
        }

        // 安全处理values为null的情况
        if (values != null) {
            // 设置30个弹性域字段
            for (int i = 1; i <= 30; i++) {
                String fieldCode = "ext_field_" + i;
                Object val = values.get(fieldCode);
                if (val != null) {
                    switch (i) {
                        case 1: extension.setExtField1(val.toString()); break;
                        case 2: extension.setExtField2(val.toString()); break;
                        case 3: extension.setExtField3(val.toString()); break;
                        case 4: extension.setExtField4(val.toString()); break;
                        case 5: extension.setExtField5(val.toString()); break;
                        case 6: extension.setExtField6(val.toString()); break;
                        case 7: extension.setExtField7(val.toString()); break;
                        case 8: extension.setExtField8(val.toString()); break;
                        case 9: extension.setExtField9(val.toString()); break;
                        case 10: extension.setExtField10(val.toString()); break;
                        case 11: extension.setExtField11(val.toString()); break;
                        case 12: extension.setExtField12(val.toString()); break;
                        case 13: extension.setExtField13(val.toString()); break;
                        case 14: extension.setExtField14(val.toString()); break;
                        case 15: extension.setExtField15(val.toString()); break;
                        case 16: extension.setExtField16(val.toString()); break;
                        case 17: extension.setExtField17(val.toString()); break;
                        case 18: extension.setExtField18(val.toString()); break;
                        case 19: extension.setExtField19(val.toString()); break;
                        case 20: extension.setExtField20(val.toString()); break;
                        case 21: extension.setExtField21(val.toString()); break;
                        case 22: extension.setExtField22(val.toString()); break;
                        case 23: extension.setExtField23(val.toString()); break;
                        case 24: extension.setExtField24(val.toString()); break;
                        case 25: extension.setExtField25(val.toString()); break;
                        case 26: extension.setExtField26(val.toString()); break;
                        case 27: extension.setExtField27(val.toString()); break;
                        case 28: extension.setExtField28(val.toString()); break;
                        case 29: extension.setExtField29(val.toString()); break;
                        case 30: extension.setExtField30(val.toString()); break;
                    }
                }
            }
        }

        extension.setUpdateTime(now);

        if (extension.getId() == null) {
            dailyExtensionMapper.insert(extension);
        } else {
            dailyExtensionMapper.updateById(extension);
        }
    }

    @Override
    public DailyExtension getByReportId(Long reportId) {
        return dailyExtensionMapper.selectOne(
                new LambdaQueryWrapper<DailyExtension>()
                        .eq(DailyExtension::getDailyReportId, reportId)
        );
    }

}
