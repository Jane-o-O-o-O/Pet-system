package com.example.petmgmt.repository;

import com.example.petmgmt.domain.entity.User;
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
public class UserRepository {
    
    private static final String FILE_NAME = "users";
    
    @Autowired
    private FileStorage fileStorage;
    
    public User save(User user) {
        List<User> users = loadAll();
        
        if (user.getId() == null) {
            // 新增
            user.setId(fileStorage.nextId(FILE_NAME));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            users.add(user);
        } else {
            // 更新
            user.setUpdatedAt(LocalDateTime.now());
            users = users.stream()
                    .map(u -> u.getId().equals(user.getId()) ? user : u)
                    .collect(Collectors.toList());
        }
        
        fileStorage.save(FILE_NAME, users);
        return user;
    }
    
    public Optional<User> findById(Long id) {
        return loadAll().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }
    
    public Optional<User> findByUsername(String username) {
        return loadAll().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }
    
    public List<User> findAll() {
        return new ArrayList<>(loadAll());
    }
    
    public List<User> findAll(Predicate<User> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    public long count(Predicate<User> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .count();
    }
    
    public void deleteById(Long id) {
        List<User> users = loadAll()
                .stream()
                .filter(u -> !u.getId().equals(id))
                .collect(Collectors.toList());
        fileStorage.save(FILE_NAME, users);
    }
    
    private List<User> loadAll() {
        return fileStorage.load(FILE_NAME, new TypeReference<>() {});
    }
}
