package com.ngo.donation.service.impl;

import com.ngo.donation.dto.request.DonorProfileUpdateRequest;
import com.ngo.donation.dto.response.DonorProfileResponse;
import com.ngo.donation.entity.Donor;
import com.ngo.donation.exception.ResourceNotFoundException;
import com.ngo.donation.repository.DonorRepository;
import com.ngo.donation.service.DonorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonorServiceImpl implements DonorService {

    private final DonorRepository donorRepository;

    @Autowired
    public DonorServiceImpl(DonorRepository donorRepository) {
        this.donorRepository = donorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DonorProfileResponse getDonorProfile(Integer donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with ID: " + donorId));

        return new DonorProfileResponse(
                donor.getDonorId(),
                donor.getName(),
                donor.getEmail(),
                donor.getPhone(),
                donor.getAddress(),
                donor.getCreatedAt(),
                donor.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public DonorProfileResponse updateDonorProfile(Integer donorId, DonorProfileUpdateRequest request) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with ID: " + donorId));

        donor.setName(request.getName());
        donor.setPhone(request.getPhone());
        donor.setAddress(request.getAddress());

        Donor updated = donorRepository.save(donor);

        return new DonorProfileResponse(
                updated.getDonorId(),
                updated.getName(),
                updated.getEmail(),
                updated.getPhone(),
                updated.getAddress(),
                updated.getCreatedAt(),
                updated.getUpdatedAt()
        );
    }
}
