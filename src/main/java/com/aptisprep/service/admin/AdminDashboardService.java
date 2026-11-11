package com.aptisprep.service.admin;

import com.aptisprep.presentation.AdminDashboardViewDTO;

public interface AdminDashboardService {
    AdminDashboardViewDTO dashboard(String adminEmail);
}
