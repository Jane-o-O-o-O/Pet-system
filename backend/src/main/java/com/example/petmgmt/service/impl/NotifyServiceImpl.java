package com.example.petmgmt.service.impl;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.Notification;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.repository.NotificationRepository;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.service.NotifyService;
import com.example.petmgmt.storage.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public void createNotification(Long userId, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(0);
        notificationRepository.save(notification);
    }

    @Override
    public PageResult<Notification> getMyNotifications(int page, int size) {
        User user = getCurrentUser();
        List<Notification> notifications = notificationRepository.findAll(
                notification -> notification.getUserId().equals(user.getId())
        ).stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        
        PageHelper.PageData<Notification> pageData = PageHelper.paginate(notifications, page, size);
        return new PageResult<>(pageData.getRecords(), pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }

    @Override
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            if (notification.getUserId().equals(getCurrentUser().getId())) {
                notification.setReadFlag(1);
                notificationRepository.save(notification);
            }
        });
    }
}
