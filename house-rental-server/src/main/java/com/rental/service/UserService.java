package com.rental.service;

import com.rental.dto.LoginDTO;
import com.rental.dto.RegisterDTO;
import com.rental.entity.SysUser;

import java.util.List;

public interface UserService {

    void register(RegisterDTO dto);

    SysUser login(LoginDTO dto);

    SysUser getById(Long id);

    List<SysUser> listAll();

    void updateStatus(Long id, Integer status);
}
