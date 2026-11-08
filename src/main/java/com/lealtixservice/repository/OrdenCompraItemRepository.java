package com.lealtixservice.repository;

import com.lealtixservice.entity.OrdenCompraItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenCompraItemRepository extends JpaRepository<OrdenCompraItem, Long> {

    List<OrdenCompraItem> findByOrdenCompraIdOrderByIdAsc(Long ordenCompraId);
}

