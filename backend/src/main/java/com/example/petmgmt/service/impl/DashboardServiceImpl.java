package com.example.petmgmt.service.impl;

import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.entity.VaccineRecord;
import com.example.petmgmt.domain.enums.OrderStatus;
import com.example.petmgmt.repository.BoardingOrderRepository;
import com.example.petmgmt.repository.PetRepository;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.repository.VaccineRecordRepository;
import com.example.petmgmt.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final BoardingOrderRepository boardingOrderRepository;
    private final VaccineRecordRepository vaccineRecordRepository;

    @Override
    public Map<String, Object> getOwnerOverview() {
        User user = getCurrentUser();
        if (user == null) return emptyOwner();
        Long ownerId = user.getId();

        long petCount = petRepository.count(pet -> pet.getOwnerId().equals(ownerId));
        List<Pet> myPets = petRepository.findAll(pet -> pet.getOwnerId().equals(ownerId));
        List<Long> petIds = myPets.stream().map(Pet::getId).collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(30);
        long vaccineRemindCount = 0;
        if (!petIds.isEmpty()) {
            vaccineRemindCount = vaccineRecordRepository.count(
                    record -> petIds.contains(record.getPetId())
                            && record.getNextDueDate() != null
                            && !record.getNextDueDate().isBefore(today)
                            && !record.getNextDueDate().isAfter(future)
            );
        }

        long created = boardingOrderRepository.count(order -> 
                order.getOwnerId().equals(ownerId) && order.getStatus() == OrderStatus.CREATED);
        long confirmed = boardingOrderRepository.count(order -> 
                order.getOwnerId().equals(ownerId) && order.getStatus() == OrderStatus.CONFIRMED);
        long boarding = boardingOrderRepository.count(order -> 
                order.getOwnerId().equals(ownerId) && order.getStatus() == OrderStatus.BOARDING);
        long activeOrderCount = created + confirmed + boarding;

        Map<String, Object> map = new HashMap<>();
        map.put("petCount", petCount);
        map.put("vaccineRemindCount", vaccineRemindCount);
        map.put("activeOrderCount", activeOrderCount);
        return map;
    }

    @Override
    public Map<String, Object> getStaffOverview() {
        User user = getCurrentUser();
        if (user == null) return emptyStaff();
        LocalDate today = LocalDate.now();

        long todayCheckIn = boardingOrderRepository.count(
                order -> order.getStartDate() != null && order.getStartDate().equals(today));
        long pendingConfirm = boardingOrderRepository.count(
                order -> order.getStatus() == OrderStatus.CREATED);
        long boardingNow = boardingOrderRepository.count(
                order -> order.getStatus() == OrderStatus.BOARDING);

        Map<String, Object> map = new HashMap<>();
        map.put("todayCheckIn", todayCheckIn);
        map.put("pendingConfirm", pendingConfirm);
        map.put("boardingNow", boardingNow);
        return map;
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return username == null ? null : userRepository.findByUsername(username).orElse(null);
    }

    private static Map<String, Object> emptyOwner() {
        Map<String, Object> m = new HashMap<>();
        m.put("petCount", 0L);
        m.put("vaccineRemindCount", 0L);
        m.put("activeOrderCount", 0L);
        return m;
    }

    private static Map<String, Object> emptyStaff() {
        Map<String, Object> m = new HashMap<>();
        m.put("todayCheckIn", 0L);
        m.put("pendingConfirm", 0L);
        m.put("boardingNow", 0L);
        return m;
    }
}
