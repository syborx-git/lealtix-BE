package com.lealtixservice.repository;

import com.lealtixservice.dto.TenantMenuProductDTO;
import com.lealtixservice.entity.TenantMenuProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantMenuProductRepository extends JpaRepository<TenantMenuProduct, Long> {
    List<TenantMenuProduct> findByCategoryId(Long id);

    @Query("select new com.lealtixservice.dto.TenantMenuProductDTO(p.id, c.id, c.nombre, c.descripcion, c.isActive, c.displayOrder, t.id, p.nombre, p.descripcion, p.isActive, p.precio, p.imgUrl) " +
           "from TenantMenuProduct p join p.category c join c.tenant t " +
           "where t.id = :tenantId "+
           "order by c.displayOrder asc, c.nombre asc")
    List<TenantMenuProductDTO> findByCategoryTenantId(@Param("tenantId") Long tenantId);

    @Query("select p from TenantMenuProduct p join p.category c join c.tenant t where t.id = :tenantId order by p.nombre asc")
    List<TenantMenuProduct> findAllByTenantId(@Param("tenantId") Long tenantId);
}
