package com.ngo.donation.repository;

import com.ngo.donation.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Integer> {

    Optional<Donor> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Donor> findByIsActiveTrue();
}
