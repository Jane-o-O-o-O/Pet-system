package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.Notification;

public interface NotifyService {
    void createNotification(Long userId, String title, String content);
    PageResult<Notification> getMyNotifications(int page, int size);
    void markAsRead(Long id);
}
