package com.ngo.donation.repository;

import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.Campaign;
import com.ngo.donation.entity.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Integer> {

    List<Campaign> findByStatus(CampaignStatus status);

    boolean existsByName(String name);

    Optional<Campaign> findByName(String name);

    List<Campaign> findByAdmin(Admin admin);

    List<Campaign> findByAdmin_AdminId(Integer adminId);
}
