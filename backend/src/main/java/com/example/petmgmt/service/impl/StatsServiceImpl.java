package com.example.petmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.entity.VaccineRecord;
import com.example.petmgmt.domain.enums.OrderStatus;
import com.example.petmgmt.mapper.BoardingOrderMapper;
import com.example.petmgmt.mapper.MedicalRecordMapper;
import com.example.petmgmt.mapper.PetMapper;
import com.example.petmgmt.mapper.UserMapper;
import com.example.petmgmt.mapper.VaccineRecordMapper;
import com.example.petmgmt.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final BoardingOrderMapper boardingOrderMapper;
    private final VaccineRecordMapper vaccineRecordMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", userMapper.selectCount(null));
        stats.put("pets", petMapper.selectCount(null));
        stats.put("medicalRecords", medicalRecordMapper.selectCount(null));
                List<BoardingOrder> allOrders = boardingOrderMapper.selectList(null);
                stats.put("orders", allOrders.size());

                Map<String, Long> orderStatusCounts = buildStatusCounts(allOrders);
                stats.put("orderStatusCounts", orderStatusCounts);
                stats.put("createdOrders", orderStatusCounts.get(OrderStatus.CREATED.name()));
                long processingOrders = orderStatusCounts.get(OrderStatus.CONFIRMED.name()) + orderStatusCounts.get(OrderStatus.BOARDING.name());
                stats.put("processingOrders", processingOrders);
                stats.put("completedOrders", orderStatusCounts.get(OrderStatus.COMPLETED.name()));
                stats.put("cancelledOrders", orderStatusCounts.get(OrderStatus.CANCELLED.name()));

        LocalDate today = LocalDate.now();
        stats.put("todayOrders", boardingOrderMapper.selectCount(
                new LambdaQueryWrapper<BoardingOrder>().eq(BoardingOrder::getStartDate, today)));

        LocalDate future = today.plusDays(30);
        stats.put("vaccineDueIn30Days", vaccineRecordMapper.selectCount(
                new LambdaQueryWrapper<VaccineRecord>()
                        .ge(VaccineRecord::getNextDueDate, today)
                        .le(VaccineRecord::getNextDueDate, future)
        ));
        return stats;
    }

    @Override
    public Map<String, Object> getBoardingStats(LocalDate from, LocalDate to) {
        List<BoardingOrder> orders = boardingOrderMapper.selectList(
                new LambdaQueryWrapper<BoardingOrder>()
                        .ge(BoardingOrder::getStartDate, from)
                        .le(BoardingOrder::getStartDate, to)
                        .orderByDesc(BoardingOrder::getCreatedAt)
        );

        BigDecimal totalRevenue = orders.stream()
                .map(BoardingOrder::getPriceTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("count", orders.size());
        stats.put("totalRevenue", totalRevenue);

                Map<String, Long> statusCounts = buildStatusCounts(orders);
                long processingCount = statusCounts.get(OrderStatus.CONFIRMED.name()) + statusCounts.get(OrderStatus.BOARDING.name());
                long completedCount = statusCounts.get(OrderStatus.COMPLETED.name());
                long cancelledCount = statusCounts.get(OrderStatus.CANCELLED.name());

                stats.put("statusCounts", statusCounts);
                stats.put("processingCount", processingCount);
                stats.put("completedCount", completedCount);
                stats.put("cancelledCount", cancelledCount);
                stats.put("completionRate", calculateRate(completedCount, orders.size()));
                stats.put("cancellationRate", calculateRate(cancelledCount, orders.size()));
                stats.put("orders", orders);
        return stats;
    }

    @Override
    public Map<String, Object> getVaccineDueStats(int days) {
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(days);

        List<VaccineRecord> list = vaccineRecordMapper.selectList(
                new LambdaQueryWrapper<VaccineRecord>()
                        .ge(VaccineRecord::getNextDueDate, today)
                        .le(VaccineRecord::getNextDueDate, future)
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("dueCount", list.size());
        stats.put("list", list);
        return stats;
    }

        private Map<String, Long> buildStatusCounts(List<BoardingOrder> orders) {
                Map<String, Long> grouped = orders.stream()
                                .filter(Objects::nonNull)
                                .filter(order -> order.getStatus() != null)
                                .collect(Collectors.groupingBy(order -> order.getStatus().name(), Collectors.counting()));

                Map<String, Long> fullStatus = new HashMap<>();
                for (OrderStatus status : OrderStatus.values()) {
                        fullStatus.put(status.name(), grouped.getOrDefault(status.name(), 0L));
                }
                return fullStatus;
        }

        private BigDecimal calculateRate(long part, int total) {
                if (total <= 0) {
                        return BigDecimal.ZERO;
                }
                return BigDecimal.valueOf(part)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }
}
