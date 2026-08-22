package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.common.QrCodeUtil;
import com.rental.config.PaymentProperties;
import com.rental.dto.LeaseOrderCreateDTO;
import com.rental.dto.LeaseOrderPayDTO;
import com.rental.entity.House;
import com.rental.entity.LeaseContract;
import com.rental.entity.LeaseOrder;
import com.rental.entity.SysUser;
import com.rental.exception.BusinessException;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.LeaseContractMapper;
import com.rental.mapper.LeaseOrderMapper;
import com.rental.mapper.SysUserMapper;
import com.rental.service.LeaseOrderService;
import com.rental.vo.PaymentStartVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LeaseOrderServiceImpl implements LeaseOrderService {

    private static final Set<String> ALLOWED_PAY_TYPES = Set.of("WECHAT", "ALIPAY");
    private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(30);

    private final LeaseOrderMapper leaseOrderMapper;
    private final LeaseContractMapper leaseContractMapper;
    private final HouseMapper houseMapper;
    private final SysUserMapper sysUserMapper;
    private final PaymentProperties paymentProperties;

    public LeaseOrderServiceImpl(LeaseOrderMapper leaseOrderMapper,
                                 LeaseContractMapper leaseContractMapper,
                                 HouseMapper houseMapper,
                                 SysUserMapper sysUserMapper,
                                 PaymentProperties paymentProperties) {
        this.leaseOrderMapper = leaseOrderMapper;
        this.leaseContractMapper = leaseContractMapper;
        this.houseMapper = houseMapper;
        this.sysUserMapper = sysUserMapper;
        this.paymentProperties = paymentProperties;
    }

    @Override
    public void create(LeaseOrderCreateDTO dto) {
        LeaseContract contract = leaseContractMapper.selectById(dto.getContractId());
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        if (!dto.getTenantId().equals(contract.getTenantId())) {
            throw new BusinessException("只能为自己的合同创建订单");
        }

        LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaseOrder::getContractId, dto.getContractId());
        if (leaseOrderMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该合同已存在订单，请勿重复创建");
        }

        LeaseOrder order = new LeaseOrder();
        order.setContractId(dto.getContractId());
        order.setTenantId(contract.getTenantId());
        order.setAmount(dto.getAmount());
        order.setPayStatus(0);

        int rows = leaseOrderMapper.insert(order);
        if (rows <= 0) {
            throw new BusinessException("订单创建失败");
        }
    }

    @Override
    public List<LeaseOrder> tenantList(Long tenantId) {
        LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaseOrder::getTenantId, tenantId)
                .orderByDesc(LeaseOrder::getId);
        List<LeaseOrder> list = leaseOrderMapper.selectList(wrapper);
        refreshOrders(list);
        return list;
    }

    @Override
    public List<LeaseOrder> landlordList(Long landlordId) {
        LambdaQueryWrapper<LeaseContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(LeaseContract::getLandlordId, landlordId)
                .orderByDesc(LeaseContract::getId);

        List<LeaseContract> contractList = leaseContractMapper.selectList(contractWrapper);
        if (contractList == null || contractList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> contractIds = new ArrayList<>();
        for (LeaseContract contract : contractList) {
            contractIds.add(contract.getId());
        }

        LambdaQueryWrapper<LeaseOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(LeaseOrder::getContractId, contractIds)
                .orderByDesc(LeaseOrder::getId);
        List<LeaseOrder> list = leaseOrderMapper.selectList(orderWrapper);
        refreshOrders(list);
        return list;
    }

    @Override
    public List<LeaseOrder> adminList() {
        LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LeaseOrder::getId);
        List<LeaseOrder> list = leaseOrderMapper.selectList(wrapper);
        refreshOrders(list);
        return list;
    }

    @Override
    public PaymentStartVO startPayment(Long id, LeaseOrderPayDTO dto) {
        LeaseOrder order = validatePayableOrder(id);
        String payType = normalizePayType(dto.getPayType());

        PaymentStartVO vo = new PaymentStartVO();
        vo.setOrderId(order.getId());
        vo.setAmount(order.getAmount());
        vo.setPayType(payType);
        vo.setDemoMode(paymentProperties.isDemoMode());

        if ("WECHAT".equals(payType)) {
            vo.setPayTypeLabel("微信支付");
            vo.setInstruction(paymentProperties.isDemoMode()
                    ? "演示模式：已生成微信支付二维码，请点击“模拟支付成功”完成订单。"
                    : "请使用微信扫一扫完成支付。");
            vo.setQrCodeBase64(QrCodeUtil.toBase64Png(
                    "HOUSE-RENTAL|WECHAT|ORDER-" + order.getId() + "|AMOUNT-" + order.getAmount() + "|TS-" + System.currentTimeMillis(),
                    260,
                    260
            ));
        } else {
            vo.setPayTypeLabel("支付宝支付");
            vo.setInstruction(paymentProperties.isDemoMode()
                    ? "演示模式：请点击“模拟支付成功”完成订单。"
                    : "请跳转到支付宝收银台完成支付。");
            vo.setCashierTitle("支付宝收银台");
            vo.setCashierDescription("订单金额：¥" + order.getAmount());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id, LeaseOrderPayDTO dto) {
        LeaseOrder order = validatePayableOrder(id);
        String payType = normalizePayType(dto.getPayType());

        LeaseContract contract = leaseContractMapper.selectById(order.getContractId());
        if (contract == null) {
            throw new BusinessException("订单关联的合同不存在");
        }

        House house = houseMapper.selectById(contract.getHouseId());
        if (house == null) {
            throw new BusinessException("合同关联的房源不存在");
        }
        if (contract.getStatus() != null && (contract.getStatus() == 2 || contract.getStatus() == 3)) {
            throw new BusinessException("合同已结束或已取消，不能支付");
        }
        if (house.getStatus() == null || house.getStatus() != 2) {
            throw new BusinessException("当前房源不是待支付状态，无法完成支付");
        }

        order.setPayType(payType);
        order.setPayStatus(1);
        order.setPayTime(LocalDateTime.now());
        int orderRows = leaseOrderMapper.updateById(order);
        if (orderRows <= 0) {
            throw new BusinessException("更新订单支付状态失败");
        }

        contract.setStatus(1);
        int contractRows = leaseContractMapper.updateById(contract);
        if (contractRows <= 0) {
            throw new BusinessException("更新合同状态失败");
        }

        house.setStatus(3);
        int houseRows = houseMapper.updateById(house);
        if (houseRows <= 0) {
            throw new BusinessException("更新房源状态失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long tenantId) {
        LeaseOrder order = detail(id);
        if (!tenantId.equals(order.getTenantId())) {
            throw new BusinessException("只能取消自己的订单");
        }
        if (!Integer.valueOf(0).equals(order.getPayStatus())) {
            throw new BusinessException("当前订单不可取消");
        }
        cancelUnpaidOrder(order, 2);
    }

    @Override
    public LeaseOrder detail(Long id) {
        LeaseOrder order = leaseOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        refreshOrderState(order);
        fillDisplayFields(order);
        return order;
    }

    private LeaseOrder validatePayableOrder(Long id) {
        LeaseOrder order = detail(id);
        if (order.getPayStatus() == null || order.getPayStatus() != 0) {
            if (Integer.valueOf(1).equals(order.getPayStatus())) {
                throw new BusinessException("订单已支付");
            }
            if (Integer.valueOf(2).equals(order.getPayStatus())) {
                throw new BusinessException("订单已取消");
            }
            if (Integer.valueOf(3).equals(order.getPayStatus())) {
                throw new BusinessException("订单已过期，请重新提交申请或联系房东");
            }
            throw new BusinessException("当前订单不可支付");
        }
        return order;
    }

    private void refreshOrders(List<LeaseOrder> list) {
        if (list == null) {
            return;
        }
        for (LeaseOrder order : list) {
            refreshOrderState(order);
            fillDisplayFields(order);
        }
    }

    private void refreshOrderState(LeaseOrder order) {
        if (order == null || !Integer.valueOf(0).equals(order.getPayStatus()) || order.getCreateTime() == null) {
            return;
        }
        if (order.getCreateTime().plus(PAYMENT_TIMEOUT).isAfter(LocalDateTime.now())) {
            return;
        }
        cancelUnpaidOrder(order, 3);
    }

    private void cancelUnpaidOrder(LeaseOrder order, int payStatus) {
        order.setPayStatus(payStatus);
        leaseOrderMapper.updateById(order);

        LeaseContract contract = leaseContractMapper.selectById(order.getContractId());
        if (contract != null && !Integer.valueOf(1).equals(contract.getStatus())) {
            contract.setStatus(3);
            leaseContractMapper.updateById(contract);
        }

        if (contract != null) {
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null && Integer.valueOf(2).equals(house.getStatus())) {
                house.setStatus(Integer.valueOf(1).equals(house.getAuditStatus()) ? 1 : 0);
                houseMapper.updateById(house);
            }
        }
    }

    private void fillDisplayFields(LeaseOrder order) {
        if (order == null) {
            return;
        }
        LeaseContract contract = leaseContractMapper.selectById(order.getContractId());
        if (contract != null) {
            order.setHouseId(contract.getHouseId());
            order.setLandlordName(userDisplayName(contract.getLandlordId()));
            House house = houseMapper.selectById(contract.getHouseId());
            order.setHouseTitle(house != null ? house.getTitle() : "--");
        } else {
            order.setHouseId(null);
            order.setHouseTitle("--");
            order.setLandlordName("--");
        }
        order.setTenantName(userDisplayName(order.getTenantId()));
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

    private String normalizePayType(String rawPayType) {
        String payType = rawPayType == null ? "" : rawPayType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PAY_TYPES.contains(payType)) {
            throw new BusinessException("支付方式仅支持微信支付或支付宝支付");
        }
        return payType;
    }
}
