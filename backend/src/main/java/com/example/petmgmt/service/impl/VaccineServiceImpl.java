package com.example.petmgmt.service.impl;

import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.SysConfig;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.entity.VaccineRecord;
import com.example.petmgmt.domain.enums.RemindStatus;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.repository.PetRepository;
import com.example.petmgmt.repository.SysConfigRepository;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.repository.VaccineRecordRepository;
import com.example.petmgmt.service.VaccineService;
import com.example.petmgmt.storage.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccineServiceImpl implements VaccineService {

    private final VaccineRecordRepository vaccineRecordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final SysConfigRepository sysConfigRepository;

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
    public void createVaccine(Long petId, VaccineRecord record) {
        checkPetAccess(petId);
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) throw new BizException(ErrorCode.FORBIDDEN);
        
        if (record.getNextDueDate().isBefore(record.getShotDate())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Next due date must be after shot date");
        }
        
        if (record.getRemindDaysBefore() == null) {
            SysConfig config = sysConfigRepository.findByKey("VACCINE_REMIND_DAYS_DEFAULT").orElse(null);
            record.setRemindDaysBefore(config != null ? Integer.parseInt(config.getConfigValue()) : 7);
        }
        
        record.setPetId(petId);
        record.setCreatedBy(user.getId());
        record.setRemindStatus(RemindStatus.PENDING);
        vaccineRecordRepository.save(record);
    }

    @Override
    public PageResult<VaccineRecord> getVaccinesByPet(Long petId, int page, int size) {
        checkPetAccess(petId);
        List<VaccineRecord> records = vaccineRecordRepository.findAll(r -> r.getPetId().equals(petId))
                .stream()
                .sorted(Comparator.comparing(VaccineRecord::getShotDate).reversed())
                .collect(Collectors.toList());
        
        PageHelper.PageData<VaccineRecord> pageData = PageHelper.paginate(records, page, size);
        return new PageResult<>(pageData.getRecords(), pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }

    @Override
    public void updateVaccine(Long id, VaccineRecord record) {
        VaccineRecord existing = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        checkPetAccess(existing.getPetId());
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) throw new BizException(ErrorCode.FORBIDDEN);
        
        existing.setVaccineName(record.getVaccineName());
        existing.setShotDate(record.getShotDate());
        existing.setNextDueDate(record.getNextDueDate());
        existing.setRemindDaysBefore(record.getRemindDaysBefore());
        
        if (record.getNextDueDate() != null && !record.getNextDueDate().equals(existing.getNextDueDate())) {
            existing.setRemindStatus(RemindStatus.PENDING);
        }
        vaccineRecordRepository.save(existing);
    }

    @Override
    public void deleteVaccine(Long id) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        checkPetAccess(record.getPetId());
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) throw new BizException(ErrorCode.FORBIDDEN);
        vaccineRecordRepository.deleteById(id);
    }

    @Override
    public List<VaccineRecord> getDueVaccines(int days) {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(days);
        
        List<VaccineRecord> records = vaccineRecordRepository.findAll(r -> {
            if (r.getNextDueDate() == null) return false;
            if (r.getNextDueDate().isBefore(today)) return false;
            if (r.getNextDueDate().isAfter(future)) return false;
            return true;
        });
        
        if (user.getRole() == Role.OWNER) {
            List<Long> myPetIds = petRepository.findAll(p -> p.getOwnerId().equals(user.getId()))
                    .stream()
                    .map(Pet::getId)
                    .collect(Collectors.toList());
            if (myPetIds.isEmpty()) return List.of();
            records = records.stream()
                    .filter(r -> myPetIds.contains(r.getPetId()))
                    .collect(Collectors.toList());
        }
        
        return records;
    }
}
