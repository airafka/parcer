package local.parser.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelReader {
    public List<Map<String, Object>> read(Path inputFile, String sheetName) throws IOException {
        if (!Files.exists(inputFile)) {
            throw new IOException("Excel file does not exist: " + inputFile);
        }

        try (InputStream inputStream = Files.newInputStream(inputFile);
            Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = resolveSheet(workbook, sheetName);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();

            Row headerRow = findFirstNotEmptyRow(sheet, formatter, evaluator);
            if (headerRow == null) {
                return List.of();
            }

            return readRows(sheet, headerRow.getRowNum(), headerRow.getRowNum() + 1, formatter, evaluator);
        }
    }

    public boolean isEtranTemplate(Path inputFile) throws IOException {
        if (!Files.exists(inputFile)) {
            throw new IOException("Excel file does not exist: " + inputFile);
        }

        try (InputStream inputStream = Files.newInputStream(inputFile);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            return workbook.getSheet("Накладная") != null && workbook.getSheet("Свод данных") != null;
        }
    }

    public List<Map<String, Object>> readEtranTemplate(Path inputFile) throws IOException {
        if (!Files.exists(inputFile)) {
            throw new IOException("Excel file does not exist: " + inputFile);
        }

        try (InputStream inputStream = Files.newInputStream(inputFile);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet invoiceSheet = workbook.getSheet("Накладная");
            Sheet summarySheet = workbook.getSheet("Свод данных");
            if (invoiceSheet == null || summarySheet == null) {
                throw new IllegalArgumentException("ETRAN template must contain sheets: Накладная, Свод данных.");
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();

            List<Map<String, Object>> invoices = readRows(invoiceSheet, 0, 2, formatter, evaluator);
            List<Map<String, Object>> summaryRows = readRows(summarySheet, 0, 2, formatter, evaluator);
            Map<String, Map<String, Object>> invoicesByNumber = indexInvoicesByNumber(invoices);
            Map<String, Object> singleInvoice = invoices.size() == 1 ? invoices.get(0) : null;
            List<Map<String, Object>> result = new ArrayList<>();

            for (Map<String, Object> summaryRow : summaryRows) {
                String invoiceNumber = stringValue(summaryRow.get("Номер накладной ЭТРАН"));
                Map<String, Object> invoice = invoicesByNumber.get(invoiceNumber);
                if (invoice == null && singleInvoice != null) {
                    invoice = singleInvoice;
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("Накладная", invoice);
                item.put("Свод данных", summaryRow);
                result.add(item);
            }
            return result;
        }
    }

    private Sheet resolveSheet(Workbook workbook, String sheetName) {
        if (sheetName == null) {
            return workbook.getSheetAt(0);
        }

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("Sheet not found: " + sheetName);
        }
        return sheet;
    }

    private Row findFirstNotEmptyRow(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (Row row : sheet) {
            if (!isRowEmpty(row, formatter, evaluator)) {
                return row;
            }
        }
        return null;
    }

    private List<String> readHeaders(Row headerRow, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<String> headers = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            String header = normalizeHeader(formatter.formatCellValue(headerRow.getCell(columnIndex), evaluator));
            headers.add(header);
        }
        return headers;
    }

    private List<Map<String, Object>> readRows(
            Sheet sheet,
            int headerRowIndex,
            int firstDataRowIndex,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            return List.of();
        }

        List<String> headers = readHeaders(headerRow, formatter, evaluator);
        List<Map<String, Object>> result = new ArrayList<>();

        for (int rowIndex = firstDataRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isRowEmpty(row, formatter, evaluator)) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                String header = headers.get(columnIndex);
                if (header == null || header.isBlank()) {
                    continue;
                }

                Object value = readCellValue(row.getCell(columnIndex), evaluator, formatter);
                if (value != null) {
                    item.put(header, value);
                }
            }

            if (!item.isEmpty()) {
                result.add(item);
            }
        }

        return result;
    }

    private Map<String, Map<String, Object>> indexInvoicesByNumber(List<Map<String, Object>> invoices) {
        Map<String, Map<String, Object>> invoicesByNumber = new LinkedHashMap<>();
        for (Map<String, Object> invoice : invoices) {
            String number = stringValue(invoice.get("Номер"));
            if (number != null) {
                invoicesByNumber.put(number, invoice);
            }
        }
        return invoicesByNumber;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return null;
        }
        return header.trim().replaceAll("\\*+$", "").trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
            String value = formatter.formatCellValue(row.getCell(columnIndex), evaluator);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Object readCellValue(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }

        CellType cellType = cell.getCellType() == CellType.FORMULA
                ? evaluator.evaluateFormulaCell(cell)
                : cell.getCellType();

        return switch (cellType) {
            case STRING -> blankToNull(cell.getStringCellValue());
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> readNumericCell(cell);
            case BLANK, ERROR, _NONE -> null;
            case FORMULA -> blankToNull(formatter.formatCellValue(cell, evaluator));
        };
    }

    private Object readNumericCell(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString();
        }

        double numericValue = cell.getNumericCellValue();
        BigDecimal decimal = BigDecimal.valueOf(numericValue).stripTrailingZeros();
        if (decimal.scale() <= 0) {
            try {
                return decimal.longValueExact();
            } catch (ArithmeticException ignored) {
                return decimal;
            }
        }
        return decimal;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
