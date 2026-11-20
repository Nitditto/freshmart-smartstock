package com.freshmart.smartstock.dto.issue;

import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import java.time.Instant;
import java.util.List;

public record IssueResponse(
        Long id,
        String code,
        IssueReason reason,
        DocumentStatus status,
        String note,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant confirmedAt,
        List<IssueLineResponse> lines) {

    /** Requires details/product/createdBy to be loaded (repository.findById uses an EntityGraph). */
    public static IssueResponse from(StockIssue i) {
        return new IssueResponse(
                i.getId(), i.getCode(), i.getReason(), i.getStatus(), i.getNote(),
                i.getCreatedBy().getId(), i.getCreatedBy().getFullName(),
                i.getCreatedAt(), i.getConfirmedAt(),
                i.getDetails().stream().map(IssueLineResponse::from).toList());
    }
}
