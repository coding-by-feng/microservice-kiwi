package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.tools.model.Project;
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
                "项目ID", "项目编号", "项目名称", "客户", "客户电话", "地址", "销售", "安装", "团队成员",
                "开始日期", "结束日期", "状态", "今日任务", "进度备注", "创建时间", "已归档"
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
                row.createCell(4).setCellValue(nvl(p.getClientPhone()));
                row.createCell(5).setCellValue(nvl(p.getAddress()));
                row.createCell(6).setCellValue(nvl(p.getSalesPerson()));
                row.createCell(7).setCellValue(nvl(p.getInstaller()));
                row.createCell(8).setCellValue(nvl(p.getTeamMembers()));
                row.createCell(9).setCellValue(nvl(p.getStartDate()));
                row.createCell(10).setCellValue(nvl(p.getEndDate()));
                row.createCell(11).setCellValue(p.getStatus() == null ? "" : p.getStatus().getLabelZh());
                row.createCell(12).setCellValue(nvl(p.getTodayTask()));
                row.createCell(13).setCellValue(nvl(p.getProgressNote()));
                row.createCell(14).setCellValue(formatDateTime(p));
                row.createCell(15).setCellValue(yn(p.getArchived()));
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] toPdf(List<Project> projects) throws IOException {
        // Page/layout constants
        // final PDRectangle PAGE_SIZE = PDRectangle.A4.rotate();
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

            // Table header
            String[] headers = new String[]{
                "ID", "编号", "名称", "客户", "地址", "开始", "结束", "状态", "归档", "创建时间"
            };
            // Column widths (percentages sum ~100%)
            float[] colPercents = new float[]{6, 10, 18, 12, 18, 8, 8, 8, 5, 7};
            float[] colWidths = new float[colPercents.length];
            for (int i = 0; i < colPercents.length; i++) colWidths[i] = tableWidth * (colPercents[i] / 100f);

            // Draw header background
            cs.setNonStrokingColor(240); // light gray
            cs.addRect(tableLeft, y - TABLE_HEADER_HEIGHT, tableWidth, TABLE_HEADER_HEIGHT);
            cs.fill();
            cs.setNonStrokingColor(0); // reset to black

            // Header texts
            float x = tableLeft + 2f;
            cs.beginText();
            cs.setFont(headerFont, 9);
            cs.newLineAtOffset(x, y - TABLE_HEADER_HEIGHT + 4f);
            for (int i = 0; i < headers.length; i++) {
                cs.showText(sanitizeForFont(headerFont, headers[i]));
                // move to next column
                x += colWidths[i];
                cs.newLineAtOffset(colWidths[i], 0); // this offset accumulates; we handle manually by resetting text on each cell below
            }
            cs.endText();
            // Re-draw header texts cell-by-cell to control positioning
            x = tableLeft + 4f;
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

            // Row rendering function
            for (Project p : projects) {
                // Prepare row cell strings (single line cells)
                String[] row = new String[]{
                    nvl(p.getId()),
                    nvl(p.getProjectCode()),
                    nvl(p.getName()),
                    nvl(p.getClientName()),
                    nvl(p.getAddress()),
                    nvl(p.getStartDate()),
                    nvl(p.getEndDate()),
                    p.getStatus() == null ? "" : p.getStatus().getLabelZh(),
                    yn(p.getArchived()),
                    formatDateTime(p)
                };
                String detail = "任务: " + nvl(p.getTodayTask()) + " | 备注: " + nvl(p.getProgressNote());

                // Wrap each cell to fit width (max 2 lines for cells; detail can be multi-line)
                List<List<String>> wrappedCells = new ArrayList<>();
                int maxCellLines = 1;
                for (int i = 0; i < row.length; i++) {
                    List<String> lines = wrapText(cellFont, 8, row[i], colWidths[i] - 6f);
                    if (lines.size() > 2) lines = lines.subList(0, 2); // cap to 2 lines to keep table compact
                    wrappedCells.add(lines);
                    maxCellLines = Math.max(maxCellLines, lines.size());
                }
                List<String> detailLines = wrapText(cellFont, 8, detail, tableWidth - 6f);

                float rowHeight = (maxCellLines * ROW_LINE_HEIGHT);
                float detailHeight = detailLines.isEmpty() ? 0f : (DETAIL_LINE_GAP + detailLines.size() * ROW_LINE_HEIGHT);
                float requiredHeight = rowHeight + detailHeight + 6f; // padding

                // Pagination check
                if (y - requiredHeight < MARGIN) {
                    // new page
                    cs.close();
                    page = new PDPage(PAGE_SIZE);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageHeight - MARGIN;

                    // redraw header background
                    cs.setNonStrokingColor(240);
                    cs.addRect(tableLeft, y - TABLE_HEADER_HEIGHT, tableWidth, TABLE_HEADER_HEIGHT);
                    cs.fill();
                    cs.setNonStrokingColor(0);
                    // redraw header texts
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

                // Draw row cells text
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

                // Draw detail line(s)
                float detailYStart = y - rowHeight - DETAIL_LINE_GAP;
                for (int i = 0; i < detailLines.size(); i++) {
                    cs.beginText();
                    cs.setFont(cellFont, 8);
                    cs.newLineAtOffset(tableLeft + 3f, detailYStart - (i * ROW_LINE_HEIGHT));
                    cs.showText(sanitizeForFont(cellFont, detailLines.get(i)));
                    cs.endText();
                }

                // Optional: draw horizontal separators
                cs.moveTo(tableLeft, y);
                cs.lineTo(tableRight, y);
                cs.stroke();
                cs.moveTo(tableLeft, y - requiredHeight);
                cs.lineTo(tableRight, y - requiredHeight);
                cs.stroke();

                y -= requiredHeight;
            }

            // draw bottom line
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
        // yyyy-MM-dd HH:mm for compact readability
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return fmt.format(p.getCreatedAt());
        } catch (Exception e) {
            return p.getCreatedAt().toString();
        }
    }

    private String yn(Boolean b) { return Boolean.TRUE.equals(b) ? "是" : "否"; }

    // ---------- PDF font helpers ----------

    private PDFont loadCjkFontOrDefault(PDDocument doc, boolean bold) {
        // Try a few common CJK fonts packaged under /fonts
        String[] candidates = bold
            ? new String[]{"/fonts/NotoSansSC-Bold.otf", "/fonts/NotoSansCJKsc-Bold.otf", "/fonts/SourceHanSansCN-Bold.otf", "/fonts/SimHei.ttf"}
            : new String[]{"/fonts/NotoSansSC-Regular.otf", "/fonts/NotoSansCJKsc-Regular.otf", "/fonts/SourceHanSansCN-Regular.otf", "/fonts/SimSun.ttf"};
        for (String path : candidates) {
            PDFont f = tryLoadFont(doc, path);
            if (f != null) return f;
        }
        // Fallback (no Chinese glyphs) – will sanitize text to ASCII subset
        return bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
    }

    private PDFont tryLoadFont(PDDocument doc, String classpath) {
        try (InputStream in = ExportService.class.getResourceAsStream(classpath)) {
            if (in == null) return null;
            return PDType0Font.load(doc, in, true); // embed
        } catch (Throwable ignore) {
            return null;
        }
    }

    private String sanitizeForFont(PDFont font, String text) {
        // If we’re on a Unicode font (PDType0Font), assume it has glyphs; return as-is.
        if (font instanceof PDType0Font) return text;
        // Fallback Type1 fonts: strip/replace non-ASCII to avoid encode errors.
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            if (cp >= 32 && cp <= 126) {
                sb.appendCodePoint(cp);
            } else {
                sb.append('?');
            }
        });
        return sb.toString();
    }

    // ---------- text wrap helper ----------
    private List<String> wrapText(PDFont font, float fontSize, String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        String[] words = text.replace("\r", "").split("\n");
        for (String paragraph : words) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                line.append(paragraph.charAt(i));
                float w = stringWidth(font, fontSize, line.toString());
                if (w > maxWidth) {
                    // back off one char if possible
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
        // approximate width; safe for both Type0 and Type1 fonts
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    // ---------- rectangle helper (landscape) ----------
    private static PDRectangle rotate(PDRectangle r) {
        return new PDRectangle(r.getHeight(), r.getWidth());
    }
}
