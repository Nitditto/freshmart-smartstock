package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.StockReceiptDetail;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReceiptDetailRepository extends JpaRepository<StockReceiptDetail, Long> {

    /** Newest first; the caller passes a Pageable to cap how many rows are read. */
    @Query("""
            select d from StockReceiptDetail d join fetch d.receipt r
            where d.product.id = :productId
              and r.status = com.freshmart.smartstock.domain.enums.DocumentStatus.CONFIRMED
            order by r.confirmedAt desc, d.id desc
            """)
    List<StockReceiptDetail> findConfirmedByProductId(@Param("productId") Long productId, Pageable pageable);
}
