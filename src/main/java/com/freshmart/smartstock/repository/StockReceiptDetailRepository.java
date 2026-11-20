package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.StockReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO[Member B / E]: add the read-only queries for product history (/api/products/{id}/history)
 * and the stock-summary report. Both must only count lines of CONFIRMED documents.
 */
public interface StockReceiptDetailRepository extends JpaRepository<StockReceiptDetail, Long> {
}
