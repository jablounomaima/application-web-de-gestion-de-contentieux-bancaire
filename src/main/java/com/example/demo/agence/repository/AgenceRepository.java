package com.example.demo.agence.repository;

import com.example.demo.agence.entity.Agence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AgenceRepository extends JpaRepository<Agence, Long> {
    Optional<Agence> findByCode(String code);
}
