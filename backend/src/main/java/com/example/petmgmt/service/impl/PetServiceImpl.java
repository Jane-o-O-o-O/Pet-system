package com.example.petmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.mapper.PetMapper;
import com.example.petmgmt.mapper.UserMapper;
import com.example.petmgmt.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetMapper petMapper;
    private final UserMapper userMapper;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public void createPet(Pet pet) {
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) {
            pet.setOwnerId(user.getId());
        } else if (pet.getOwnerId() != null) {
            User owner = userMapper.selectById(pet.getOwnerId());
            if (owner == null) throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        petMapper.insert(pet);
    }

    @Override
    public PageResult<Pet> getPets(int page, int size, String keyword, String species) {
        User user = getCurrentUser();
        LambdaQueryWrapper<Pet> query = new LambdaQueryWrapper<>();
        if (user.getRole() == Role.OWNER) {
            query.eq(Pet::getOwnerId, user.getId());
        }
        if (StringUtils.hasText(keyword)) {
            query.like(Pet::getName, keyword);
        }
        if (StringUtils.hasText(species)) {
            query.eq(Pet::getSpecies, species);
        }

        Page<Pet> petPage = petMapper.selectPage(new Page<>(page, size), query);
        return new PageResult<>(petPage.getRecords(), petPage.getTotal(), petPage.getCurrent(), petPage.getSize());
    }

    @Override
    public Pet getPetById(Long id) {
        Pet pet = petMapper.selectById(id);
        if (pet == null) throw new BizException(ErrorCode.PET_NOT_FOUND);

        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER && !pet.getOwnerId().equals(user.getId())) {
            throw new BizException(ErrorCode.PET_ACCESS_DENIED);
        }
        return pet;
    }

    @Override
    public void updatePet(Long id, Pet pet) {
        Pet existing = getPetById(id);
        if (pet.getName() != null) existing.setName(pet.getName());
        if (pet.getSpecies() != null) existing.setSpecies(pet.getSpecies());
        if (pet.getBreed() != null) existing.setBreed(pet.getBreed());
        if (pet.getGender() != null) existing.setGender(pet.getGender());
        if (pet.getBirthDate() != null) existing.setBirthDate(pet.getBirthDate());
        if (pet.getWeight() != null) existing.setWeight(pet.getWeight());
        if (pet.getSterilized() != null) existing.setSterilized(pet.getSterilized());
        if (pet.getPhotoUrl() != null) existing.setPhotoUrl(pet.getPhotoUrl());
        if (pet.getRemark() != null) existing.setRemark(pet.getRemark());
        petMapper.updateById(existing);
    }

    @Override
    public void deletePet(Long id) {
        getPetById(id);
        petMapper.deleteById(id);
    }
}
