package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.Pet;
import java.util.List;

public interface PetService {
    void createPet(Pet pet);
    PageResult<Pet> getPets(int page, int size, String keyword, String species);
    Pet getPetById(Long id);
    void updatePet(Long id, Pet pet);
    void deletePet(Long id);
}
