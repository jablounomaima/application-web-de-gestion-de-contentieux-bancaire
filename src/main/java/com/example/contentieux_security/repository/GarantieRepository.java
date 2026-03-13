package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Garantie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GarantieRepository extends JpaRepository<Garantie, Long> {
    List<Garantie> findByRisque_Id(Long risqueId);
}