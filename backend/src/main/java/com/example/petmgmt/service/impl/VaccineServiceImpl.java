package com.example.petmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.example.petmgmt.mapper.PetMapper;
import com.example.petmgmt.mapper.SysConfigMapper;
import com.example.petmgmt.mapper.UserMapper;
import com.example.petmgmt.mapper.VaccineRecordMapper;
import com.example.petmgmt.service.VaccineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccineServiceImpl implements VaccineService {

    private final VaccineRecordMapper vaccineRecordMapper;
    private final PetMapper petMapper;
    private final UserMapper userMapper;
    private final SysConfigMapper sysConfigMapper;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private void checkPetAccess(Long petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) throw new BizException(ErrorCode.PET_NOT_FOUND);
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
            SysConfig config = sysConfigMapper.selectById("VACCINE_REMIND_DAYS_DEFAULT");
            record.setRemindDaysBefore(config != null ? Integer.parseInt(config.getConfigValue()) : 7);
        }
        
        record.setPetId(petId);
        record.setCreatedBy(user.getId());
        record.setRemindStatus(RemindStatus.PENDING);
        vaccineRecordMapper.insert(record);
    }

    @Override
    public PageResult<VaccineRecord> getVaccinesByPet(Long petId, int page, int size) {
        checkPetAccess(petId);
        Page<VaccineRecord> vrPage = vaccineRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<VaccineRecord>().eq(VaccineRecord::getPetId, petId).orderByDesc(VaccineRecord::getShotDate)
        );
        return new PageResult<>(vrPage.getRecords(), vrPage.getTotal(), vrPage.getCurrent(), vrPage.getSize());
    }

    @Override
    public void updateVaccine(Long id, VaccineRecord record) {
        VaccineRecord existing = vaccineRecordMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkPetAccess(existing.getPetId());
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) throw new BizException(ErrorCode.FORBIDDEN);
        
        record.setId(id);
        record.setPetId(existing.getPetId());
        if (record.getNextDueDate() != null && !record.getNextDueDate().equals(existing.getNextDueDate())) {
            record.setRemindStatus(RemindStatus.PENDING);
        }
        vaccineRecordMapper.updateById(record);
    }

    @Override
    public void deleteVaccine(Long id) {
        VaccineRecord record = vaccineRecordMapper.selectById(id);
        if (record == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkPetAccess(record.getPetId());
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) throw new BizException(ErrorCode.FORBIDDEN);
        vaccineRecordMapper.deleteById(id);
    }

    @Override
    public List<VaccineRecord> getDueVaccines(int days) {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(days);
        
        LambdaQueryWrapper<VaccineRecord> query = new LambdaQueryWrapper<VaccineRecord>()
                .ge(VaccineRecord::getNextDueDate, today)
                .le(VaccineRecord::getNextDueDate, future);
        
        if (user.getRole() == Role.OWNER) {
            List<Pet> myPets = petMapper.selectList(new LambdaQueryWrapper<Pet>().eq(Pet::getOwnerId, user.getId()));
            if (myPets.isEmpty()) return List.of();
            query.in(VaccineRecord::getPetId, myPets.stream().map(Pet::getId).collect(Collectors.toList()));
        }
        
        return vaccineRecordMapper.selectList(query);
    }
}
