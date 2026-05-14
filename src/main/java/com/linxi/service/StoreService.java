package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.dto.StoreQueryDTO;
import com.linxi.entity.Store;

import java.util.List;

public interface StoreService {

    Store getById(Long id);

    Page<Store> page(StoreQueryDTO query);

    List<Store> listAll();

    boolean save(Store store);

    boolean update(Store store);

    boolean delete(Long id);
}
