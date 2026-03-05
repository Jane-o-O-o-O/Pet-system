package com.example.petmgmt.service.impl;

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
import com.example.petmgmt.repository.BoardingOrderRepository;
import com.example.petmgmt.repository.PetRepository;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.service.BoardingOrderService;
import com.example.petmgmt.storage.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardingOrderServiceImpl implements BoardingOrderService {

    private final BoardingOrderRepository boardingOrderRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void createOrder(BoardingOrder order) {
        User user = getCurrentUser();
        Pet pet = petRepository.findById(order.getPetId())
                .orElseThrow(() -> new BizException(ErrorCode.PET_NOT_FOUND));

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
        boardingOrderRepository.save(order);
    }

    @Override
    public PageResult<BoardingOrderVO> getOrders(int page, int size, OrderStatus status) {
        User user = getCurrentUser();
        
        var orders = boardingOrderRepository.findAll(order -> {
            if (user.getRole() == Role.OWNER && !order.getOwnerId().equals(user.getId())) {
                return false;
            }
            if (status != null && order.getStatus() != status) {
                return false;
            }
            return true;
        }).stream()
            .sorted(Comparator.comparing(BoardingOrder::getCreatedAt).reversed())
            .collect(Collectors.toList());
        
        PageHelper.PageData<BoardingOrder> pageData = PageHelper.paginate(orders, page, size);
        
        var voList = pageData.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return new PageResult<>(voList, pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }
    
    private BoardingOrderVO convertToVO(BoardingOrder order) {
        BoardingOrderVO vo = new BoardingOrderVO();
        BeanUtils.copyProperties(order, vo);
        
        petRepository.findById(order.getPetId()).ifPresent(pet -> {
            vo.setPetName(pet.getName());
        });
        
        userRepository.findById(order.getOwnerId()).ifPresent(owner -> {
            vo.setOwnerName(owner.getUsername());
        });
        
        return vo;
    }

    @Override
    public BoardingOrder getOrderById(Long id) {
        BoardingOrder order = boardingOrderRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER && !order.getOwnerId().equals(user.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    @Override
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
        boardingOrderRepository.save(order);
    }
}
