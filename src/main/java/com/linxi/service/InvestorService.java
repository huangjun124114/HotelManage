package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.dto.InvestorQueryDTO;
import com.linxi.entity.Investor;

import java.util.List;

public interface InvestorService {

    Investor getById(Long id);

    Page<Investor> page(InvestorQueryDTO query);

    List<Investor> listAll();

    boolean save(Investor investor);

    boolean update(Investor investor);

    boolean delete(Long id);
}
