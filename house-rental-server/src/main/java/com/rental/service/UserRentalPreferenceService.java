package com.rental.service;

import com.rental.dto.UserRentalPreferenceUpdateDTO;
import com.rental.entity.UserRentalPreference;

public interface UserRentalPreferenceService {

    UserRentalPreference getByUserId(Long userId);

    UserRentalPreference saveOrUpdate(Long userId, UserRentalPreferenceUpdateDTO dto);

    void removeByUserId(Long userId);
}
