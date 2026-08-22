package com.rental.service;

import com.rental.dto.ComplaintAddDTO;
import com.rental.dto.ComplaintProcessDTO;
import com.rental.entity.Complaint;

import java.util.List;

public interface ComplaintService {

    void add(ComplaintAddDTO dto);

    List<Complaint> userList(Long userId);

    List<Complaint> adminList();

    void process(Long id, ComplaintProcessDTO dto);
}