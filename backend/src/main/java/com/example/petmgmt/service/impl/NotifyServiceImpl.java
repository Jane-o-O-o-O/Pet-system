package com.example.petmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.Notification;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.mapper.NotificationMapper;
import com.example.petmgmt.mapper.UserMapper;
import com.example.petmgmt.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public void createNotification(Long userId, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(0);
        notificationMapper.insert(notification);
    }

    @Override
    public PageResult<Notification> getMyNotifications(int page, int size) {
        User user = getCurrentUser();
        Page<Notification> nPage = notificationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, user.getId()).orderByDesc(Notification::getCreatedAt)
        );
        return new PageResult<>(nPage.getRecords(), nPage.getTotal(), nPage.getCurrent(), nPage.getSize());
    }

    @Override
    public void markAsRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification != null && notification.getUserId().equals(getCurrentUser().getId())) {
            notification.setReadFlag(1);
            notificationMapper.updateById(notification);
        }
    }
}
