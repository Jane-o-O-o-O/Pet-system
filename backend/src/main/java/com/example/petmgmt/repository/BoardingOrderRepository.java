package com.example.petmgmt.repository;

import com.example.petmgmt.domain.entity.BoardingOrder;
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
public class BoardingOrderRepository {
    
    private static final String FILE_NAME = "boarding_orders";
    
    @Autowired
    private FileStorage fileStorage;
    
    public BoardingOrder save(BoardingOrder order) {
        List<BoardingOrder> orders = loadAll();
        
        if (order.getId() == null) {
            order.setId(fileStorage.nextId(FILE_NAME));
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orders.add(order);
        } else {
            order.setUpdatedAt(LocalDateTime.now());
            orders = orders.stream()
                    .map(o -> o.getId().equals(order.getId()) ? order : o)
                    .collect(Collectors.toList());
        }
        
        fileStorage.save(FILE_NAME, orders);
        return order;
    }
    
    public Optional<BoardingOrder> findById(Long id) {
        return loadAll().stream()
                .filter(o -> o.getId().equals(id))
                .findFirst();
    }
    
    public Optional<BoardingOrder> findByOrderNo(String orderNo) {
        return loadAll().stream()
                .filter(o -> o.getOrderNo().equals(orderNo))
                .findFirst();
    }
    
    public List<BoardingOrder> findAll() {
        return new ArrayList<>(loadAll());
    }
    
    public List<BoardingOrder> findAll(Predicate<BoardingOrder> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    public long count(Predicate<BoardingOrder> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .count();
    }
    
    public void deleteById(Long id) {
        List<BoardingOrder> orders = loadAll()
                .stream()
                .filter(o -> !o.getId().equals(id))
                .collect(Collectors.toList());
        fileStorage.save(FILE_NAME, orders);
    }
    
    private List<BoardingOrder> loadAll() {
        return fileStorage.load(FILE_NAME, new TypeReference<>() {});
    }
}
