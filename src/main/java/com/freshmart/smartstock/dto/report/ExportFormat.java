package com.freshmart.smartstock.dto.report;

import com.freshmart.smartstock.common.exception.ApiException;
import java.util.Locale;

/** File formats of the stock-summary export; Excel opens CSV differently per regional settings, so XLSX exists too. */
public enum ExportFormat {
    CSV("csv", "text/csv;charset=UTF-8"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String contentType;

    ExportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /** Case-insensitive; anything other than csv or xlsx => 400. */
    public static ExportFormat parse(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ExportFormat format : values()) {
            if (format.extension.equals(text)) {
                return format;
            }
        }
        throw ApiException.badRequest("Định dạng xuất không hợp lệ (chỉ hỗ trợ csv hoặc xlsx)");
    }
}
