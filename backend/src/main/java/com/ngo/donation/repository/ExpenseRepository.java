package com.ngo.donation.repository;

import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.Campaign;
import com.ngo.donation.entity.Expense;
import com.ngo.donation.entity.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    List<Expense> findByCampaign(Campaign campaign);

    List<Expense> findByCampaign_CampaignId(Integer campaignId);

    List<Expense> findByAdmin(Admin admin);

    List<Expense> findByAdmin_AdminId(Integer adminId);

    List<Expense> findByCampaign_CampaignIdAndCategory(Integer campaignId, ExpenseCategory category);
}
