package ru.mirea.project.util;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class DataExporter {
    private DataExporter() { }
    private static Path output(String name) throws IOException {
        Path path = Path.of(name).toAbsolutePath();
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        return path;
    }
    public static void exportToCSV(String name,List<String[]> data,String[] headers) throws IOException {
        Path path = output(name);
        try (BufferedWriter writer = Files.newBufferedWriter(path,StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // Excel определяет UTF-8 по BOM.
            writeRow(writer,headers);
            for (String[] row : data) writeRow(writer,row);
        }
        System.out.println("Данные экспортированы: " + path);
    }
    private static void writeRow(Writer writer,String[] row) throws IOException {
        for (int i = 0; i < row.length; i++) {
            if (i > 0) writer.write(';');
            String text = Objects.toString(row[i],"");
            // CSV открывается в Excel: текст не должен интерпретироваться как формула.
            if (text.matches("(?s)^[\\s]*[=+@-].*")) text = "'" + text;
            writer.write("\"" + text.replace("\"","\"\"") + "\"");
        }
        writer.write("\r\n");
    }
    public static void exportToExcel(String name,List<String[]> data,String[] headers) throws IOException {
        Path path = output(name);
        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("Данные");
            CellStyle style = book.createCellStyle();
            Font font = book.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(style);
            }
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i+1);
                for (int j = 0; j < data.get(i).length; j++)
                    row.createCell(j).setCellValue(Objects.toString(data.get(i)[j],""));
            }
            sheet.createFreezePane(0,1);
            if (headers.length > 0) sheet.setAutoFilter(new CellRangeAddress(0,data.size(),0,headers.length-1));
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i,Math.min(16000,sheet.getColumnWidth(i)+600));
            }
            try (OutputStream stream = Files.newOutputStream(path)) { book.write(stream); }
        }
        System.out.println("Данные экспортированы в Excel: " + path);
    }
}
