package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Garantie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarantieRepository extends JpaRepository<Garantie, Long> {
}
