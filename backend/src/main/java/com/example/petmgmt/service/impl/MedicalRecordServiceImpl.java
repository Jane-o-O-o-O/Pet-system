package com.example.petmgmt.service.impl;

import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.MedicalRecord;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.repository.MedicalRecordRepository;
import com.example.petmgmt.repository.PetRepository;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.service.MedicalRecordService;
import com.example.petmgmt.storage.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    private void checkPetAccess(Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BizException(ErrorCode.PET_NOT_FOUND));
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER && !pet.getOwnerId().equals(user.getId())) {
            throw new BizException(ErrorCode.PET_ACCESS_DENIED);
        }
    }

    @Override
    public void createRecord(Long petId, MedicalRecord record) {
        checkPetAccess(petId);
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        record.setPetId(petId);
        record.setCreatedBy(user.getId());
        medicalRecordRepository.save(record);
    }

    @Override
    public PageResult<MedicalRecord> getRecordsByPet(Long petId, int page, int size) {
        checkPetAccess(petId);
        var records = medicalRecordRepository.findAll(r -> r.getPetId().equals(petId))
                .stream()
                .sorted(Comparator.comparing(MedicalRecord::getVisitDate).reversed())
                .collect(Collectors.toList());
        
        PageHelper.PageData<MedicalRecord> pageData = PageHelper.paginate(records, page, size);
        return new PageResult<>(pageData.getRecords(), pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }

    @Override
    public MedicalRecord getRecordById(Long id) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        checkPetAccess(record.getPetId());
        return record;
    }

    @Override
    public void updateRecord(Long id, MedicalRecord record) {
        MedicalRecord existing = getRecordById(id);
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        existing.setDoctorName(record.getDoctorName());
        existing.setVisitDate(record.getVisitDate());
        existing.setComplaint(record.getComplaint());
        existing.setDiagnosis(record.getDiagnosis());
        existing.setTreatment(record.getTreatment());
        existing.setAttachments(record.getAttachments());
        medicalRecordRepository.save(existing);
    }

    @Override
    public void deleteRecord(Long id) {
        MedicalRecord record = getRecordById(id);
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        medicalRecordRepository.deleteById(id);
    }
}
