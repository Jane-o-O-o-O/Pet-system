package com.example.petmgmt.service.impl;

import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.entity.MedicalRecord;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.RegistrationAppointment;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.entity.VaccineRecord;
import com.example.petmgmt.domain.enums.OrderStatus;
import com.example.petmgmt.domain.vo.BoardingOrderVO;
import com.example.petmgmt.repository.BoardingOrderRepository;
import com.example.petmgmt.repository.MedicalRecordRepository;
import com.example.petmgmt.repository.PetRepository;
import com.example.petmgmt.repository.RegistrationAppointmentRepository;
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
    private final RegistrationAppointmentRepository registrationAppointmentRepository;

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

    @Override
    public String exportReport(LocalDate from, LocalDate to) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');

        appendSection(csv, "概览统计", List.of(
                List.of("指标", "数值"),
                List.of("用户总数", stringify(userRepository.count(user -> true))),
                List.of("宠物总数", stringify(petRepository.count(pet -> true))),
                List.of("医疗记录总数", stringify(medicalRecordRepository.count(record -> true))),
                List.of("寄养订单总数", stringify(boardingOrderRepository.count(order -> true))),
                List.of("挂号预约总数", stringify(registrationAppointmentRepository.findAll().size())),
                List.of("统计区间", from + " 至 " + to)
        ));

        List<BoardingOrder> orders = boardingOrderRepository.findAll(order ->
                order.getStartDate() != null
                        && !order.getStartDate().isBefore(from)
                        && !order.getStartDate().isAfter(to));
        appendSection(csv, "寄养订单明细", buildBoardingRows(orders));

        List<VaccineRecord> vaccines = vaccineRecordRepository.findAll(record ->
                record.getShotDate() != null
                        && !record.getShotDate().isBefore(from)
                        && !record.getShotDate().isAfter(to));
        appendSection(csv, "疫苗记录明细", buildVaccineRows(vaccines));

        List<MedicalRecord> medicalRecords = medicalRecordRepository.findAll(record ->
                record.getVisitDate() != null
                        && !record.getVisitDate().toLocalDate().isBefore(from)
                        && !record.getVisitDate().toLocalDate().isAfter(to));
        appendSection(csv, "医疗用药明细", buildMedicalRows(medicalRecords));

        List<RegistrationAppointment> appointments = registrationAppointmentRepository.findAll(item ->
                item.getAppointmentTime() != null
                        && !item.getAppointmentTime().toLocalDate().isBefore(from)
                        && !item.getAppointmentTime().toLocalDate().isAfter(to));
        appendSection(csv, "挂号预约明细", buildAppointmentRows(appointments));
        return csv.toString();
    }

    private List<List<String>> buildBoardingRows(List<BoardingOrder> orders) {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("订单号", "宠物", "主人", "开始日期", "结束日期", "房型", "总价", "状态", "备注"));
        orders.stream()
                .sorted(Comparator.comparing(BoardingOrder::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(order -> rows.add(List.of(
                        stringify(order.getOrderNo()),
                        petName(order.getPetId()),
                        userName(order.getOwnerId()),
                        stringify(order.getStartDate()),
                        stringify(order.getEndDate()),
                        stringify(order.getRoomType()),
                        stringify(order.getPriceTotal()),
                        stringify(order.getStatus()),
                        stringify(order.getRemark())
                )));
        return rows;
    }

    private List<List<String>> buildVaccineRows(List<VaccineRecord> vaccines) {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("宠物", "主人", "疫苗名称", "接种日期", "下次接种日期", "提醒状态"));
        vaccines.stream()
                .sorted(Comparator.comparing(VaccineRecord::getShotDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(record -> {
                    Long ownerId = petOwnerId(record.getPetId());
                    rows.add(List.of(
                            petName(record.getPetId()),
                            userName(ownerId),
                            stringify(record.getVaccineName()),
                            stringify(record.getShotDate()),
                            stringify(record.getNextDueDate()),
                            stringify(record.getRemindStatus())
                    ));
                });
        return rows;
    }

    private List<List<String>> buildMedicalRows(List<MedicalRecord> medicalRecords) {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("宠物", "主人", "就诊时间", "医生", "症状", "诊断", "治疗/用药"));
        medicalRecords.stream()
                .sorted(Comparator.comparing(MedicalRecord::getVisitDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(record -> {
                    Long ownerId = petOwnerId(record.getPetId());
                    rows.add(List.of(
                            petName(record.getPetId()),
                            userName(ownerId),
                            stringify(record.getVisitDate()),
                            stringify(record.getDoctorName()),
                            stringify(record.getComplaint()),
                            stringify(record.getDiagnosis()),
                            stringify(record.getTreatment())
                    ));
                });
        return rows;
    }

    private List<List<String>> buildAppointmentRows(List<RegistrationAppointment> appointments) {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("预约号", "宠物", "主人", "预约时间", "状态", "症状", "处理结果", "处理人", "备注"));
        appointments.stream()
                .sorted(Comparator.comparing(RegistrationAppointment::getAppointmentTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(item -> rows.add(List.of(
                        stringify(item.getAppointmentNo()),
                        petName(item.getPetId()),
                        userName(item.getOwnerId()),
                        stringify(item.getAppointmentTime()),
                        stringify(item.getStatus()),
                        stringify(item.getSymptom()),
                        stringify(item.getResult()),
                        userName(item.getStaffId()),
                        stringify(item.getRemark())
                )));
        return rows;
    }

    private void appendSection(StringBuilder csv, String title, List<List<String>> rows) {
        csv.append(escapeCsv(title)).append('\n');
        rows.forEach(row -> csv.append(row.stream().map(this::escapeCsv).collect(Collectors.joining(","))).append('\n'));
        csv.append('\n');
    }

    private String escapeCsv(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String petName(Long petId) {
        return petRepository.findById(petId).map(Pet::getName).orElse("");
    }

    private Long petOwnerId(Long petId) {
        return petRepository.findById(petId).map(Pet::getOwnerId).orElse(null);
    }

    private String userName(Long userId) {
        if (userId == null) {
            return "";
        }
        return userRepository.findById(userId).map(User::getUsername).orElse("");
    }
}
