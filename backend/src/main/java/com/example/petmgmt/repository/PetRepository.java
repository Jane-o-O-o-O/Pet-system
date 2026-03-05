package com.example.petmgmt.repository;

import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.storage.FileStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
public class PetRepository {
    
    private static final String FILE_NAME = "pets";
    
    @Autowired
    private FileStorage fileStorage;
    
    public Pet save(Pet pet) {
        List<Pet> pets = loadAll();
        
        if (pet.getId() == null) {
            pet.setId(fileStorage.nextId(FILE_NAME));
            pet.setCreatedAt(LocalDateTime.now());
            pet.setUpdatedAt(LocalDateTime.now());
            pets.add(pet);
        } else {
            pet.setUpdatedAt(LocalDateTime.now());
            pets = pets.stream()
                    .map(p -> p.getId().equals(pet.getId()) ? pet : p)
                    .collect(Collectors.toList());
        }
        
        fileStorage.save(FILE_NAME, pets);
        return pet;
    }
    
    public Optional<Pet> findById(Long id) {
        return loadAll().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }
    
    public List<Pet> findAll() {
        return new ArrayList<>(loadAll());
    }
    
    public List<Pet> findAll(Predicate<Pet> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    public long count(Predicate<Pet> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .count();
    }
    
    public void deleteById(Long id) {
        List<Pet> pets = loadAll()
                .stream()
                .filter(p -> !p.getId().equals(id))
                .collect(Collectors.toList());
        fileStorage.save(FILE_NAME, pets);
    }
    
    private List<Pet> loadAll() {
        return fileStorage.load(FILE_NAME, new TypeReference<>() {});
    }
}
