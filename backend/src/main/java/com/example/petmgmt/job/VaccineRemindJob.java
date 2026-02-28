package com.example.petmgmt.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.VaccineRecord;
import com.example.petmgmt.domain.enums.RemindStatus;
import com.example.petmgmt.mapper.PetMapper;
import com.example.petmgmt.mapper.VaccineRecordMapper;
import com.example.petmgmt.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class VaccineRemindJob {

    private final VaccineRecordMapper vaccineRecordMapper;
    private final PetMapper petMapper;
    private final NotifyService notifyService;

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void run() {
        log.info("Starting vaccine reminder job...");

        LocalDate today = LocalDate.now();
        LocalDate maxFuture = today.plusDays(30);

        List<VaccineRecord> pendingRecords = vaccineRecordMapper.selectList(
                new LambdaQueryWrapper<VaccineRecord>()
                        .eq(VaccineRecord::getRemindStatus, RemindStatus.PENDING)
                        .le(VaccineRecord::getNextDueDate, maxFuture)
        );

        int count = 0;
        for (VaccineRecord vr : pendingRecords) {
            int remindDays = vr.getRemindDaysBefore() != null ? vr.getRemindDaysBefore() : 7;
            LocalDate remindDate = vr.getNextDueDate().minusDays(remindDays);

            if (!today.isBefore(remindDate) && !today.isAfter(vr.getNextDueDate())) {
                Pet pet = petMapper.selectById(vr.getPetId());
                if (pet != null) {
                    String title = "疫苗到期提醒: " + pet.getName();
                    String content = String.format("您的宠物 %s 的疫苗 [%s] 将于 %s 到期，请及时安排接种。",
                            pet.getName(), vr.getVaccineName(), vr.getNextDueDate());

                    notifyService.createNotification(pet.getOwnerId(), title, content);

                    vr.setRemindStatus(RemindStatus.SENT);
                    vaccineRecordMapper.updateById(vr);
                    count++;
                    log.info("Sent reminder for pet {} vaccine {}", pet.getName(), vr.getVaccineName());
                }
            }
        }
        log.info("Vaccine reminder job finished. Sent {} reminders.", count);
    }
}
