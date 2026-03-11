package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.vo.PetVO;

public interface PetService {
    void createPet(Pet pet);
    PageResult<PetVO> getPets(int page, int size, String keyword, String species);
    Pet getPetById(Long id);
    void updatePet(Long id, Pet pet);
    void deletePet(Long id);
}
