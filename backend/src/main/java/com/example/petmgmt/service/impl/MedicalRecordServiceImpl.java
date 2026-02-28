package com.example.petmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.MedicalRecord;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.mapper.MedicalRecordMapper;
import com.example.petmgmt.mapper.PetMapper;
import com.example.petmgmt.mapper.UserMapper;
import com.example.petmgmt.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final PetMapper petMapper;
    private final UserMapper userMapper;

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
    public void createRecord(Long petId, MedicalRecord record) {
        checkPetAccess(petId);
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        record.setPetId(petId);
        record.setCreatedBy(user.getId());
        medicalRecordMapper.insert(record);
    }

    @Override
    public PageResult<MedicalRecord> getRecordsByPet(Long petId, int page, int size) {
        checkPetAccess(petId);
        Page<MedicalRecord> mrPage = medicalRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<MedicalRecord>().eq(MedicalRecord::getPetId, petId).orderByDesc(MedicalRecord::getVisitDate)
        );
        return new PageResult<>(mrPage.getRecords(), mrPage.getTotal(), mrPage.getCurrent(), mrPage.getSize());
    }

    @Override
    public MedicalRecord getRecordById(Long id) {
        MedicalRecord record = medicalRecordMapper.selectById(id);
        if (record == null) throw new BizException(ErrorCode.NOT_FOUND);
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
        record.setId(id);
        record.setPetId(existing.getPetId());
        medicalRecordMapper.updateById(record);
    }

    @Override
    public void deleteRecord(Long id) {
        MedicalRecord record = getRecordById(id);
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        medicalRecordMapper.deleteById(id);
    }
}
