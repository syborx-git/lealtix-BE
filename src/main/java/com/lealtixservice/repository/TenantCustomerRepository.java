package com.lealtixservice.repository;

import com.lealtixservice.entity.TenantCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantCustomerRepository extends JpaRepository<TenantCustomer, Long>, JpaSpecificationExecutor<TenantCustomer> {
    List<TenantCustomer> findByTenantId(Long tenantId);
    Optional<TenantCustomer> findByEmailAndTenantId(String email, Long tenantId);
    Optional<TenantCustomer> findByPhoneAndTenantId(String phone, Long tenantId);
    boolean existsByEmailAndTenantId(String email, Long tenantId);

    Page<TenantCustomer> findByTenantIdAndActiveTrue(Long tenantId, Pageable pageable);

    Page<TenantCustomer> findByTenantIdAndEmailContainingIgnoreCaseAndActiveTrue(Long tenantId, String email, Pageable pageable);
}

