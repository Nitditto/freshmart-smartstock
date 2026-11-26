package com.freshmart.smartstock.dto.report;

/** A generated file ready to download: the controller only turns it into an HTTP attachment. */
public record ReportFile(String fileName, String contentType, byte[] content) {
}
