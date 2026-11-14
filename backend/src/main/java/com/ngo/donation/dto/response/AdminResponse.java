package com.ngo.donation.dto.response;

import com.ngo.donation.entity.enums.AdminRole;

import java.time.LocalDateTime;

public class AdminResponse {

    private Integer adminId;
    private String name;
    private String email;
    private AdminRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public AdminResponse() {
    }

    public AdminResponse(Integer adminId, String name, String email, AdminRole role,
                         Boolean isActive, LocalDateTime createdAt) {
        this.adminId = adminId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
