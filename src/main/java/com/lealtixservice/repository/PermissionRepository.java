package com.lealtixservice.repository;

import com.lealtixservice.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    Optional<Permission> findByCode(String code);
    
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource")
    List<Permission> findByResource(@Param("resource") String resource);
    
    @Query("SELECT p FROM Permission p WHERE p.action = :action")
    List<Permission> findByAction(@Param("action") String action);
    
    @Query("SELECT p FROM Permission p WHERE p.category = :category")
    List<Permission> findByCategory(@Param("category") String category);
    
    @Query("SELECT p FROM Permission p WHERE p.code IN :codes")
    List<Permission> findByCodes(@Param("codes") List<String> codes);
}
