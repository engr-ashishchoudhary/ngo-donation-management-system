package com.ngo.donation.repository;

import com.ngo.donation.entity.Campaign;
import com.ngo.donation.entity.Donation;
import com.ngo.donation.entity.Donor;
import com.ngo.donation.entity.enums.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Integer> {

    List<Donation> findByDonor(Donor donor);

    List<Donation> findByDonor_DonorId(Integer donorId);

    List<Donation> findByCampaign(Campaign campaign);

    List<Donation> findByCampaign_CampaignId(Integer campaignId);

    List<Donation> findByStatus(DonationStatus status);

    List<Donation> findByCampaign_CampaignIdAndStatus(Integer campaignId, DonationStatus status);

    Optional<Donation> findByTransactionRef(String transactionRef);

    boolean existsByTransactionRef(String transactionRef);
}
