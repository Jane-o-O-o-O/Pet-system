package com.example.petmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.BoardingOrder;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.entity.VaccineRecord;
import com.example.petmgmt.domain.enums.OrderStatus;
import com.example.petmgmt.mapper.BoardingOrderMapper;
import com.example.petmgmt.mapper.PetMapper;
import com.example.petmgmt.mapper.UserMapper;
import com.example.petmgmt.mapper.VaccineRecordMapper;
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

    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final BoardingOrderMapper boardingOrderMapper;
    private final VaccineRecordMapper vaccineRecordMapper;

    @Override
    public Map<String, Object> getOwnerOverview() {
        User user = getCurrentUser();
        if (user == null) return emptyOwner();
        Long ownerId = user.getId();

        long petCount = petMapper.selectCount(new LambdaQueryWrapper<Pet>().eq(Pet::getOwnerId, ownerId));
        List<Pet> myPets = petMapper.selectList(new LambdaQueryWrapper<Pet>().eq(Pet::getOwnerId, ownerId));
        List<Long> petIds = myPets.stream().map(Pet::getId).collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(30);
        long vaccineRemindCount = 0;
        if (!petIds.isEmpty()) {
            vaccineRemindCount = vaccineRecordMapper.selectCount(
                    new LambdaQueryWrapper<VaccineRecord>()
                            .in(VaccineRecord::getPetId, petIds)
                            .ge(VaccineRecord::getNextDueDate, today)
                            .le(VaccineRecord::getNextDueDate, future)
            );
        }

        long created = boardingOrderMapper.selectCount(new LambdaQueryWrapper<BoardingOrder>()
                .eq(BoardingOrder::getOwnerId, ownerId).eq(BoardingOrder::getStatus, OrderStatus.CREATED));
        long confirmed = boardingOrderMapper.selectCount(new LambdaQueryWrapper<BoardingOrder>()
                .eq(BoardingOrder::getOwnerId, ownerId).eq(BoardingOrder::getStatus, OrderStatus.CONFIRMED));
        long boarding = boardingOrderMapper.selectCount(new LambdaQueryWrapper<BoardingOrder>()
                .eq(BoardingOrder::getOwnerId, ownerId).eq(BoardingOrder::getStatus, OrderStatus.BOARDING));
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

        long todayCheckIn = boardingOrderMapper.selectCount(
                new LambdaQueryWrapper<BoardingOrder>().eq(BoardingOrder::getStartDate, today));
        long pendingConfirm = boardingOrderMapper.selectCount(
                new LambdaQueryWrapper<BoardingOrder>().eq(BoardingOrder::getStatus, OrderStatus.CREATED));
        long boardingNow = boardingOrderMapper.selectCount(
                new LambdaQueryWrapper<BoardingOrder>().eq(BoardingOrder::getStatus, OrderStatus.BOARDING));

        Map<String, Object> map = new HashMap<>();
        map.put("todayCheckIn", todayCheckIn);
        map.put("pendingConfirm", pendingConfirm);
        map.put("boardingNow", boardingNow);
        return map;
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return username == null ? null : userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
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
