package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.VaccineRecord;
import java.util.List;

public interface VaccineService {
    void createVaccine(Long petId, VaccineRecord record);
    PageResult<VaccineRecord> getVaccinesByPet(Long petId, int page, int size);
    void updateVaccine(Long id, VaccineRecord record);
    void deleteVaccine(Long id);
    List<VaccineRecord> getDueVaccines(int days);
}
