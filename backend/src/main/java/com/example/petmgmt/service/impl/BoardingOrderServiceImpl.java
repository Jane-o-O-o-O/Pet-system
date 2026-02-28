package com.example.petmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.OrderStatus;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.domain.vo.BoardingOrderVO;
import org.springframework.beans.BeanUtils;
import com.example.petmgmt.mapper.BoardingOrderMapper;
import com.example.petmgmt.mapper.PetMapper;
import com.example.petmgmt.mapper.UserMapper;
import com.example.petmgmt.service.BoardingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardingOrderServiceImpl implements BoardingOrderService {

    private final BoardingOrderMapper boardingOrderMapper;
    private final PetMapper petMapper;
    private final UserMapper userMapper;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional
    public void createOrder(BoardingOrder order) {
        User user = getCurrentUser();
        Pet pet = petMapper.selectById(order.getPetId());
        if (pet == null) throw new BizException(ErrorCode.PET_NOT_FOUND);

        if (user.getRole() == Role.OWNER && !pet.getOwnerId().equals(user.getId())) {
            throw new BizException(ErrorCode.PET_ACCESS_DENIED);
        }

        if (order.getStartDate() != null && order.getEndDate() != null) {
            if (order.getEndDate().isBefore(order.getStartDate())) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "结束日期不能早于开始日期");
            }
            if (order.getStartDate().isBefore(LocalDate.now())) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "开始日期不能早于今天");
            }
        }

        order.setOwnerId(pet.getOwnerId());
        order.setOrderNo("BO" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus(OrderStatus.CREATED);
        boardingOrderMapper.insert(order);
    }

    @Override
    public PageResult<BoardingOrderVO> getOrders(int page, int size, OrderStatus status) {
        User user = getCurrentUser();
        LambdaQueryWrapper<BoardingOrder> query = new LambdaQueryWrapper<>();
        if (user.getRole() == Role.OWNER) {
            query.eq(BoardingOrder::getOwnerId, user.getId());
        }
        if (status != null) {
            query.eq(BoardingOrder::getStatus, status);
        }
        query.orderByDesc(BoardingOrder::getCreatedAt);

        Page<BoardingOrder> orderPage = boardingOrderMapper.selectPage(new Page<>(page, size), query);
        
        java.util.List<BoardingOrderVO> voList = orderPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(java.util.stream.Collectors.toList());
        
        return new PageResult<>(voList, orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize());
    }
    
    private BoardingOrderVO convertToVO(BoardingOrder order) {
        BoardingOrderVO vo = new BoardingOrderVO();
        BeanUtils.copyProperties(order, vo);
        
        Pet pet = petMapper.selectById(order.getPetId());
        if (pet != null) {
            vo.setPetName(pet.getName());
        }
        
        User owner = userMapper.selectById(order.getOwnerId());
        if (owner != null) {
            vo.setOwnerName(owner.getUsername());
        }
        
        return vo;
    }

    @Override
    public BoardingOrder getOrderById(Long id) {
        BoardingOrder order = boardingOrderMapper.selectById(id);
        if (order == null) throw new BizException(ErrorCode.NOT_FOUND);
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER && !order.getOwnerId().equals(user.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, OrderStatus newStatus) {
        BoardingOrder order = getOrderById(id);
        User user = getCurrentUser();
        OrderStatus current = order.getStatus();

        if (newStatus == OrderStatus.CANCELLED) {
            if (current == OrderStatus.COMPLETED) {
                throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
            }
        } else {
            if (user.getRole() == Role.OWNER) {
                throw new BizException(ErrorCode.FORBIDDEN.getCode(), "宠物主人只能取消订单");
            }
            boolean valid = false;
            if (current == OrderStatus.CREATED && newStatus == OrderStatus.CONFIRMED) valid = true;
            else if (current == OrderStatus.CONFIRMED && newStatus == OrderStatus.BOARDING) valid = true;
            else if (current == OrderStatus.BOARDING && newStatus == OrderStatus.COMPLETED) valid = true;

            if (!valid) throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }

        order.setStatus(newStatus);
        if (user.getRole() == Role.STAFF || user.getRole() == Role.ADMIN) {
            order.setStaffId(user.getId());
        }
        boardingOrderMapper.updateById(order);
    }
}
