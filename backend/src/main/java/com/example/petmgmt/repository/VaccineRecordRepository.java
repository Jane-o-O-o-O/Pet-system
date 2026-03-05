package com.example.petmgmt.repository;

import com.example.petmgmt.domain.entity.VaccineRecord;
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
public class VaccineRecordRepository {
    
    private static final String FILE_NAME = "vaccine_records";
    
    @Autowired
    private FileStorage fileStorage;
    
    public VaccineRecord save(VaccineRecord record) {
        List<VaccineRecord> records = loadAll();
        
        if (record.getId() == null) {
            record.setId(fileStorage.nextId(FILE_NAME));
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            records.add(record);
        } else {
            record.setUpdatedAt(LocalDateTime.now());
            records = records.stream()
                    .map(r -> r.getId().equals(record.getId()) ? record : r)
                    .collect(Collectors.toList());
        }
        
        fileStorage.save(FILE_NAME, records);
        return record;
    }
    
    public Optional<VaccineRecord> findById(Long id) {
        return loadAll().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }
    
    public List<VaccineRecord> findAll() {
        return new ArrayList<>(loadAll());
    }
    
    public List<VaccineRecord> findAll(Predicate<VaccineRecord> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    public long count(Predicate<VaccineRecord> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .count();
    }
    
    public void deleteById(Long id) {
        List<VaccineRecord> records = loadAll()
                .stream()
                .filter(r -> !r.getId().equals(id))
                .collect(Collectors.toList());
        fileStorage.save(FILE_NAME, records);
    }
    
    private List<VaccineRecord> loadAll() {
        return fileStorage.load(FILE_NAME, new TypeReference<>() {});
    }
}
