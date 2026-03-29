package com.ben.tradeanalyzer.ingest;

import com.ben.tradeanalyzer.model.Trade;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Reads trade records from an .xlsx file using Apache POI
 */
public class ExcelTradeReader {

    private static final String COL_ENTRY_DATE = "Entry Date";
    private static final String COL_PNL = "Trade P&L ($)";
    private static final String COL_RETURN = "Trade Return (%)";
    private static final String COL_R_MULTIPLE = "R-Multiple";

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            COL_ENTRY_DATE, COL_PNL, COL_RETURN, COL_R_MULTIPLE
    );

    public List<Trade> read(Path file) throws IOException {
        List<Trade> trades = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columnIndex = readHeader(sheet.getRow(0));

            validateRequiredColumns(columnIndex);

            int totalRows = 0;
            int skipped = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRows++;
                Trade trade = parseRow(row, columnIndex, i + 1);
                if (trade != null) {
                    trades.add(trade);
                } else {
                    skipped++;
                }
            }

            if (skipped > 0) {
                System.err.printf("[warn] Skipped %d of %d rows due to missing or malformed data%n",
                        skipped, totalRows);
            }
        }

        trades.sort(Comparator.comparing(Trade::entryDate));
        return trades;
    }

    private Map<String, Integer> readHeader(Row headerRow) {
        Map<String, Integer> index = new LinkedHashMap<>();
        if (headerRow == null) return index;

        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell != null && cell.getCellType() == CellType.STRING) {
                index.put(cell.getStringCellValue().trim(), c);
            }
        }
        return index;
    }

    private void validateRequiredColumns(Map<String, Integer> columnIndex) {
        List<String> missing = REQUIRED_COLUMNS.stream()
                .filter(col -> !columnIndex.containsKey(col))
                .sorted()
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required columns: " + String.join(", ", missing));
        }
    }

    /**
     * Parses a single row into a Trade. Returns null if the row is incomplete
     */
    private Trade parseRow(Row row, Map<String, Integer> columnIndex, int rowNum) {
        try {
            LocalDate entryDate = readDate(row, columnIndex.get(COL_ENTRY_DATE));
            if (entryDate == null) return null;

            Double pnl = readNumeric(row, columnIndex.get(COL_PNL));
            if (pnl == null) return null;

            Double returnPct = readNumeric(row, columnIndex.get(COL_RETURN));
            if (returnPct == null) return null;

            Double rMultiple = readRMultiple(row, columnIndex.get(COL_R_MULTIPLE));
            if (rMultiple == null) return null;

            return new Trade(entryDate, pnl, returnPct, rMultiple);
        } catch (Exception e) {
            System.err.printf("[warn] Row %d: %s%n", rowNum, e.getMessage());
            return null;
        }
    }

    private LocalDate readDate(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;

        try {
            Date date = switch (cell.getCellType()) {
                case NUMERIC -> cell.getDateCellValue();
                case FORMULA -> cell.getDateCellValue();
                default -> null;
            };
            if (date == null) return null;
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    private Double readNumeric(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;

        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case FORMULA -> cell.getNumericCellValue();
                case STRING -> {
                    String text = cell.getStringCellValue().trim();
                    yield text.isEmpty() ? null : Double.parseDouble(text);
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Double readRMultiple(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;

        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> parseRMultipleString(cell.getStringCellValue());
                case FORMULA -> {
                    // Cached result could be string ("1.52 R") or numeric
                    if (cell.getCachedFormulaResultType() == CellType.STRING) {
                        yield parseRMultipleString(cell.getStringCellValue());
                    } else {
                        yield cell.getNumericCellValue();
                    }
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseRMultipleString(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim().replaceAll("\\s*R$", "");
        return cleaned.isEmpty() ? null : Double.parseDouble(cleaned);
    }
}
