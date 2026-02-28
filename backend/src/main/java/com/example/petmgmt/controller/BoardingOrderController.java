package com.example.petmgmt.controller;

import com.example.petmgmt.common.ApiResponse;
import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.enums.OrderStatus;
import com.example.petmgmt.service.BoardingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/boarding-orders")
@RequiredArgsConstructor
public class BoardingOrderController {

    private final BoardingOrderService boardingOrderService;

    @PostMapping
    public ApiResponse<Void> createOrder(@RequestBody BoardingOrder order) {
        boardingOrderService.createOrder(order);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<PageResult<BoardingOrder>> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status) {
        return ApiResponse.success(boardingOrderService.getOrders(page, size, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<BoardingOrder> getOrderById(@PathVariable Long id) {
        return ApiResponse.success(boardingOrderService.getOrderById(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "状态参数不能为空");
        }
        try {
            boardingOrderService.updateStatus(id, OrderStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "无效的订单状态: " + statusStr);
        }
        return ApiResponse.success();
    }
}
