package com.example.petmgmt.service;

import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.RegistrationAppointment;
import com.example.petmgmt.domain.enums.AppointmentStatus;
import com.example.petmgmt.domain.vo.RegistrationAppointmentVO;

public interface RegistrationAppointmentService {
    void createAppointment(RegistrationAppointment appointment);

    PageResult<RegistrationAppointmentVO> getAppointments(int page, int size, AppointmentStatus status);

    RegistrationAppointment getAppointmentById(Long id);

    void updateAppointment(Long id, RegistrationAppointment appointment);
}
