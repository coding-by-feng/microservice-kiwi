package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.tools.model.Project;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExportService {

    public byte[] toExcel(List<Project> projects) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("施工安排表");
            String[] headers = new String[]{"项目编号", "项目名称", "客户", "地址", "销售", "安装", "开始日期", "结束日期", "状态"};
            Row header = sheet.createRow(0);
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int r = 1;
            for (Project p : projects) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nvl(p.getProjectCode()));
                row.createCell(1).setCellValue(nvl(p.getName()));
                row.createCell(2).setCellValue(nvl(p.getClientName()));
                row.createCell(3).setCellValue(nvl(p.getAddress()));
                row.createCell(4).setCellValue(nvl(p.getSalesPerson()));
                row.createCell(5).setCellValue(nvl(p.getInstaller()));
                row.createCell(6).setCellValue(nvl(p.getStartDate()));
                row.createCell(7).setCellValue(nvl(p.getEndDate()));
                row.createCell(8).setCellValue(nvl(p.getStatus()));
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] toPdf(List<Project> projects) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.setFont(PDType1Font.HELVETICA, 10);
            float y = page.getMediaBox().getHeight() - 40;
            cs.beginText();
            cs.newLineAtOffset(40, y);
            cs.showText("施工安排表");
            cs.endText();
            y -= 20;
            String[] headers = new String[]{"项目编号", "项目名称", "客户", "地址", "销售", "安装", "开始日期", "结束日期", "状态"};
            float x = 40;
            y -= 10;
            cs.moveTo(x, y);
            cs.setLineWidth(0.5f);
            cs.stroke();
            y -= 12;
            cs.beginText();
            cs.newLineAtOffset(x, y);
            for (int i = 0; i < headers.length; i++) {
                cs.showText(headers[i] + " ");
            }
            cs.endText();
            y -= 16;
            for (Project p : projects) {
                if (y < 60) {
                    cs.close();
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    y = page.getMediaBox().getHeight() - 40;
                }
                cs.beginText();
                cs.newLineAtOffset(x, y);
                String line = String.join(" | ",
                        nvl(p.getProjectCode()), nvl(p.getName()), nvl(p.getClientName()), nvl(p.getAddress()),
                        nvl(p.getSalesPerson()), nvl(p.getInstaller()), nvl(p.getStartDate()), nvl(p.getEndDate()), nvl(p.getStatus()));
                cs.showText(line);
                cs.endText();
                y -= 14;
            }
            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
