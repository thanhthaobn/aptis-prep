package com.aptisprep.mvc.student.service;

import com.aptisprep.mvc.student.dto.AdminDashboardViewDTO;

public interface AdminDashboardService {
    AdminDashboardViewDTO dashboard(String adminEmail);
}
