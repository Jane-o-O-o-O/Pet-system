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

    @Override
    public Map<String, Object> getOperationalStats() {
        Map<String, Object> stats = new HashMap<>();

        // 疫苗库存统计（按疫苗名称分组计数）
        List<VaccineRecord> allVaccines = vaccineRecordMapper.selectList(null);
        Map<String, Long> vaccineInventory = allVaccines.stream()
                .filter(v -> v.getVaccineName() != null)
                .collect(Collectors.groupingBy(VaccineRecord::getVaccineName, Collectors.counting()));
        stats.put("vaccineInventory", vaccineInventory);
        stats.put("totalVaccineRecords", allVaccines.size());

        // 当前寄养中的订单（CONFIRMED 和 BOARDING 状态）
        List<BoardingOrder> activeBoardings = boardingOrderMapper.selectList(
                new LambdaQueryWrapper<BoardingOrder>()
                        .in(BoardingOrder::getStatus, OrderStatus.CONFIRMED, OrderStatus.BOARDING)
        );

        // 按房型统计当前使用数量
        Map<String, Long> roomUsage = activeBoardings.stream()
                .filter(o -> o.getRoomType() != null)
                .collect(Collectors.groupingBy(BoardingOrder::getRoomType, Collectors.counting()));

        // 房型容量配置（可以从 sys_config 读取，这里先硬编码）
        Map<String, Integer> roomCapacity = new HashMap<>();
        roomCapacity.put("Standard", 20);
        roomCapacity.put("Deluxe", 10);
        roomCapacity.put("Suite", 5);

        // 计算剩余空位
        Map<String, Map<String, Object>> roomStats = new HashMap<>();
        for (Map.Entry<String, Integer> entry : roomCapacity.entrySet()) {
            String roomType = entry.getKey();
            int capacity = entry.getValue();
            long used = roomUsage.getOrDefault(roomType, 0L);
            long available = Math.max(0, capacity - used);

            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("capacity", capacity);
            roomInfo.put("used", used);
            roomInfo.put("available", available);
            roomInfo.put("usageRate", calculateRate(used, capacity));
            roomStats.put(roomType, roomInfo);
        }

        stats.put("roomStats", roomStats);
        stats.put("totalActiveBoardings", activeBoardings.size());

        // 当前订单状态分布
        List<BoardingOrder> allOrders = boardingOrderMapper.selectList(null);
        Map<String, Long> statusDistribution = buildStatusCounts(allOrders);
        stats.put("statusDistribution", statusDistribution);

        return stats;
    }
