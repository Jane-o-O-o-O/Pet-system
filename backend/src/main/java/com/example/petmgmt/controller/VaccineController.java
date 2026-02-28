package com.example.petmgmt.controller;

import com.example.petmgmt.common.ApiResponse;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.VaccineRecord;
import com.example.petmgmt.service.VaccineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VaccineController {

    private final VaccineService vaccineService;

    @PostMapping("/pets/{petId}/vaccines")
    public ApiResponse<Void> createVaccine(@PathVariable Long petId, @RequestBody VaccineRecord record) {
        vaccineService.createVaccine(petId, record);
        return ApiResponse.success();
    }

    @GetMapping("/pets/{petId}/vaccines")
    public ApiResponse<PageResult<VaccineRecord>> getVaccinesByPet(
            @PathVariable Long petId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(vaccineService.getVaccinesByPet(petId, page, size));
    }

    @PutMapping("/vaccines/{id}")
    public ApiResponse<Void> updateVaccine(@PathVariable Long id, @RequestBody VaccineRecord record) {
        vaccineService.updateVaccine(id, record);
        return ApiResponse.success();
    }

    @DeleteMapping("/vaccines/{id}")
    public ApiResponse<Void> deleteVaccine(@PathVariable Long id) {
        vaccineService.deleteVaccine(id);
        return ApiResponse.success();
    }

    @GetMapping("/reminds/vaccines")
    public ApiResponse<List<VaccineRecord>> getDueVaccines(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(vaccineService.getDueVaccines(days));
    }
}
