package com.docsshare_web_backend.commons.services;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

@Service
public class ExcelExportService<T> {

    public void export(HttpServletResponse response, String fileName, List<T> data) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                XSSFSheet sheet = workbook.createSheet("Sheet1");

                if (data != null && !data.isEmpty()) {
                    T first = data.getFirst();
                    PropertyDescriptor[] props = Introspector.getBeanInfo(first.getClass(), Object.class).getPropertyDescriptors();

                    // Header
                    XSSFRow headerRow = sheet.createRow(0);
                    for (int i = 0; i < props.length; i++) {
                        headerRow.createCell(i).setCellValue(props[i].getName());
                    }

                    // Rows
                    int rowIndex = 1;
                    for (T item : data) {
                        XSSFRow row = sheet.createRow(rowIndex++);
                        for (int i = 0; i < props.length; i++) {
                            Method getter = props[i].getReadMethod();
                            Object value = getter.invoke(item);
                            row.createCell(i).setCellValue(value != null ? value.toString() : "");
                        }
                    }
                }

                workbook.write(response.getOutputStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write Excel file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Excel export failed", e);
        }
    }
}
