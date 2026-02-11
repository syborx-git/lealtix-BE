package com.lealtixservice.service;

import com.lealtixservice.dto.BulkCustomerUploadResponse;
import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.TenantCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TenantCustomerService {
    TenantCustomer save(TenantCustomer customer);
    Optional<TenantCustomer> findById(Long id);
    List<TenantCustomer> findAll();
    void deleteById(Long id);
    List<TenantCustomer> findByTenantId(Long tenantId);
    
    // Nuevos métodos
    Page<TenantCustomer> findByTenantIdPaginated(Long tenantId, Pageable pageable);
    Page<TenantCustomer> findByTenantIdAndEmailPaginated(Long tenantId, String email, Pageable pageable);
    void softDeleteById(Long id);
    BulkCustomerUploadResponse bulkUpload(Long tenantId, List<TenantCustomerDTO> customers);
}

