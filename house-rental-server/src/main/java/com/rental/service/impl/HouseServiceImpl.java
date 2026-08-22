package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.HouseAddDTO;
import com.rental.dto.HouseSearchDTO;
import com.rental.dto.HouseUpdateDTO;
import com.rental.entity.House;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.SysUserMapper;
import com.rental.service.HouseService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class HouseServiceImpl implements HouseService {

    private final HouseMapper houseMapper;
    private final SysUserMapper sysUserMapper;

    public HouseServiceImpl(HouseMapper houseMapper,
                            SysUserMapper sysUserMapper) {
        this.houseMapper = houseMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void add(HouseAddDTO dto) {
        if (dto.getPublisherId() == null) {
            throw new BusinessException("未获取到当前发布者信息");
        }

        House house = new House();
        applyForm(house, dto.getTitle(), dto.getAddress(), dto.getCity(), dto.getArea(),
                dto.getRentPrice(), dto.getDeposit(), dto.getHouseType(), dto.getSquare(), dto.getFloor(),
                dto.getDescription(), dto.getImageUrls(), dto.getLongitude(), dto.getLatitude());
        house.setStatus(0);
        house.setAuditStatus(0);
        house.setPublisherId(dto.getPublisherId());

        int rows = houseMapper.insert(house);
        if (rows <= 0) {
            throw new BusinessException("房源发布失败");
        }
    }

    @Override
    public void update(Long id, Long publisherId, HouseUpdateDTO dto) {
        House house = detail(id);
        if (!publisherId.equals(house.getPublisherId())) {
            throw new BusinessException("只能编辑自己的房源");
        }
        if (house.getStatus() != null && house.getStatus() == 3) {
            throw new BusinessException("已出租房源不支持编辑");
        }

        applyForm(house, dto.getTitle(), dto.getAddress(), dto.getCity(), dto.getArea(),
                dto.getRentPrice(), dto.getDeposit(), dto.getHouseType(), dto.getSquare(), dto.getFloor(),
                dto.getDescription(), dto.getImageUrls(), dto.getLongitude(), dto.getLatitude());

        house.setAuditStatus(0);
        house.setStatus(0);

        int rows = houseMapper.updateById(house);
        if (rows <= 0) {
            throw new BusinessException("房源更新失败");
        }
    }

    @Override
    public void updateStatus(Long id, Long publisherId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("房源状态只允许设置为上架或下架");
        }

        House house = detail(id);
        if (!publisherId.equals(house.getPublisherId())) {
            throw new BusinessException("只能操作自己的房源");
        }
        if (house.getStatus() != null && house.getStatus() == 3) {
            throw new BusinessException("已出租房源不支持上下架");
        }
        if (status == 1 && !Integer.valueOf(1).equals(house.getAuditStatus())) {
            throw new BusinessException("房源审核通过后才能上架");
        }

        house.setStatus(status);
        int rows = houseMapper.updateById(house);
        if (rows <= 0) {
            throw new BusinessException("房源状态更新失败");
        }
    }

    @Override
    public List<House> list() {
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getStatus, 1)
                .eq(House::getAuditStatus, 1)
                .orderByDesc(House::getId);
        List<House> list = houseMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public List<House> search(HouseSearchDTO dto) {
        if (dto.getMinRent() != null && dto.getMaxRent() != null
                && dto.getMinRent().compareTo(dto.getMaxRent()) > 0) {
            throw new BusinessException("最低租金不能高于最高租金");
        }

        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getStatus, 1)
                .eq(House::getAuditStatus, 1)
                .like(StringUtils.hasText(dto.getCity()), House::getCity, dto.getCity())
                .like(StringUtils.hasText(dto.getArea()), House::getArea, dto.getArea())
                .ge(dto.getMinRent() != null, House::getRentPrice, dto.getMinRent())
                .le(dto.getMaxRent() != null, House::getRentPrice, dto.getMaxRent())
                .like(StringUtils.hasText(dto.getHouseType()), House::getHouseType, dto.getHouseType())
                .and(StringUtils.hasText(dto.getKeyword()), nested -> nested
                        .like(House::getTitle, dto.getKeyword())
                        .or()
                        .like(House::getAddress, dto.getKeyword())
                        .or()
                        .like(House::getDescription, dto.getKeyword()))
                .orderByAsc(House::getRentPrice)
                .orderByDesc(House::getId)
                .last("LIMIT " + dto.getLimit());

        List<House> list = houseMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public House detail(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        fillDisplayFields(house);
        return house;
    }

    @Override
    public House publicDetail(Long id) {
        House house = detail(id);
        if (!Integer.valueOf(1).equals(house.getAuditStatus()) || !Integer.valueOf(1).equals(house.getStatus())) {
            throw new BusinessException("该房源当前不可查看");
        }
        return house;
    }

    @Override
    public House publisherDetail(Long id, Long publisherId) {
        House house = detail(id);
        if (!publisherId.equals(house.getPublisherId())) {
            throw new BusinessException("只能查看自己的房源详情");
        }
        return house;
    }

    @Override
    public House adminDetail(Long id) {
        return detail(id);
    }

    @Override
    public List<House> auditList() {
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getAuditStatus, 0)
                .orderByDesc(House::getId);
        List<House> list = houseMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public void auditPass(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        house.setAuditStatus(1);
        if (house.getStatus() == null || house.getStatus() == 0) {
            house.setStatus(1);
        }
        houseMapper.updateById(house);
    }

    @Override
    public void auditReject(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        house.setAuditStatus(2);
        house.setStatus(0);
        houseMapper.updateById(house);
    }

    @Override
    public List<House> myList(Long publisherId) {
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getPublisherId, publisherId)
                .orderByDesc(House::getId);
        List<House> list = houseMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    @Override
    public void delete(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        if (house.getStatus() != null && (house.getStatus() == 2 || house.getStatus() == 3)) {
            throw new BusinessException("已有签约或订单关联的房源不允许删除");
        }
        houseMapper.deleteById(id);
    }

    private void applyForm(House house,
                           String title,
                           String address,
                           String city,
                           String area,
                           java.math.BigDecimal rentPrice,
                           java.math.BigDecimal deposit,
                           String houseType,
                           java.math.BigDecimal square,
                           String floor,
                           String description,
                           String imageUrls,
                           java.math.BigDecimal longitude,
                           java.math.BigDecimal latitude) {
        house.setTitle(title);
        house.setAddress(address);
        house.setCity(city);
        house.setArea(area);
        house.setRentPrice(rentPrice);
        house.setDeposit(deposit);
        house.setHouseType(houseType);
        house.setSquare(square);
        house.setFloor(floor);
        house.setDescription(description);
        house.setImageUrls(normalizeImageUrls(imageUrls));
        house.setLongitude(longitude);
        house.setLatitude(latitude);
    }

    private String normalizeImageUrls(String imageUrls) {
        if (!StringUtils.hasText(imageUrls)) {
            return null;
        }
        return imageUrls.trim();
    }

    private void fillDisplayFields(List<House> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (House house : list) {
            fillDisplayFields(house);
        }
    }

    private void fillDisplayFields(House house) {
        if (house == null) {
            return;
        }
        house.setPublisherName(userDisplayName(house.getPublisherId()));
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
