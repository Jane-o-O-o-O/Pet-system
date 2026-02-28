package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.MedicalRecord;

public interface MedicalRecordService {
    void createRecord(Long petId, MedicalRecord record);
    PageResult<MedicalRecord> getRecordsByPet(Long petId, int page, int size);
    MedicalRecord getRecordById(Long id);
    void updateRecord(Long id, MedicalRecord record);
    void deleteRecord(Long id);
}
