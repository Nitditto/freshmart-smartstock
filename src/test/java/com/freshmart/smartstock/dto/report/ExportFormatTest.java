package com.freshmart.smartstock.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.freshmart.smartstock.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ExportFormatTest {

    @Test
    void parsesCsvAndXlsxIgnoringCaseAndSpaces() {
        assertEquals(ExportFormat.CSV, ExportFormat.parse("csv"));
        assertEquals(ExportFormat.XLSX, ExportFormat.parse(" XLSX "));
    }

    @Test
    void rejectsAnythingElseWith400() {
        for (String bad : new String[] {"pdf", "", null, "xls"}) {
            ApiException ex = assertThrows(ApiException.class, () -> ExportFormat.parse(bad));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }
    }
}
