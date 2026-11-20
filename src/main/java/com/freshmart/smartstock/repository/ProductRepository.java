package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    /**
     * Keyword matches name or SKU (case-insensitive). categoryId is optional.
     * onlyActive=true hides soft-deleted products.
     */
    @Query("""
            select p from Product p
            where (:onlyActive = false or p.active = true)
              and (:categoryId is null or p.category.id = :categoryId)
              and (:keyword is null
                   or lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(p.sku) like lower(concat('%', :keyword, '%')))
            """)
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId,
                         @Param("onlyActive") boolean onlyActive,
                         Pageable pageable);

    /** Low-stock rule: QuantityInStock <= MinStock (active products only). */
    @Query("""
            select p from Product p
            where p.active = true and p.quantityInStock <= p.minStock
            order by p.quantityInStock asc, p.id asc
            """)
    List<Product> findLowStock();

    /**
     * Row-level lock (SELECT ... FOR UPDATE) used while confirming a document, so two concurrent
     * confirmations cannot both read the same stock and oversell. Always ordered by id to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
