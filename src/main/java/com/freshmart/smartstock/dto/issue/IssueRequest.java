package com.freshmart.smartstock.dto.issue;

import com.freshmart.smartstock.domain.enums.IssueReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Used by POST /api/issues and PUT /api/issues/{id}. Creator is taken from the JWT, never from here. */
public record IssueRequest(
        @NotNull IssueReason reason,
        @Size(max = 500) String note,
        @NotEmpty(message = "Phiếu phải có ít nhất 1 dòng") List<@Valid IssueLineRequest> lines) {
}
