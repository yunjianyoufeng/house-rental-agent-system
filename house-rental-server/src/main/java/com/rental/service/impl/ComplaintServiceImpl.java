package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.ComplaintAddDTO;
import com.rental.dto.ComplaintProcessDTO;
import com.rental.entity.Complaint;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.ComplaintMapper;
import com.rental.mapper.SysUserMapper;
import com.rental.service.ComplaintService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintMapper complaintMapper;
    private final SysUserMapper sysUserMapper;

    public ComplaintServiceImpl(ComplaintMapper complaintMapper,
                                SysUserMapper sysUserMapper) {
        this.complaintMapper = complaintMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void add(ComplaintAddDTO dto) {
        Complaint complaint = new Complaint();
        complaint.setUserId(dto.getUserId());
        complaint.setTargetId(dto.getTargetId());
        complaint.setContent(dto.getContent());
        complaint.setStatus(0);

        int rows = complaintMapper.insert(complaint);
        if (rows <= 0) {
            throw new BusinessException("投诉提交失败");
        }
    }

    @Override
    public List<Complaint> userList(Long userId) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Complaint::getUserId, userId)
                .orderByDesc(Complaint::getId);
        List<Complaint> list = complaintMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public List<Complaint> adminList() {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Complaint::getId);
        List<Complaint> list = complaintMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public void process(Long id, ComplaintProcessDTO dto) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new BusinessException("投诉记录不存在");
        }
        if (dto.getStatus() == null || dto.getStatus() != 1) {
            throw new BusinessException("投诉状态只允许更新为已处理");
        }
        complaint.setStatus(dto.getStatus());
        complaint.setResult(dto.getResult());
        complaintMapper.updateById(complaint);
    }

    private void fillDisplayFields(List<Complaint> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Complaint complaint : list) {
            complaint.setUserName(userDisplayName(complaint.getUserId()));
            complaint.setTargetName(userDisplayName(complaint.getTargetId()));
        }
    }

    private String userDisplayName(Long userId) {
        if (userId == null) {
            return "--";
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return "用户" + userId;
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "用户" + userId;
    }
}
