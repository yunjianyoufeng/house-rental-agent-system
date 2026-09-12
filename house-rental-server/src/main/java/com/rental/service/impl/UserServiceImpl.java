package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.LoginDTO;
import com.rental.dto.RegisterDTO;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.SysUserMapper;
import com.rental.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;

    public UserServiceImpl(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Override
    public void register(RegisterDTO dto) {
        // BCrypt 的输入上限按 UTF-8 字节计算，不能只依赖字符数校验。
        if (dto.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("密码的UTF-8编码不能超过72字节");
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, dto.getUsername());

        SysUser existUser = sysUserMapper.selectOne(wrapper);
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleCode("TENANT");
        user.setStatus(1);

        int rows = sysUserMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException("注册失败");
        }
    }

    @Override
    public SysUser login(LoginDTO dto) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, dto.getUsername());

        SysUser user = sysUserMapper.selectOne(wrapper);
        String storedPassword = user == null ? null : user.getPassword();
        boolean bcrypt = storedPassword != null && storedPassword.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
        // 不存在的用户也计算 BCrypt，并统一错误提示，避免直接枚举账号。
        boolean passwordMatched = passwordEncoder.matches(dto.getPassword(), bcrypt ? storedPassword : dummyPasswordHash);
        if (demoEnabled && !bcrypt && storedPassword != null) {
            passwordMatched = dto.getPassword().equals(storedPassword);
        }
        if (user == null || !passwordMatched || !Integer.valueOf(1).equals(user.getStatus())
                || (!demoEnabled && "123456".equals(dto.getPassword()))) {
            throw new BusinessException("用户名或密码错误，或账号不可用");
        }
        if (!bcrypt) {
            // 仅显式开启的本地演示兼容旧数据，匹配成功后立即转为 BCrypt。
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            sysUserMapper.updateById(user);
        }

        return user;
    }

    @Override
    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    @Override
    public List<SysUser> listAll() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysUser::getRoleCode).orderByDesc(SysUser::getId);
        return sysUserMapper.selectList(wrapper);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("用户状态只允许设置为启用或禁用");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        if (sysUserMapper.updateById(user) <= 0) {
            throw new BusinessException("用户状态更新失败");
        }
    }
}
