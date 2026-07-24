package com.nichita.myvoyage.export

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Рендер отчёта в Word (.docx) — это ZIP с XML (Office Open XML).
 * Собираем минимально необходимые части вручную, без Apache POI:
 * на Android это надёжнее и без тяжёлых зависимостей.
 */
object DocxExporter {

    private const val WINE = "5F152E"
    private const val INK = "222222"
    private const val MUTED = "777777"
    private const val ALT = "F4EEF1"
    private const val BORDER = "DDD3D8"
    private const val TOTAL_W = 9000 // ширина таблицы в dxa

    fun export(context: Context, report: TripReport): File {
        val b = StringBuilder()
        b.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        b.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")

        b.append(para(report.kicker, 22, MUTED))
        b.append(para(report.title, 44, WINE, bold = true))
        b.append(para(report.period, 22, MUTED))
        b.append(para("Итого потрачено: ${report.totalText}", 30, WINE, bold = true))
        report.summaryLines.forEach { b.append(para(it, 22, INK)) }
        b.append(para("", 10, INK))

        report.tables.forEach { t ->
            if (t.rows.isEmpty()) return@forEach
            b.append(para(t.title, 28, WINE, bold = true))
            b.append(table(t))
            b.append(para("", 10, INK))
        }

        b.append(para(report.generatedAt, 18, MUTED))
        b.append(
            """<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>""" +
                """<w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr>"""
        )
        b.append("""</w:body></w:document>""")

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, report.fileName("docx"))
        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zip ->
            zip.put("[Content_Types].xml", CONTENT_TYPES)
            zip.put("_rels/.rels", RELS)
            zip.put("word/document.xml", b.toString())
        }
        return file
    }

    private fun table(t: ReportTable): String {
        val sum = t.weights.sum()
        fun colW(i: Int) = (TOTAL_W * t.weights[i] / sum).toInt()

        val b = StringBuilder()
        b.append("""<w:tbl><w:tblPr><w:tblW w:w="$TOTAL_W" w:type="dxa"/><w:tblBorders>""")
        listOf("top", "left", "bottom", "right", "insideH", "insideV").forEach {
            b.append("""<w:$it w:val="single" w:sz="4" w:space="0" w:color="$BORDER"/>""")
        }
        b.append("""</w:tblBorders></w:tblPr><w:tblGrid>""")
        t.headers.indices.forEach { b.append("""<w:gridCol w:w="${colW(it)}"/>""") }
        b.append("""</w:tblGrid>""")

        // Шапка
        b.append("<w:tr>")
        t.headers.forEachIndexed { i, h -> b.append(cell(h, colW(i), "FFFFFF", bold = true, fill = WINE)) }
        b.append("</w:tr>")

        // Строки
        t.rows.forEach { row ->
            b.append("<w:tr>")
            row.forEachIndexed { i, c ->
                b.append(cell(c, colW(i), INK, bold = false, right = i == row.size - 1))
            }
            b.append("</w:tr>")
        }
        b.append("</w:tbl>")
        return b.toString()
    }

    private fun cell(
        text: String,
        w: Int,
        color: String,
        bold: Boolean,
        fill: String? = null,
        right: Boolean = false
    ): String {
        val shd = if (fill != null) """<w:shd w:val="clear" w:color="auto" w:fill="$fill"/>""" else ""
        val jc = if (right) """<w:jc w:val="right"/>""" else ""
        val b = if (bold) "<w:b/>" else ""
        return """<w:tc><w:tcPr><w:tcW w:w="$w" w:type="dxa"/>$shd</w:tcPr>""" +
            """<w:p><w:pPr>$jc</w:pPr><w:r><w:rPr>$b<w:color w:val="$color"/><w:sz w:val="20"/></w:rPr>""" +
            """<w:t xml:space="preserve">${esc(text)}</w:t></w:r></w:p></w:tc>"""
    }

    private fun para(text: String, sz: Int, color: String, bold: Boolean = false): String {
        val b = if (bold) "<w:b/>" else ""
        return """<w:p><w:r><w:rPr>$b<w:color w:val="$color"/><w:sz w:val="$sz"/></w:rPr>""" +
            """<w:t xml:space="preserve">${esc(text)}</w:t></w:r></w:p>"""
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun ZipOutputStream.put(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
        """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
        """<Default Extension="xml" ContentType="application/xml"/>""" +
        """<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>""" +
        """</Types>"""

    private val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>""" +
        """</Relationships>"""
}
