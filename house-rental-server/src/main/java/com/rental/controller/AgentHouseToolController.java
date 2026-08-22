package com.rental.controller;

import com.rental.common.Result;
import com.rental.dto.HouseSearchDTO;
import com.rental.entity.House;
import com.rental.service.HouseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/agent-tools/houses")
public class AgentHouseToolController {

    private final HouseService houseService;

    public AgentHouseToolController(HouseService houseService) {
        this.houseService = houseService;
    }

    @GetMapping("/search")
    public Result<List<House>> search(@ModelAttribute @Valid HouseSearchDTO dto) {
        return Result.success(houseService.search(dto));
    }

    @GetMapping("/{id}")
    public Result<House> detail(@PathVariable Long id) {
        return Result.success(houseService.publicDetail(id));
    }
}
