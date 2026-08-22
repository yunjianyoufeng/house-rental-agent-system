package com.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rental.entity.LeaseOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LeaseOrderMapper extends BaseMapper<LeaseOrder> {
}