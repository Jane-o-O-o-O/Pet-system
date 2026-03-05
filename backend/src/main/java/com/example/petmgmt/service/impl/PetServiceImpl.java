package com.example.petmgmt.service.impl;

import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.repository.PetRepository;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.service.PetService;
import com.example.petmgmt.storage.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void createPet(Pet pet) {
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER) {
            pet.setOwnerId(user.getId());
        } else if (pet.getOwnerId() != null) {
            User owner = userRepository.findById(pet.getOwnerId())
                    .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        }
        petRepository.save(pet);
    }

    @Override
    public PageResult<Pet> getPets(int page, int size, String keyword, String species) {
        User user = getCurrentUser();
        
        List<Pet> pets = petRepository.findAll(pet -> {
            if (user.getRole() == Role.OWNER && !pet.getOwnerId().equals(user.getId())) {
                return false;
            }
            if (StringUtils.hasText(keyword) && !pet.getName().contains(keyword)) {
                return false;
            }
            if (StringUtils.hasText(species) && !species.equals(pet.getSpecies())) {
                return false;
            }
            return true;
        });
        
        pets = pets.stream()
                .sorted(Comparator.comparing(Pet::getCreatedAt).reversed())
                .collect(Collectors.toList());
        
        PageHelper.PageData<Pet> pageData = PageHelper.paginate(pets, page, size);
        return new PageResult<>(pageData.getRecords(), pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }

    @Override
    public Pet getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.PET_NOT_FOUND));

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
        petRepository.save(existing);
    }

    @Override
    public void deletePet(Long id) {
        getPetById(id);
        petRepository.deleteById(id);
    }
}
