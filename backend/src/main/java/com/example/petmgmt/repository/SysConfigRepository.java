package com.example.petmgmt.repository;

import com.example.petmgmt.domain.entity.SysConfig;
import com.example.petmgmt.storage.FileStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SysConfigRepository {
    
    private static final String FILE_NAME = "sys_configs";
    
    @Autowired
    private FileStorage fileStorage;
    
    public SysConfig save(SysConfig config) {
        List<SysConfig> configs = loadAll();
        
        config.setUpdatedAt(LocalDateTime.now());
        
        boolean exists = configs.stream()
                .anyMatch(c -> c.getConfigKey().equals(config.getConfigKey()));
        
        if (exists) {
            configs = configs.stream()
                    .map(c -> c.getConfigKey().equals(config.getConfigKey()) ? config : c)
                    .collect(Collectors.toList());
        } else {
            configs.add(config);
        }
        
        fileStorage.save(FILE_NAME, configs);
        return config;
    }
    
    public Optional<SysConfig> findByKey(String key) {
        return loadAll().stream()
                .filter(c -> c.getConfigKey().equals(key))
                .findFirst();
    }
    
    public List<SysConfig> findAll() {
        return new ArrayList<>(loadAll());
    }
    
    public void deleteByKey(String key) {
        List<SysConfig> configs = loadAll()
                .stream()
                .filter(c -> !c.getConfigKey().equals(key))
                .collect(Collectors.toList());
        fileStorage.save(FILE_NAME, configs);
    }
    
    private List<SysConfig> loadAll() {
        return fileStorage.load(FILE_NAME, new TypeReference<>() {});
    }
}
