package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.enums.OrderStatus;

public interface BoardingOrderService {
    void createOrder(BoardingOrder order);
    PageResult<BoardingOrder> getOrders(int page, int size, OrderStatus status);
    BoardingOrder getOrderById(Long id);
    void updateStatus(Long id, OrderStatus status);
}
