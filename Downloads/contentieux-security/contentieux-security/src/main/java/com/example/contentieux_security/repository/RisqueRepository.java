package com.example.contentieux_security.repository;

import com.example.contentieux_security.entity.Risque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RisqueRepository extends JpaRepository<Risque, Long> {
}
