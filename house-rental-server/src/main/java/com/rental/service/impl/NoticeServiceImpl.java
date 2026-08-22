package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.NoticeAddDTO;
import com.rental.entity.Notice;
import com.rental.exception.BusinessException;
import com.rental.mapper.NoticeMapper;
import com.rental.service.NoticeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @Override
    public void add(NoticeAddDTO dto) {
        Notice notice = new Notice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setStatus(1);

        int rows = noticeMapper.insert(notice);
        if (rows <= 0) {
            throw new BusinessException("公告发布失败");
        }
    }

    @Override
    public void update(Long id, NoticeAddDTO dto) {
        Notice notice = detail(id);
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        int rows = noticeMapper.updateById(notice);
        if (rows <= 0) {
            throw new BusinessException("公告更新失败");
        }
    }

    @Override
    public List<Notice> list() {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getId);
        return noticeMapper.selectList(wrapper);
    }

    @Override
    public Notice detail(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        return notice;
    }

    @Override
    public void delete(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        noticeMapper.deleteById(id);
    }
}
