package com.example.petmgmt.controller;

import com.example.petmgmt.common.ApiResponse;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ApiResponse<Void> createPet(@RequestBody Pet pet) {
        petService.createPet(pet);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<PageResult<Pet>> getPets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String species) {
        return ApiResponse.success(petService.getPets(page, size, keyword, species));
    }

    @GetMapping("/{id}")
    public ApiResponse<Pet> getPetById(@PathVariable Long id) {
        return ApiResponse.success(petService.getPetById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updatePet(@PathVariable Long id, @RequestBody Pet pet) {
        petService.updatePet(id, pet);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return ApiResponse.success();
    }
}
