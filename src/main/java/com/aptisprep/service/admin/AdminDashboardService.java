package com.aptisprep.service.admin;

import com.aptisprep.dto.AdminDashboardViewDTO;

public interface AdminDashboardService {
    public AdminDashboardViewDTO dashboard(String adminEmail);
}
