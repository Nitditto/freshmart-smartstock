package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.dto.issue.IssueConfirmResponse;
import com.freshmart.smartstock.dto.issue.IssueRequest;
import com.freshmart.smartstock.dto.issue.IssueResponse;
import com.freshmart.smartstock.service.IssueService;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * TODO[Member C]: implement. On insufficient stock throw
 * ApiException.conflict("Không đủ tồn kho", List<ShortageItem>) - the handler puts the list into `data`.
 */
@Service
public class IssueServiceImpl implements IssueService {

    @Override
    public IssueResponse create(IssueRequest request) {
        throw ApiException.notImplemented("Member C");
    }

    @Override
    public PageResponse<IssueResponse> search(DocumentStatus status, LocalDate from, LocalDate to,
                                              Pageable pageable) {
        throw ApiException.notImplemented("Member C");
    }

    @Override
    public IssueResponse get(Long id) {
        throw ApiException.notImplemented("Member C");
    }

    @Override
    public IssueResponse update(Long id, IssueRequest request) {
        throw ApiException.notImplemented("Member C");
    }

    @Override
    public IssueConfirmResponse confirm(Long id) {
        throw ApiException.notImplemented("Member C");
    }

    @Override
    public IssueResponse cancel(Long id) {
        throw ApiException.notImplemented("Member C");
    }
}
