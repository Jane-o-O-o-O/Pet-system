package com.example.petmgmt.repository;

import com.example.petmgmt.domain.entity.Notification;
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
public class NotificationRepository {
    
    private static final String FILE_NAME = "notifications";
    
    @Autowired
    private FileStorage fileStorage;
    
    public Notification save(Notification notification) {
        List<Notification> notifications = loadAll();
        
        if (notification.getId() == null) {
            notification.setId(fileStorage.nextId(FILE_NAME));
            notification.setCreatedAt(LocalDateTime.now());
            notifications.add(notification);
        } else {
            notifications = notifications.stream()
                    .map(n -> n.getId().equals(notification.getId()) ? notification : n)
                    .collect(Collectors.toList());
        }
        
        fileStorage.save(FILE_NAME, notifications);
        return notification;
    }
    
    public Optional<Notification> findById(Long id) {
        return loadAll().stream()
                .filter(n -> n.getId().equals(id))
                .findFirst();
    }
    
    public List<Notification> findAll() {
        return new ArrayList<>(loadAll());
    }
    
    public List<Notification> findAll(Predicate<Notification> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    public long count(Predicate<Notification> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .count();
    }
    
    public void deleteById(Long id) {
        List<Notification> notifications = loadAll()
                .stream()
                .filter(n -> !n.getId().equals(id))
                .collect(Collectors.toList());
        fileStorage.save(FILE_NAME, notifications);
    }
    
    private List<Notification> loadAll() {
        return fileStorage.load(FILE_NAME, new TypeReference<>() {});
    }
}
