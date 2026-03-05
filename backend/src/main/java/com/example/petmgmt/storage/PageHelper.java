package com.example.petmgmt.storage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分页辅助类
 */
public class PageHelper {
    
    public static <T> PageData<T> paginate(List<T> list, int page, int size) {
        if (list == null || list.isEmpty()) {
            return new PageData<>(List.of(), 0, page, size);
        }
        
        long total = list.size();
        int startIndex = (page - 1) * size;
        
        if (startIndex >= total) {
            return new PageData<>(List.of(), total, page, size);
        }
        
        int endIndex = Math.min(startIndex + size, list.size());
        List<T> pageData = list.subList(startIndex, endIndex);
        
        return new PageData<>(pageData, total, page, size);
    }
    
    public static class PageData<T> {
        private final List<T> records;
        private final long total;
        private final long current;
        private final long size;
        
        public PageData(List<T> records, long total, long current, long size) {
            this.records = records;
            this.total = total;
            this.current = current;
            this.size = size;
        }
        
        public List<T> getRecords() {
            return records;
        }
        
        public long getTotal() {
            return total;
        }
        
        public long getCurrent() {
            return current;
        }
        
        public long getSize() {
            return size;
        }
    }
}
