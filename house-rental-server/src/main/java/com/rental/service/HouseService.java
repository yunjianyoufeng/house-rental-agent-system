package com.rental.service;

import com.rental.dto.HouseAddDTO;
import com.rental.dto.HouseSearchDTO;
import com.rental.dto.HouseUpdateDTO;
import com.rental.entity.House;

import java.util.List;

public interface HouseService {

    void add(HouseAddDTO dto);

    void update(Long id, Long publisherId, HouseUpdateDTO dto);

    void updateStatus(Long id, Long publisherId, Integer status);

    List<House> list();

    List<House> search(HouseSearchDTO dto);

    House detail(Long id);

    House publicDetail(Long id);

    House publisherDetail(Long id, Long publisherId);

    House adminDetail(Long id);

    List<House> auditList();

    void auditPass(Long id);

    void auditReject(Long id);

    List<House> myList(Long publisherId);

    void delete(Long id);
}
