package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.Product;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only aggregates for the stock-summary report (owner: Member E). One GROUP BY query per document type
 * returns, for every product that moved, the quantity confirmed before the period and inside it, so the report
 * never loads document entities. Only CONFIRMED documents count, dated by confirmedAt (api-contract section 5.8).
 */
public interface ReportRepository extends Repository<Product, Long> {

    /** Quantities of one product: {@code beforeQty} in [-inf, from), {@code withinQty} in [from, to). */
    interface ProductMovement {
        Long getProductId();

        Long getBeforeQty();

        Long getWithinQty();
    }

    @Query("""
            select d.product.id as productId,
                   sum(case when r.confirmedAt < :from then d.quantity else 0 end) as beforeQty,
                   sum(case when r.confirmedAt >= :from then d.quantity else 0 end) as withinQty
            from StockReceiptDetail d join d.receipt r
            where r.status = com.freshmart.smartstock.domain.enums.DocumentStatus.CONFIRMED
              and r.confirmedAt < :to
            group by d.product.id
            """)
    List<ProductMovement> sumConfirmedReceipts(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select d.product.id as productId,
                   sum(case when i.confirmedAt < :from then d.quantity else 0 end) as beforeQty,
                   sum(case when i.confirmedAt >= :from then d.quantity else 0 end) as withinQty
            from StockIssueDetail d join d.issue i
            where i.status = com.freshmart.smartstock.domain.enums.DocumentStatus.CONFIRMED
              and i.confirmedAt < :to
            group by d.product.id
            """)
    List<ProductMovement> sumConfirmedIssues(@Param("from") Instant from, @Param("to") Instant to);

    /** Every product, hidden ones included: a hidden product may still have moved during the period. */
    @Query("select p from Product p order by p.id")
    List<Product> findAllProductsOrderById();
}
