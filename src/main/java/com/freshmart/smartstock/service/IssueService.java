package com.freshmart.smartstock.service;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.dto.issue.IssueConfirmResponse;
import com.freshmart.smartstock.dto.issue.IssueRequest;
import com.freshmart.smartstock.dto.issue.IssueResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

/**
 * Goods issues. Same document rules as receipts, plus:
 * - confirm checks ALL lines first; if any line exceeds stock => 409 with the shortage list and NOTHING is deducted
 * - reason DAMAGED can only be confirmed by ADMIN
 * - confirm result lists the products that just dropped to/below MinStock
 */
public interface IssueService {

    IssueResponse create(IssueRequest request);

    PageResponse<IssueResponse> search(DocumentStatus status, LocalDate from, LocalDate to, Pageable pageable);

    IssueResponse get(Long id);

    IssueResponse update(Long id, IssueRequest request);

    IssueConfirmResponse confirm(Long id);

    IssueResponse cancel(Long id);
}
