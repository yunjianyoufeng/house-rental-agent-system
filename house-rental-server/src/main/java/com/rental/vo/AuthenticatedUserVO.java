package com.rental.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatedUserVO {
    private Long id;
    private String roleCode;
    private Integer status;
}
