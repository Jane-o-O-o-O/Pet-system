package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.enums.OrderStatus;
import com.example.petmgmt.domain.vo.BoardingOrderVO;

public interface BoardingOrderService {
    void createOrder(BoardingOrder order);
    PageResult<BoardingOrderVO> getOrders(int page, int size, OrderStatus status);
    BoardingOrder getOrderById(Long id);
    void updateStatus(Long id, OrderStatus status);
}
