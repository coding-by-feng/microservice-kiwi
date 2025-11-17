package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.tools.model.project.Project;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService {

    public byte[] toExcel(List<Project> projects) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("施工安排表");
            String[] headers = new String[]{
                "项目ID", "项目编号", "项目名称", "客户", "地址", "销售", "安装", "团队成员",
                "开始日期", "结束日期", "玻璃", "玻璃备注", "框架", "框架备注", "采购", "采购备注",
                "运输", "运输备注", "安装阶段", "安装备注", "维修", "维修备注",
                "今日任务", "进度备注", "项目变更", "创建时间", "已归档"
            };
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
                row.createCell(0).setCellValue(nvl(p.getId()));
                row.createCell(1).setCellValue(nvl(p.getProjectCode()));
                row.createCell(2).setCellValue(nvl(p.getName()));
                row.createCell(3).setCellValue(nvl(p.getClientName()));
                row.createCell(4).setCellValue(nvl(p.getAddress()));
                row.createCell(5).setCellValue(nvl(p.getSalesPerson()));
                row.createCell(6).setCellValue(nvl(p.getInstaller()));
                row.createCell(7).setCellValue(nvl(p.getTeamMembers()));
                row.createCell(8).setCellValue(nvl(p.getStartDate()));
                row.createCell(9).setCellValue(nvl(p.getEndDate()));
                row.createCell(10).setCellValue(yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getGlass())));
                row.createCell(11).setCellValue(nvl(p.getStages() != null ? p.getStages().getGlassRemark() : null));
                row.createCell(12).setCellValue(yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getFrame())));
                row.createCell(13).setCellValue(nvl(p.getStages() != null ? p.getStages().getFrameRemark() : null));
                row.createCell(14).setCellValue(yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getPurchase())));
                row.createCell(15).setCellValue(nvl(p.getStages() != null ? p.getStages().getPurchaseRemark() : null));
                row.createCell(16).setCellValue(yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getTransport())));
                row.createCell(17).setCellValue(nvl(p.getStages() != null ? p.getStages().getTransportRemark() : null));
                row.createCell(18).setCellValue(yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getInstall())));
                row.createCell(19).setCellValue(nvl(p.getStages() != null ? p.getStages().getInstallRemark() : null));
                row.createCell(20).setCellValue(yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getRepair())));
                row.createCell(21).setCellValue(nvl(p.getStages() != null ? p.getStages().getRepairRemark() : null));
                row.createCell(22).setCellValue(nvl(p.getTodayTask()));
                row.createCell(23).setCellValue(nvl(p.getProgressNote()));
                row.createCell(24).setCellValue(nvl(p.getChangeNote()));
                row.createCell(25).setCellValue(formatDateTime(p));
                row.createCell(26).setCellValue(yn(p.getArchived()));
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] toPdf(List<Project> projects) throws IOException {
        final PDRectangle PAGE_SIZE = rotate(PDRectangle.A4);
        final float MARGIN = 36f;
        final float HEADER_HEIGHT = 24f;
        final float TABLE_HEADER_HEIGHT = 18f;
        final float ROW_LINE_HEIGHT = 12f;
        final float DETAIL_LINE_GAP = 2f;

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PAGE_SIZE);
            doc.addPage(page);

            PDFont titleFont = loadCjkFontOrDefault(doc, true);
            PDFont headerFont = titleFont;
            PDFont cellFont = loadCjkFontOrDefault(doc, false);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float pageWidth = PAGE_SIZE.getWidth();
            float pageHeight = PAGE_SIZE.getHeight();
            float tableLeft = MARGIN;
            float tableRight = pageWidth - MARGIN;
            float tableWidth = tableRight - tableLeft;
            float y = pageHeight - MARGIN;

            // Title
            cs.beginText();
            cs.setFont(titleFont, 14);
            cs.newLineAtOffset(tableLeft, y);
            cs.showText(sanitizeForFont(titleFont, "施工安排表"));
            cs.endText();
            y -= HEADER_HEIGHT;

            String[] headers = new String[]{
                "编号", "名称", "客户", "地址", "开始", "结束",
                "玻璃", "框架", "采购", "运输", "安装", "维修",
                "归档", "创建时间"
            };
            float[] colPercents = new float[]{12, 16, 12, 18, 7, 7, 5, 5, 5, 5, 5, 5, 5, 8};
            float[] colWidths = new float[colPercents.length];
            for (int i = 0; i < colPercents.length; i++) colWidths[i] = tableWidth * (colPercents[i] / 100f);

            cs.setNonStrokingColor(240);
            cs.addRect(tableLeft, y - TABLE_HEADER_HEIGHT, tableWidth, TABLE_HEADER_HEIGHT);
            cs.fill();
            cs.setNonStrokingColor(0);

            float x = tableLeft + 4f;
            float cursorY = y - 13f;
            for (int i = 0; i < headers.length; i++) {
                cs.beginText();
                cs.setFont(headerFont, 9);
                cs.newLineAtOffset(x, cursorY);
                cs.showText(sanitizeForFont(headerFont, headers[i]));
                cs.endText();
                x += colWidths[i];
            }
            y -= TABLE_HEADER_HEIGHT;

            for (Project p : projects) {
                String[] row = new String[]{
                    nvl(p.getProjectCode()), nvl(p.getName()), nvl(p.getClientName()), nvl(p.getAddress()),
                    nvl(p.getStartDate()), nvl(p.getEndDate()),
                    yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getGlass())),
                    yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getFrame())),
                    yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getPurchase())),
                    yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getTransport())),
                    yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getInstall())),
                    yn(p.getStages() != null && Boolean.TRUE.equals(p.getStages().getRepair())),
                    yn(p.getArchived()),
                    formatDateTime(p)
                };
                String detail = "任务: " + nvl(p.getTodayTask()) +
                                " | 备注: " + nvl(p.getProgressNote()) +
                                " | 变更: " + nvl(p.getChangeNote()) +
                                " | 阶段备注: " + joinRemarks(p);

                List<List<String>> wrappedCells = new ArrayList<>();
                int maxCellLines = 1;
                for (int i = 0; i < row.length; i++) {
                    List<String> lines = wrapText(cellFont, 8, row[i], colWidths[i] - 6f);
                    if (lines.size() > 2) lines = lines.subList(0, 2);
                    wrappedCells.add(lines);
                    maxCellLines = Math.max(maxCellLines, lines.size());
                }
                List<String> detailLines = wrapText(cellFont, 8, detail, tableWidth - 6f);

                float rowHeight = (maxCellLines * ROW_LINE_HEIGHT);
                float detailHeight = detailLines.isEmpty() ? 0f : (DETAIL_LINE_GAP + detailLines.size() * ROW_LINE_HEIGHT);
                float requiredHeight = rowHeight + detailHeight + 6f;

                if (y - requiredHeight < MARGIN) {
                    cs.close();
                    page = new PDPage(PAGE_SIZE);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageHeight - MARGIN;

                    cs.setNonStrokingColor(240);
                    cs.addRect(tableLeft, y - TABLE_HEADER_HEIGHT, tableWidth, TABLE_HEADER_HEIGHT);
                    cs.fill();
                    cs.setNonStrokingColor(0);
                    x = tableLeft + 4f;
                    cursorY = y - 13f;
                    for (int i = 0; i < headers.length; i++) {
                        cs.beginText();
                        cs.setFont(headerFont, 9);
                        cs.newLineAtOffset(x, cursorY);
                        cs.showText(sanitizeForFont(headerFont, headers[i]));
                        cs.endText();
                        x += colWidths[i];
                    }
                    y -= TABLE_HEADER_HEIGHT;
                }

                float textY = y - 4f;
                for (int line = 0; line < maxCellLines; line++) {
                    x = tableLeft + 3f;
                    for (int c = 0; c < wrappedCells.size(); c++) {
                        String part = line < wrappedCells.get(c).size() ? wrappedCells.get(c).get(line) : "";
                        cs.beginText();
                        cs.setFont(cellFont, 8);
                        cs.newLineAtOffset(x, textY - (line * ROW_LINE_HEIGHT));
                        cs.showText(sanitizeForFont(cellFont, part));
                        cs.endText();
                        x += colWidths[c];
                    }
                }

                float detailYStart = y - rowHeight - DETAIL_LINE_GAP;
                for (int i = 0; i < detailLines.size(); i++) {
                    cs.beginText();
                    cs.setFont(cellFont, 8);
                    cs.newLineAtOffset(tableLeft + 3f, detailYStart - (i * ROW_LINE_HEIGHT));
                    cs.showText(sanitizeForFont(cellFont, detailLines.get(i)));
                    cs.endText();
                }

                cs.moveTo(tableLeft, y);
                cs.lineTo(tableRight, y);
                cs.stroke();
                cs.moveTo(tableLeft, y - requiredHeight);
                cs.lineTo(tableRight, y - requiredHeight);
                cs.stroke();

                y -= requiredHeight;
            }

            cs.moveTo(tableLeft, y);
            cs.lineTo(tableRight, y);
            cs.stroke();

            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String formatDateTime(Project p) {
        if (p.getCreatedAt() == null) return "";
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return fmt.format(p.getCreatedAt());
        } catch (Exception e) {
            return p.getCreatedAt().toString();
        }
    }

    private String yn(Boolean b) { return Boolean.TRUE.equals(b) ? "是" : "否"; }

    private PDFont loadCjkFontOrDefault(PDDocument doc, boolean bold) {
        String[] candidates = bold
            ? new String[]{"/fonts/NotoSansSC-Bold.otf", "/fonts/NotoSansCJKsc-Bold.otf", "/fonts/SourceHanSansCN-Bold.otf", "/fonts/SimHei.ttf"}
            : new String[]{"/fonts/NotoSansSC-Regular.otf", "/fonts/NotoSansCJKsc-Regular.otf", "/fonts/SourceHanSansCN-Regular.otf", "/fonts/SimSun.ttf"};
        for (String path : candidates) {
            PDFont f = tryLoadFont(doc, path);
            if (f != null) return f;
        }
        return bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
    }

    private PDFont tryLoadFont(PDDocument doc, String classpath) {
        try (InputStream in = ExportService.class.getResourceAsStream(classpath)) {
            if (in == null) return null;
            return PDType0Font.load(doc, in, true);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private String sanitizeForFont(PDFont font, String text) {
        if (font instanceof PDType0Font) return text;
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            if (cp >= 32 && cp <= 126) sb.appendCodePoint(cp); else sb.append('?');
        });
        return sb.toString();
    }

    private List<String> wrapText(PDFont font, float fontSize, String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.replace("\r", "").split("\n");
        for (String paragraph : words) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                line.append(paragraph.charAt(i));
                float w = stringWidth(font, fontSize, line.toString());
                if (w > maxWidth) {
                    if (line.length() > 1) {
                        String l = line.substring(0, line.length() - 1);
                        if (!l.trim().isEmpty()) lines.add(l);
                        line.setLength(0);
                        line.append(paragraph.charAt(i));
                    } else {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                }
            }
            if (line.length() > 0) lines.add(line.toString());
        }
        return lines;
    }

    private float stringWidth(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private static PDRectangle rotate(PDRectangle r) {
        return new PDRectangle(r.getHeight(), r.getWidth());
    }

    private String joinRemarks(Project p) {
        if (p.getStages() == null) return "";
        StringBuilder sb = new StringBuilder();
        appendRemark(sb, "玻璃", p.getStages().getGlassRemark());
        appendRemark(sb, "框架", p.getStages().getFrameRemark());
        appendRemark(sb, "采购", p.getStages().getPurchaseRemark());
        appendRemark(sb, "运输", p.getStages().getTransportRemark());
        appendRemark(sb, "安装", p.getStages().getInstallRemark());
        appendRemark(sb, "维修", p.getStages().getRepairRemark());
        return sb.toString();
    }
    private void appendRemark(StringBuilder sb, String label, String remark) {
        if (remark != null && !remark.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(label).append(": ").append(remark.trim());
        }
    }
}
