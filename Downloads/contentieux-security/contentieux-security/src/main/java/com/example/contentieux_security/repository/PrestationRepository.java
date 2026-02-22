package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Prestation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrestationRepository extends JpaRepository<Prestation, Long> {
}
