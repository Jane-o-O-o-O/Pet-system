package com.example.petmgmt.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 文件存储服务 - 使用JSON文件替代数据库
 */
@Component
public class FileStorage {
    
    private static final String STORAGE_DIR = "data";
    private final ObjectMapper objectMapper;
    private final Map<String, ReadWriteLock> locks = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();
    
    public FileStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // 文件存储时包含所有字段，不受@JsonIgnore影响
        // 因为我们需要持久化所有数据包括密码
    }
    
    @PostConstruct
    public void init() throws IOException {
        Path storagePath = Paths.get(STORAGE_DIR);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
    }
    
    /**
     * 保存数据列表到文件
     */
    public <T> void save(String fileName, List<T> data) {
        ReadWriteLock lock = getLock(fileName);
        lock.writeLock().lock();
        try {
            File file = new File(STORAGE_DIR, fileName + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save data to file: " + fileName, e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 从文件读取数据列表
     */
    public <T> List<T> load(String fileName, TypeReference<List<T>> typeReference) {
        ReadWriteLock lock = getLock(fileName);
        lock.readLock().lock();
        try {
            File file = new File(STORAGE_DIR, fileName + ".json");
            if (!file.exists()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data from file: " + fileName, e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 生成下一个ID
     */
    public Long nextId(String entityName) {
        return sequences.computeIfAbsent(entityName, k -> {
            // 从现有数据中获取最大ID
            long maxId = 0;
            File file = new File(STORAGE_DIR, entityName + ".json");
            if (file.exists()) {
                try {
                    List<Map<String, Object>> data = objectMapper.readValue(file, new TypeReference<>() {});
                    for (Map<String, Object> item : data) {
                        Object idObj = item.get("id");
                        if (idObj instanceof Number) {
                            long id = ((Number) idObj).longValue();
                            if (id > maxId) {
                                maxId = id;
                            }
                        }
                    }
                } catch (IOException e) {
                    // 文件不存在或格式错误，从0开始
                }
            }
            return new AtomicLong(maxId);
        }).incrementAndGet();
    }
    
    private ReadWriteLock getLock(String fileName) {
        return locks.computeIfAbsent(fileName, k -> new ReentrantReadWriteLock());
    }
}
