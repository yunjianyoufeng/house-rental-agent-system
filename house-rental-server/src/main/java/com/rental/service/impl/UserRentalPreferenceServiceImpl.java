package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.UserRentalPreferenceUpdateDTO;
import com.rental.entity.UserRentalPreference;
import com.rental.exception.BusinessException;
import com.rental.mapper.UserRentalPreferenceMapper;
import com.rental.service.UserRentalPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class UserRentalPreferenceServiceImpl implements UserRentalPreferenceService {

    private final UserRentalPreferenceMapper preferenceMapper;

    public UserRentalPreferenceServiceImpl(UserRentalPreferenceMapper preferenceMapper) {
        this.preferenceMapper = preferenceMapper;
    }

    @Override
    public UserRentalPreference getByUserId(Long userId) {
        return preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserRentalPreference>()
                        .eq(UserRentalPreference::getUserId, userId)
                        .last("LIMIT 1")
        );
    }

    @Override
    @Transactional
    public UserRentalPreference saveOrUpdate(Long userId,
                                             UserRentalPreferenceUpdateDTO dto) {
        UserRentalPreference preference = getByUserId(userId);
        boolean creating = preference == null;
        if (creating) {
            preference = new UserRentalPreference();
            preference.setUserId(userId);
        }

        if (dto.getBudgetMin() != null) {
            preference.setBudgetMin(dto.getBudgetMin());
        }
        if (dto.getBudgetMax() != null) {
            preference.setBudgetMax(dto.getBudgetMax());
        }
        setTextIfPresent(dto.getPreferredCity(), preference::setPreferredCity);
        setTextIfPresent(dto.getPreferredArea(), preference::setPreferredArea);
        setTextIfPresent(dto.getPreferredHouseType(), preference::setPreferredHouseType);
        setTextIfPresent(dto.getWorkplace(), preference::setWorkplace);
        if (dto.getMaxCommuteMinutes() != null) {
            preference.setMaxCommuteMinutes(dto.getMaxCommuteMinutes());
        }
        setTextIfPresent(dto.getPreferenceTags(), preference::setPreferenceTags);

        validateBudget(preference.getBudgetMin(), preference.getBudgetMax());
        int rows = creating
                ? preferenceMapper.insert(preference)
                : preferenceMapper.updateById(preference);
        if (rows <= 0) {
            throw new BusinessException("租房偏好保存失败");
        }
        return getByUserId(userId);
    }

    @Override
    @Transactional
    public void removeByUserId(Long userId) {
        preferenceMapper.delete(
                new LambdaQueryWrapper<UserRentalPreference>()
                        .eq(UserRentalPreference::getUserId, userId)
        );
    }

    private void setTextIfPresent(String value,
                                  java.util.function.Consumer<String> setter) {
        if (StringUtils.hasText(value)) {
            setter.accept(value.trim());
        }
    }

    private void validateBudget(BigDecimal budgetMin, BigDecimal budgetMax) {
        if (budgetMin != null && budgetMax != null
                && budgetMin.compareTo(budgetMax) > 0) {
            throw new BusinessException("最低预算不能高于最高预算");
        }
    }
}
