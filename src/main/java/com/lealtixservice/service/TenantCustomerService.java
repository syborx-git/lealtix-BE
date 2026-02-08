package com.lealtixservice.service;

import com.lealtixservice.dto.BulkCustomerUploadResponse;
import com.lealtixservice.entity.TenantCustomer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantCustomerService {
    TenantCustomer save(TenantCustomer customer);
    TenantCustomer update(TenantCustomer customer);
    Optional<TenantCustomer> findById(Long id);
    List<TenantCustomer> findAll();
    void deleteById(Long id);
    List<TenantCustomer> findByTenantId(Long tenantId);

    Page<TenantCustomer> findByTenantIdPaginated(Long tenantId, Pageable pageable);
    Page<TenantCustomer> findByTenantIdAndEmailPaginated(Long tenantId, String email, Pageable pageable);
    void softDeleteById(Long id);
    BulkCustomerUploadResponse bulkUpload(Long tenantId, List<com.lealtixservice.dto.TenantCustomerDTO> customers);
}