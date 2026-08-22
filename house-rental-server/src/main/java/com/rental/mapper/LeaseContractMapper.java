package com.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rental.entity.LeaseContract;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LeaseContractMapper extends BaseMapper<LeaseContract> {
}