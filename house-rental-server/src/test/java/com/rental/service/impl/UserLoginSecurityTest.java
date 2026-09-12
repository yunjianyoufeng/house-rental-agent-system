package com.rental.service.impl;

import com.rental.dto.LoginDTO;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserLoginSecurityTest {
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserServiceImpl service = new UserServiceImpl(users, encoder);

    @Test
    void missingWrongDisabledAndDefaultCredentialsHaveSameError() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("someone");
        dto.setPassword("123456");
        String expected = assertThrows(BusinessException.class, () -> service.login(dto)).getMessage();
        for (String stored : new String[]{"123456", encoder.encode("123456"), encoder.encode("a-different-password")}) {
            SysUser user = new SysUser();
            user.setPassword(stored);
            user.setStatus(1);
            when(users.selectOne(any())).thenReturn(user);
            assertEquals(expected, assertThrows(BusinessException.class, () -> service.login(dto)).getMessage());
        }
        verify(users, never()).updateById(any(SysUser.class));
    }

    @Test
    void changedPasswordOnExistingAdminStillWorks() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setRoleCode("ADMIN");
        user.setStatus(1);
        user.setPassword(encoder.encode("independent-secret-password"));
        when(users.selectOne(any())).thenReturn(user);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("independent-secret-password");
        assertSame(user, service.login(dto));
    }
}
