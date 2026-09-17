package com.lealtixservice.repository;

import com.lealtixservice.entity.BebidaReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BebidaRecetaRepository extends JpaRepository<BebidaReceta, Long> {

    List<BebidaReceta> findByBebidaIdOrderByIdAsc(Long bebidaId);
}