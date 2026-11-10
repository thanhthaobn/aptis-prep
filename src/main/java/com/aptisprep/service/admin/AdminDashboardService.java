package com.aptisprep.service.admin;

import com.aptisprep.dto.view.AdminDashboardView;

public interface AdminDashboardService {
    AdminDashboardView dashboard(String adminEmail);
}
