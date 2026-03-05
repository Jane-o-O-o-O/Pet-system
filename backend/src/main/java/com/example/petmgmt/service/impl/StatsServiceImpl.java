package com.example.petmgmt.service.impl;

import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.entity.VaccineRecord;
import com.example.petmgmt.domain.enums.OrderStatus;
import com.example.petmgmt.domain.vo.BoardingOrderVO;
import com.example.petmgmt.repository.BoardingOrderRepository;
import com.example.petmgmt.repository.MedicalRecordRepository;
import com.example.petmgmt.repository.PetRepository;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.repository.VaccineRecordRepository;
import com.example.petmgmt.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final BoardingOrderRepository boardingOrderRepository;
    private final VaccineRecordRepository vaccineRecordRepository;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", userRepository.count(user -> true));
        stats.put("pets", petRepository.count(pet -> true));
        stats.put("medicalRecords", medicalRecordRepository.count(record -> true));
                List<BoardingOrder> allOrders = boardingOrderRepository.findAll(order -> true);
                stats.put("orders", allOrders.size());

                Map<String, Long> orderStatusCounts = buildStatusCounts(allOrders);
                stats.put("orderStatusCounts", orderStatusCounts);
                stats.put("createdOrders", orderStatusCounts.get(OrderStatus.CREATED.name()));
                long processingOrders = orderStatusCounts.get(OrderStatus.CONFIRMED.name()) + orderStatusCounts.get(OrderStatus.BOARDING.name());
                stats.put("processingOrders", processingOrders);
                stats.put("completedOrders", orderStatusCounts.get(OrderStatus.COMPLETED.name()));
                stats.put("cancelledOrders", orderStatusCounts.get(OrderStatus.CANCELLED.name()));

        LocalDate today = LocalDate.now();
        stats.put("todayOrders", boardingOrderRepository.count(
                order -> order.getStartDate() != null && order.getStartDate().equals(today)));

        LocalDate future = today.plusDays(30);
        stats.put("vaccineDueIn30Days", vaccineRecordRepository.count(
                record -> record.getNextDueDate() != null
                        && !record.getNextDueDate().isBefore(today)
                        && !record.getNextDueDate().isAfter(future)
        ));
        return stats;
    }

    @Override
    public Map<String, Object> getBoardingStats(LocalDate from, LocalDate to) {
        List<BoardingOrder> orders = boardingOrderRepository.findAll(
                order -> order.getStartDate() != null
                        && !order.getStartDate().isBefore(from)
                        && !order.getStartDate().isAfter(to)
        ).stream()
                .sorted(Comparator.comparing(BoardingOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

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
                
                List<BoardingOrderVO> orderVOs = orders.stream()
                        .map(this::convertToVO)
                        .collect(Collectors.toList());
                stats.put("orders", orderVOs);
        return stats;
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
    public Map<String, Object> getVaccineDueStats(int days) {
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(days);

        List<VaccineRecord> list = vaccineRecordRepository.findAll(
                record -> record.getNextDueDate() != null
                        && !record.getNextDueDate().isBefore(today)
                        && !record.getNextDueDate().isAfter(future)
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
        List<VaccineRecord> allVaccines = vaccineRecordRepository.findAll(record -> true);
        Map<String, Long> vaccineInventory = allVaccines.stream()
                .filter(v -> v.getVaccineName() != null)
                .collect(Collectors.groupingBy(VaccineRecord::getVaccineName, Collectors.counting()));
        stats.put("vaccineInventory", vaccineInventory);
        stats.put("totalVaccineRecords", allVaccines.size());

        // 当前寄养中的订单（CONFIRMED 和 BOARDING 状态）
        List<BoardingOrder> activeBoardings = boardingOrderRepository.findAll(
                order -> order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.BOARDING
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
        List<BoardingOrder> allOrders = boardingOrderRepository.findAll(order -> true);
        Map<String, Long> statusDistribution = buildStatusCounts(allOrders);
        stats.put("statusDistribution", statusDistribution);

        return stats;
    }
}
