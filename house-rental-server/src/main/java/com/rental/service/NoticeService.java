package com.rental.service;

import com.rental.dto.NoticeAddDTO;
import com.rental.entity.Notice;

import java.util.List;

public interface NoticeService {

    void add(NoticeAddDTO dto);

    void update(Long id, NoticeAddDTO dto);

    List<Notice> list();

    Notice detail(Long id);

    void delete(Long id);
}
