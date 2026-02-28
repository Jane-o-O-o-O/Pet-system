package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.dto.ChangePasswordRequest;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;

public interface UserService {
    PageResult<User> getUsers(int page, int size, String keyword);
    void createUser(User user);
    void updateStatus(Long id, Integer status);
    void updateRole(Long id, Role role);
    User getCurrentUser();
    void changePassword(ChangePasswordRequest request);
}
