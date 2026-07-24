package com.nichita.myvoyage.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Рендер отчёта в PDF средствами Android [PdfDocument] (без внешних библиотек).
 * Формат A4, аккуратные таблицы с винной шапкой и чередованием строк,
 * автоматический перенос на новую страницу.
 */
object PdfExporter {

    private const val W = 595   // A4, точки
    private const val H = 842
    private const val M = 40f   // поля

    private val WINE = 0xFF5F152E.toInt()
    private val INK = 0xFF222222.toInt()
    private val MUTED = 0xFF777777.toInt()
    private val ALT = 0xFFF4EEF1.toInt()
    private val LINE = 0xFFDDD3D8.toInt()

    fun export(context: Context, report: TripReport): File {
        val doc = PdfDocument()

        val pTitle = paint(WINE, 22f, bold = true)
        val pSub = paint(MUTED, 11f)
        val pH2 = paint(WINE, 14f, bold = true)
        val pBody = paint(INK, 10.5f)
        val pHead = paint(Color.WHITE, 10.5f, bold = true)
        val headBg = Paint().apply { color = WINE }
        val altBg = Paint().apply { color = ALT }
        val linePaint = Paint().apply { color = LINE; strokeWidth = 0.7f }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNum).create())
        var canvas = page.canvas
        var y = M
        val contentW = W - 2 * M

        fun newPage() {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNum).create())
            canvas = page.canvas
            y = M
        }
        fun need(h: Float) { if (y + h > H - M) newPage() }

        // --- Шапка отчёта ---
        canvas.drawText(report.kicker, M, y + 11, pSub); y += 20
        canvas.drawText(report.title, M, y + 18, pTitle); y += 30
        canvas.drawText(report.period, M, y + 11, pSub); y += 22
        canvas.drawText("Итого потрачено: ${report.totalText}", M, y + 13, pH2); y += 24
        report.summaryLines.forEach { canvas.drawText(it, M, y + 11, pBody); y += 15 }
        y += 12

        // --- Таблицы ---
        report.tables.forEach { table ->
            if (table.rows.isEmpty()) return@forEach
            need(50f)
            canvas.drawText(table.title, M, y + 12, pH2); y += 20

            val sum = table.weights.sum()
            val x = FloatArray(table.headers.size + 1)
            x[0] = M
            for (i in table.headers.indices) x[i + 1] = x[i] + contentW * (table.weights[i] / sum)
            val rowH = 18f

            fun header() {
                canvas.drawRect(M, y, M + contentW, y + rowH, headBg)
                table.headers.forEachIndexed { i, h ->
                    canvas.drawText(clip(h, pHead, x[i + 1] - x[i] - 8), x[i] + 4, y + 12.5f, pHead)
                }
                y += rowH
            }

            need(rowH * 2); header()
            table.rows.forEachIndexed { idx, row ->
                if (y + rowH > H - M) { newPage(); header() }
                if (idx % 2 == 1) canvas.drawRect(M, y, M + contentW, y + rowH, altBg)
                row.forEachIndexed { i, cell ->
                    val cellW = x[i + 1] - x[i] - 8
                    val text = clip(cell, pBody, cellW)
                    if (i == row.size - 1) { // последняя колонка — суммы, по правому краю
                        canvas.drawText(text, x[i + 1] - 4 - pBody.measureText(text), y + 12.5f, pBody)
                    } else {
                        canvas.drawText(text, x[i] + 4, y + 12.5f, pBody)
                    }
                }
                y += rowH
            }
            canvas.drawLine(M, y, M + contentW, y, linePaint)
            y += 18
        }

        need(20f)
        canvas.drawText(report.generatedAt, M, y + 10, pSub)

        doc.finishPage(page)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, report.fileName("pdf"))
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun paint(c: Int, size: Float, bold: Boolean = false) = Paint().apply {
        color = c; textSize = size; isFakeBoldText = bold; isAntiAlias = true
    }

    /** Обрезает текст с многоточием, чтобы влез в ширину колонки. */
    private fun clip(text: String, paint: Paint, maxW: Float): String {
        if (paint.measureText(text) <= maxW) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }
}
