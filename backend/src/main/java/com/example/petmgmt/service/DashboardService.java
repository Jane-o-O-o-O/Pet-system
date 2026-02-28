package com.example.petmgmt.service;

import java.util.Map;

public interface DashboardService {
    /** 宠物主人首页概览 */
    Map<String, Object> getOwnerOverview();
    /** 服务人员首页概览 */
    Map<String, Object> getStaffOverview();
}
