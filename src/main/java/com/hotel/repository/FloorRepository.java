package com.hotel.repository;

import com.hotel.domain.Floor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FloorRepository {
    Floor save(Floor floor);
    Optional<Floor> findById(UUID id);
    List<Floor> findAll();
    void delete(UUID id);
}
