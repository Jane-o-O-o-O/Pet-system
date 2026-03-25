package com.example.petmgmt.controller;

import com.example.petmgmt.common.ApiResponse;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.domain.entity.RegistrationAppointment;
import com.example.petmgmt.domain.enums.AppointmentStatus;
import com.example.petmgmt.domain.vo.RegistrationAppointmentVO;
import com.example.petmgmt.service.RegistrationAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registration-appointments")
@RequiredArgsConstructor
public class RegistrationAppointmentController {

    private final RegistrationAppointmentService registrationAppointmentService;

    @PostMapping
    public ApiResponse<Void> createAppointment(@RequestBody RegistrationAppointment appointment) {
        registrationAppointmentService.createAppointment(appointment);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<PageResult<RegistrationAppointmentVO>> getAppointments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AppointmentStatus status) {
        return ApiResponse.success(registrationAppointmentService.getAppointments(page, size, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<RegistrationAppointment> getAppointment(@PathVariable Long id) {
        return ApiResponse.success(registrationAppointmentService.getAppointmentById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateAppointment(@PathVariable Long id, @RequestBody RegistrationAppointment appointment) {
        registrationAppointmentService.updateAppointment(id, appointment);
        return ApiResponse.success();
    }
}
