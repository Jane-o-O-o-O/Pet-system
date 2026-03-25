package com.example.petmgmt.service.impl;

import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.entity.Pet;
import com.example.petmgmt.domain.entity.RegistrationAppointment;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.AppointmentStatus;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.domain.vo.RegistrationAppointmentVO;
import com.example.petmgmt.repository.PetRepository;
import com.example.petmgmt.repository.RegistrationAppointmentRepository;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.service.RegistrationAppointmentService;
import com.example.petmgmt.storage.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationAppointmentServiceImpl implements RegistrationAppointmentService {

    private final RegistrationAppointmentRepository registrationAppointmentRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    private Pet getPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new BizException(ErrorCode.PET_NOT_FOUND));
    }

    @Override
    public void createAppointment(RegistrationAppointment appointment) {
        User user = getCurrentUser();
        if (user.getRole() != Role.OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "Only owners can create registration appointments");
        }

        Pet pet = getPet(appointment.getPetId());
        if (!pet.getOwnerId().equals(user.getId())) {
            throw new BizException(ErrorCode.PET_ACCESS_DENIED);
        }
        if (appointment.getAppointmentTime() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Appointment time is required");
        }
        if (appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Appointment time cannot be earlier than now");
        }

        appointment.setOwnerId(user.getId());
        appointment.setAppointmentNo("RA" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setResult(null);
        registrationAppointmentRepository.save(appointment);
    }

    @Override
    public PageResult<RegistrationAppointmentVO> getAppointments(int page, int size, AppointmentStatus status) {
        User user = getCurrentUser();
        var appointments = registrationAppointmentRepository.findAll(item -> {
                    if (user.getRole() == Role.OWNER && !item.getOwnerId().equals(user.getId())) {
                        return false;
                    }
                    return status == null || item.getStatus() == status;
                }).stream()
                .sorted(Comparator.comparing(RegistrationAppointment::getAppointmentTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        PageHelper.PageData<RegistrationAppointment> pageData = PageHelper.paginate(appointments, page, size);
        var records = pageData.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }

    @Override
    public RegistrationAppointment getAppointmentById(Long id) {
        RegistrationAppointment appointment = registrationAppointmentRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        User user = getCurrentUser();
        if (user.getRole() == Role.OWNER && !appointment.getOwnerId().equals(user.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return appointment;
    }

    @Override
    public void updateAppointment(Long id, RegistrationAppointment payload) {
        RegistrationAppointment appointment = getAppointmentById(id);
        User user = getCurrentUser();

        if (user.getRole() == Role.OWNER) {
            if (payload.getStatus() != AppointmentStatus.CANCELLED) {
                throw new BizException(ErrorCode.FORBIDDEN.getCode(), "Owners can only cancel appointments");
            }
            if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "Completed appointment cannot be cancelled");
            }
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setRemark(payload.getRemark());
            registrationAppointmentRepository.save(appointment);
            return;
        }

        if (payload.getAppointmentTime() != null) {
            appointment.setAppointmentTime(payload.getAppointmentTime());
        }
        appointment.setStatus(payload.getStatus() == null ? appointment.getStatus() : payload.getStatus());
        appointment.setResult(payload.getResult());
        appointment.setRemark(payload.getRemark());
        appointment.setSymptom(payload.getSymptom());
        appointment.setStaffId(user.getId());
        registrationAppointmentRepository.save(appointment);
    }

    private RegistrationAppointmentVO convertToVO(RegistrationAppointment appointment) {
        RegistrationAppointmentVO vo = new RegistrationAppointmentVO();
        BeanUtils.copyProperties(appointment, vo);

        petRepository.findById(appointment.getPetId()).ifPresent(pet -> vo.setPetName(pet.getName()));
        userRepository.findById(appointment.getOwnerId()).ifPresent(owner -> vo.setOwnerName(owner.getUsername()));
        if (appointment.getStaffId() != null) {
            userRepository.findById(appointment.getStaffId()).ifPresent(staff -> vo.setStaffName(staff.getUsername()));
        }
        return vo;
    }
}
