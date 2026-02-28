package com.example.petmgmt.controller;

import com.example.petmgmt.common.ApiResponse;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.MedicalRecord;
import com.example.petmgmt.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping("/pets/{petId}/medical-records")
    public ApiResponse<Void> createRecord(@PathVariable Long petId, @RequestBody MedicalRecord record) {
        medicalRecordService.createRecord(petId, record);
        return ApiResponse.success();
    }

    @GetMapping("/pets/{petId}/medical-records")
    public ApiResponse<PageResult<MedicalRecord>> getRecordsByPet(
            @PathVariable Long petId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(medicalRecordService.getRecordsByPet(petId, page, size));
    }

    @GetMapping("/medical-records/{id}")
    public ApiResponse<MedicalRecord> getRecordById(@PathVariable Long id) {
        return ApiResponse.success(medicalRecordService.getRecordById(id));
    }

    @PutMapping("/medical-records/{id}")
    public ApiResponse<Void> updateRecord(@PathVariable Long id, @RequestBody MedicalRecord record) {
        medicalRecordService.updateRecord(id, record);
        return ApiResponse.success();
    }

    @DeleteMapping("/medical-records/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        medicalRecordService.deleteRecord(id);
        return ApiResponse.success();
    }
}
