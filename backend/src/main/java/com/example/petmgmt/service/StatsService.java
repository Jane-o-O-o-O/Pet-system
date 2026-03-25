package com.example.petmgmt.service;

import java.time.LocalDate;
import java.util.Map;

public interface StatsService {
    Map<String, Object> getOverview();
    Map<String, Object> getBoardingStats(LocalDate from, LocalDate to);
    Map<String, Object> getVaccineDueStats(int days);
    Map<String, Object> getOperationalStats();
    String exportReport(LocalDate from, LocalDate to);
}
