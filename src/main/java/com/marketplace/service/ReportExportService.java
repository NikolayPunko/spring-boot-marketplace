package com.marketplace.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.marketplace.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final ReportRepository repo;

    // ======== PUBLIC API ========

    public byte[] exportCsv(String reportKey, Map<String, String> params) {
        List<Map<String, Object>> data = loadReport(reportKey, params);
        String csv = toCsv(data);
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportXlsx(String reportKey, Map<String, String> params) {
        List<Map<String, Object>> data = loadReport(reportKey, params);
        return toXlsx(reportKey, data);
    }

    public byte[] exportPdf(String reportKey, Map<String, String> params) {
        List<Map<String, Object>> data = loadReport(reportKey, params);
        return toPdf(reportKey, data);
    }

    // ======== REPORT DISPATCHER ========

    private List<Map<String, Object>> loadReport(String reportKey, Map<String, String> params) {

        return switch (reportKey) {

            case "top-products" -> {
                int limit = intParam(params, "limit", 5);
                yield repo.topProducts(limit);
            }

            case "top-sellers" -> {
                LocalDate start = dateParam(params, "start");
                LocalDate end = dateParam(params, "end");
                yield repo.topSellers(start, end);
            }

            case "avg-check-by-month" -> repo.avgCheckByMonth();

            case "category-revenue-share" -> repo.categoryRevenueShare();

            case "users-without-orders" -> repo.usersWithoutOrders();

            case "order-details" -> {
                long orderId = longParam(params, "orderId");
                yield repo.orderDetails(orderId);
            }

            case "payments-by-status" -> repo.paymentsByStatus();

            case "low-stock" -> {
                int threshold = intParam(params, "threshold", 5);
                yield repo.lowStock(threshold);
            }

            case "logins-by-day" -> {
                LocalDate start = dateParam(params, "start");
                LocalDate end = dateParam(params, "end");
                yield repo.loginsByDay(start, end);
            }

            case "audit-summary" -> {
                LocalDate start = dateParam(params, "start");
                LocalDate end = dateParam(params, "end");
                yield repo.auditSummary(start, end);
            }

            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown reportKey: " + reportKey);
        };
    }

    // ======== CSV ========

    private String toCsv(List<Map<String, Object>> data) {

        if (data == null || data.isEmpty()) {
            return "";
        }

        List<String> headers = new ArrayList<>(data.get(0).keySet());

        StringBuilder sb = new StringBuilder();

        // headers
        sb.append(String.join(",", headers)).append("\n");

        // rows
        for (Map<String, Object> row : data) {
            List<String> values = new ArrayList<>();
            for (String h : headers) {
                Object v = row.get(h);
                values.add(escapeCsv(v));
            }
            sb.append(String.join(",", values)).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);

        boolean needQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        s = s.replace("\"", "\"\"");

        return needQuotes ? "\"" + s + "\"" : s;
    }

    // ======== XLSX ========

    private byte[] toXlsx(String title, List<Map<String, Object>> data) {

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("report");

            if (data == null || data.isEmpty()) {
                Row r = sheet.createRow(0);
                r.createCell(0).setCellValue("No data");
                return workbookToBytes(wb);
            }

            List<String> headers = new ArrayList<>(data.get(0).keySet());

            // header style
            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(headerStyle);
            }

            // data rows
            for (int r = 0; r < data.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Map<String, Object> map = data.get(r);

                for (int c = 0; c < headers.size(); c++) {
                    Object v = map.get(headers.get(c));
                    Cell cell = row.createCell(c);
                    cell.setCellValue(v == null ? "" : String.valueOf(v));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            return workbookToBytes(wb);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "XLSX export failed: " + e.getMessage());
        }
    }

    private byte[] workbookToBytes(Workbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    // ======== PDF ========

    private byte[] toPdf(String title, List<Map<String, Object>> data) {

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(doc, out);

            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            doc.add(new Paragraph("Report: " + title, titleFont));
            doc.add(new Paragraph("Generated: " + new Date()));
            doc.add(new Paragraph(" "));

            if (data == null || data.isEmpty()) {
                doc.add(new Paragraph("No data"));
                doc.close();
                return out.toByteArray();
            }

            List<String> headers = new ArrayList<>(data.get(0).keySet());
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            for (String h : headers) {
                table.addCell(new Phrase(h, headerFont));
            }

            for (Map<String, Object> row : data) {
                for (String h : headers) {
                    Object v = row.get(h);
                    table.addCell(new Phrase(v == null ? "" : String.valueOf(v), cellFont));
                }
            }

            doc.add(table);
            doc.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF export failed: " + e.getMessage());
        }
    }

    // ======== PARAMS HELPERS ========

    private int intParam(Map<String, String> params, String key, int def) {
        try {
            String v = params.get(key);
            if (v == null || v.isBlank()) return def;
            return Integer.parseInt(v);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid int param: " + key);
        }
    }

    private long longParam(Map<String, String> params, String key) {
        try {
            String v = params.get(key);
            if (v == null || v.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing param: " + key);
            }
            return Long.parseLong(v);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid long param: " + key);
        }
    }

    private LocalDate dateParam(Map<String, String> params, String key) {
        try {
            String v = params.get(key);
            if (v == null || v.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing param: " + key);
            }
            return LocalDate.parse(v);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date param: " + key);
        }
    }
}