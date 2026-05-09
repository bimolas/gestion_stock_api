package com.example.demo.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.models.Alert;
import com.example.demo.models.AlertStatus;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<Alert> findByFingerprintAndStatusIn(String fingerprint, Collection<AlertStatus> statuses);

    boolean existsByFingerprintAndStatusIn(String fingerprint, Collection<AlertStatus> statuses);
}
