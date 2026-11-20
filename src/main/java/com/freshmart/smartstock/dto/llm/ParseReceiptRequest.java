package com.freshmart.smartstock.dto.llm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParseReceiptRequest(
        @NotBlank(message = "Nội dung hóa đơn không được để trống")
        @Size(max = 8000, message = "Hóa đơn quá dài (tối đa 8000 ký tự)") String text) {
}
