package com.linxi.service;

import org.springframework.web.multipart.MultipartFile;

public interface DataImportService {

    /**
     * 导入Excel数据
     */
    int importExcel(MultipartFile file) throws Exception;
}
