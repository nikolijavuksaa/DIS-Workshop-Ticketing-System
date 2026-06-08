package com.dis.workshopticketing.workshopservice.repository;

import com.dis.workshopticketing.workshopservice.model.WorkshopSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkshopSessionRepository extends JpaRepository<WorkshopSession, Long> {

    List<WorkshopSession> findAllByWorkshopId(Long workshopId);

    List<WorkshopSession> findAllByWorkshopIdAndActiveTrue(Long workshopId);

    Optional<WorkshopSession> findByIdAndActiveTrueAndWorkshopActiveTrue(Long id);

    boolean existsByIdAndActiveTrueAndWorkshopActiveTrue(Long id);
}
