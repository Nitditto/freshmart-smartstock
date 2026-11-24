package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.StockIssueDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockIssueDetailRepository extends JpaRepository<StockIssueDetail, Long> {

    @Query("""
            select d from StockIssueDetail d join fetch d.issue i
            where d.product.id = :productId and i.status = com.freshmart.smartstock.domain.enums.DocumentStatus.CONFIRMED
            order by i.confirmedAt desc, d.id desc
            """)
    List<StockIssueDetail> findConfirmedByProductId(@Param("productId") Long productId);
}
