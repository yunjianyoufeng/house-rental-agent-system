package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.HouseSearchDTO;
import com.rental.entity.House;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HouseServiceSearchTest {

    @Test
    void whitespaceSeparatedKeywordsAreMatchedIndividually() {
        HouseMapper houseMapper = mock(HouseMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        HouseServiceImpl service = new HouseServiceImpl(houseMapper, userMapper);

        when(houseMapper.selectList(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<House> wrapper = invocation.getArgument(0);
            wrapper.getSqlSegment();
            List<String> values = wrapper.getParamNameValuePairs().values().stream()
                    .map(String::valueOf)
                    .toList();
            assertTrue(values.stream().anyMatch(value -> value.contains("安静")));
            assertTrue(values.stream().anyMatch(value -> value.contains("学校")));
            assertFalse(values.stream().anyMatch(value -> value.contains("安静 学校")));
            return List.of();
        });

        HouseSearchDTO dto = new HouseSearchDTO();
        dto.setKeyword("安静 学校");

        service.search(dto);
    }
}
