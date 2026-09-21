package com.lealtixservice.repository;

import com.lealtixservice.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {


    Optional<Tenant> findByAppUserId(Long userId);

    Optional<Tenant> getBySlug(String slug);

    @Query("select t.id from Tenant t")
    List<Long> findAllTenantIds();
}
