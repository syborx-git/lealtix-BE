package com.lealtixservice.repository;

import com.lealtixservice.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role")
    List<RolePermission> findByRole(@Param("role") String role);
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role AND rp.permission.id = :permissionId")
    Optional<RolePermission> findByRoleAndPermissionId(@Param("role") String role, @Param("permissionId") Long permissionId);
    
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :role AND rp.permission.id = :permissionId")
    void deleteByRoleAndPermissionId(@Param("role") String role, @Param("permissionId") Long permissionId);
    
    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp WHERE rp.role = :role AND rp.permission.id = :permissionId")
    boolean existsByRoleAndPermissionId(@Param("role") String role, @Param("permissionId") Long permissionId);
}
