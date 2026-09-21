package com.nichita.myvoyage.export

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Рендер отчёта в Excel (.xlsx) — это ZIP с XML (Office Open XML / SpreadsheetML).
 * Части собираются вручную, без Apache POI: на Android это надёжнее и легче.
 *
 * Книга состоит из нескольких листов («Сводка» + отдельный лист на каждую
 * таблицу отчёта). Текст в ячейках — inline strings, чтобы не возиться с
 * sharedStrings. Оформление (винная шапка, рамки, чередование строк) задаётся
 * стилями из [STYLES].
 */
object XlsxExporter {

    private const val WINE = "FF5F152E"
    private const val ALT = "FFF4EEF1"
    private const val BORDER = "FFDDD3D8"

    // Индексы стилей из cellXfs в styles.xml
    private const val S_TITLE = 1
    private const val S_SUB = 2
    private const val S_TOTAL = 3
    private const val S_HEAD = 4
    private const val S_CELL = 5
    private const val S_CELL_R = 6
    private const val S_CELL_ALT = 7
    private const val S_CELL_ALT_R = 8

    private data class Sheet(val name: String, val xml: String)

    fun export(context: Context, report: TripReport): File {
        val sheets = mutableListOf<Sheet>()

        // Лист 1 — шапка отчёта + разбивка по категориям.
        val header = buildList {
            add(report.title to S_TITLE)
            add(report.period to S_SUB)
            add("Итого потрачено: ${report.totalText}" to S_TOTAL)
            report.summaryLines.forEach { add(it to S_CELL) }
            add(report.generatedAt to S_SUB)
        }
        sheets += Sheet("Сводка", sheetXml(header, report.tables.firstOrNull()))

        // Остальные таблицы — каждая на отдельном листе.
        report.tables.drop(1).forEach { t ->
            sheets += Sheet(sheetName(t.title), sheetXml(emptyList(), t))
        }

        val dir = ExportCache.dir(context).apply { mkdirs() }
        val file = File(dir, report.fileName("xlsx"))
        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zip ->
            zip.put("[Content_Types].xml", contentTypes(sheets.size))
            zip.put("_rels/.rels", RELS)
            zip.put("xl/workbook.xml", workbook(sheets))
            zip.put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            zip.put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, s -> zip.put("xl/worksheets/sheet${i + 1}.xml", s.xml) }
        }
        return file
    }

    /** Один лист: необязательная шапка (текст+стиль) и одна таблица. */
    private fun sheetXml(header: List<Pair<String, Int>>, table: ReportTable?): String {
        val cols = table?.headers?.size ?: 1
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        if (table != null) {
            sb.append("<cols>")
            val weightSum = table.weights.sum().takeIf { it > 0 } ?: 1f
            table.weights.forEachIndexed { i, w ->
                val width = (10 + w / weightSum * 60).coerceIn(12f, 60f)
                sb.append(
                    """<col min="${i + 1}" max="${i + 1}" """ +
                        """width="${String.format(Locale.US, "%.1f", width)}" customWidth="1"/>"""
                )
            }
            sb.append("</cols>")
        }

        sb.append("<sheetData>")
        var r = 1
        val merges = mutableListOf<String>()

        header.forEach { (text, style) ->
            sb.append(row(r, listOf(cell("${col(0)}$r", style, text))))
            if (cols > 1) merges.add("${col(0)}$r:${col(cols - 1)}$r")
            r++
        }
        if (header.isNotEmpty()) r++ // пустая строка-отступ

        if (table != null) {
            sb.append(row(r, listOf(cell("${col(0)}$r", S_TOTAL, table.title))))
            if (cols > 1) merges.add("${col(0)}$r:${col(cols - 1)}$r")
            r++

            sb.append(row(r, table.headers.mapIndexed { i, h -> cell("${col(i)}$r", S_HEAD, h) }))
            r++

            table.rows.forEachIndexed { idx, dataRow ->
                val alt = idx % 2 == 1
                val cells = dataRow.mapIndexed { i, c ->
                    val last = i == dataRow.size - 1
                    val style = when {
                        last && alt -> S_CELL_ALT_R
                        last -> S_CELL_R
                        alt -> S_CELL_ALT
                        else -> S_CELL
                    }
                    cell("${col(i)}$r", style, c)
                }
                sb.append(row(r, cells))
                r++
            }
        }
        sb.append("</sheetData>")

        if (merges.isNotEmpty()) {
            sb.append("""<mergeCells count="${merges.size}">""")
            merges.forEach { sb.append("""<mergeCell ref="$it"/>""") }
            sb.append("</mergeCells>")
        }
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun row(r: Int, cells: List<String>): String =
        """<row r="$r">${cells.joinToString("")}</row>"""

    private fun cell(ref: String, style: Int, text: String): String =
        """<c r="$ref" s="$style" t="inlineStr"><is><t xml:space="preserve">${esc(text)}</t></is></c>"""

    /** Индекс колонки (0-based) → буква(ы) Excel: 0→A, 25→Z, 26→AA. */
    private fun col(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, 'A' + i % 26)
            i = i / 26 - 1
            if (i < 0) break
        }
        return sb.toString()
    }

    /** Имя листа: без запрещённых символов, не длиннее 31. */
    private fun sheetName(raw: String): String =
        raw.replace(Regex("[\\[\\]:*?/\\\\]"), " ").trim().take(31).ifEmpty { "Лист" }

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

    private fun contentTypes(sheetCount: Int): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        sb.append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        sb.append("""<Default Extension="xml" ContentType="application/xml"/>""")
        sb.append(
            """<Override PartName="/xl/workbook.xml" """ +
                """ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>"""
        )
        sb.append(
            """<Override PartName="/xl/styles.xml" """ +
                """ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>"""
        )
        for (i in 1..sheetCount) {
            sb.append(
                """<Override PartName="/xl/worksheets/sheet$i.xml" """ +
                    """ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
            )
        }
        sb.append("</Types>")
        return sb.toString()
    }

    private fun workbook(sheets: List<Sheet>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append(
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
                """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>"""
        )
        sheets.forEachIndexed { i, s ->
            sb.append("""<sheet name="${esc(s.name)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        sb.append("</sheets></workbook>")
        return sb.toString()
    }

    private fun workbookRels(sheetCount: Int): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            sb.append(
                """<Relationship Id="rId$i" """ +
                    """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" """ +
                    """Target="worksheets/sheet$i.xml"/>"""
            )
        }
        sb.append(
            """<Relationship Id="rId${sheetCount + 1}" """ +
                """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" """ +
                """Target="styles.xml"/>"""
        )
        sb.append("</Relationships>")
        return sb.toString()
    }

    private val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" """ +
        """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" """ +
        """Target="xl/workbook.xml"/></Relationships>"""

    private val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
        """<fonts count="6">""" +
        """<font><sz val="11"/><color rgb="FF222222"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="20"/><color rgb="$WINE"/><name val="Calibri"/></font>""" +
        """<font><sz val="10"/><color rgb="FF777777"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="14"/><color rgb="$WINE"/><name val="Calibri"/></font>""" +
        """<font><sz val="11"/><color rgb="FF222222"/><name val="Calibri"/></font>""" +
        """</fonts>""" +
        """<fills count="4">""" +
        """<fill><patternFill patternType="none"/></fill>""" +
        """<fill><patternFill patternType="gray125"/></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="$WINE"/><bgColor indexed="64"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="$ALT"/><bgColor indexed="64"/></patternFill></fill>""" +
        """</fills>""" +
        """<borders count="2">""" +
        """<border><left/><right/><top/><bottom/><diagonal/></border>""" +
        """<border>""" +
        """<left style="thin"><color rgb="$BORDER"/></left>""" +
        """<right style="thin"><color rgb="$BORDER"/></right>""" +
        """<top style="thin"><color rgb="$BORDER"/></top>""" +
        """<bottom style="thin"><color rgb="$BORDER"/></bottom>""" +
        """<diagonal/></border>""" +
        """</borders>""" +
        """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
        """<cellXfs count="9">""" +
        """<xf xfId="0" fontId="0" fillId="0" borderId="0"/>""" +
        """<xf xfId="0" fontId="1" fillId="0" borderId="0" applyFont="1"/>""" +
        """<xf xfId="0" fontId="2" fillId="0" borderId="0" applyFont="1"/>""" +
        """<xf xfId="0" fontId="4" fillId="0" borderId="0" applyFont="1"/>""" +
        """<xf xfId="0" fontId="3" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" """ +
        """applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>""" +
        """<xf xfId="0" fontId="5" fillId="0" borderId="1" applyFont="1" applyBorder="1" """ +
        """applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>""" +
        """<xf xfId="0" fontId="5" fillId="0" borderId="1" applyFont="1" applyBorder="1" """ +
        """applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        """<xf xfId="0" fontId="5" fillId="3" borderId="1" applyFont="1" applyFill="1" applyBorder="1" """ +
        """applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>""" +
        """<xf xfId="0" fontId="5" fillId="3" borderId="1" applyFont="1" applyFill="1" applyBorder="1" """ +
        """applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
        """</cellXfs>""" +
        """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
        """</styleSheet>"""
}
