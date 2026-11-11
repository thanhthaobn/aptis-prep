package com.aptisprep.service.admin;

import com.aptisprep.dto.view.AdminDashboardViewDTO;

public interface AdminDashboardService {
    AdminDashboardViewDTO dashboard(String adminEmail);
}
