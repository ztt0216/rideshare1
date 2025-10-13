package com.unimelb.rideshare.service.impl;

import com.unimelb.rideshare.common.Result;
import com.unimelb.rideshare.domain.model.Rider;
import com.unimelb.rideshare.mapper.RiderDataMapper;
import com.unimelb.rideshare.service.RiderService;
import com.unimelb.rideshare.unitofwork.UnitOfWork;
import com.unimelb.rideshare.unitofwork.UnitOfWorkFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Default rider service implementation.
 */
public final class RiderServiceImpl implements RiderService {
    private final RiderDataMapper riderMapper;
    private final UnitOfWorkFactory unitOfWorkFactory;

    public RiderServiceImpl(RiderDataMapper riderMapper, UnitOfWorkFactory unitOfWorkFactory) {
        this.riderMapper = riderMapper;
        this.unitOfWorkFactory = unitOfWorkFactory;
    }

    @Override
    public Result<Rider> registerRider(String name, String email) {
        try {
            Rider rider = new Rider(UUID.randomUUID(), name, email);
            UnitOfWork unitOfWork = unitOfWorkFactory.create();
            unitOfWork.registerNew(rider, Rider.class);
            unitOfWork.commit();
            return Result.ok(rider);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    @Override
    public Optional<Rider> findById(UUID riderId) {
        return riderMapper.findById(riderId);
    }
}
