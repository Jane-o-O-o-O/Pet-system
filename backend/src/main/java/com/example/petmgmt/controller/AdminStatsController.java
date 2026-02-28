package com.example.petmgmt.controller;

import com.example.petmgmt.common.ApiResponse;
import com.example.petmgmt.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        return ApiResponse.success(statsService.getOverview());
    }

    @GetMapping("/boarding")
    public ApiResponse<Map<String, Object>> getBoardingStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(statsService.getBoardingStats(from, to));
    }

    @GetMapping("/vaccine-due")
    public ApiResponse<Map<String, Object>> getVaccineDueStats(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(statsService.getVaccineDueStats(days));
    }
}
