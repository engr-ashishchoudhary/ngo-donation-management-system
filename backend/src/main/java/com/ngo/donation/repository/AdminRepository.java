package com.ngo.donation.repository;

import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.enums.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Admin> findByRole(AdminRole role);

    List<Admin> findByIsActiveTrue();
}
