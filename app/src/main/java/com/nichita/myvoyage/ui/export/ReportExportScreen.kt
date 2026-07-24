package com.nichita.myvoyage.ui.export

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nichita.myvoyage.export.DocxExporter
import com.nichita.myvoyage.export.PdfExporter
import com.nichita.myvoyage.export.ReportSharing
import com.nichita.myvoyage.export.TripReport
import com.nichita.myvoyage.export.XlsxExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Форматы экспорта отчёта. */
private enum class ExportKind(val label: String, val hint: String) {
    PDF("Скачать PDF", "Готовый к печати отчёт A4"),
    WORD("Скачать Word", "Документ .docx для редактирования"),
    EXCEL("Скачать Excel", "Таблицы .xlsx с листами по разделам")
}

/**
 * Общий экран экспорта отчёта (рейс/офис/автомобиль): превью и три кнопки —
 * PDF, Word и Excel. Файл генерируется в IO-потоке и сразу открывается
 * системным окном «Поделиться/Сохранить». Пока [report] == null (данные ещё
 * готовятся) показывается индикатор загрузки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportExportScreen(
    report: TripReport?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun export(kind: ExportKind) {
        val r = report ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                val (file, mime) = withContext(Dispatchers.IO) {
                    when (kind) {
                        ExportKind.PDF -> PdfExporter.export(context, r) to ReportSharing.MIME_PDF
                        ExportKind.WORD -> DocxExporter.export(context, r) to ReportSharing.MIME_DOCX
                        ExportKind.EXCEL -> XlsxExporter.export(context, r) to ReportSharing.MIME_XLSX
                    }
                }
                ReportSharing.share(context, file, mime)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Не удалось создать файл: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                busy = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Экспорт отчёта") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { padding ->
        val r = report
        if (r == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportPreview(r)

            ExportKind.entries.forEach { kind ->
                Button(
                    onClick = { export(kind) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(kind.label, style = MaterialTheme.typography.titleSmall)
                        Text(kind.hint, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (busy) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            Text(
                "Файл можно сохранить на устройство или отправить в мессенджере.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportPreview(report: TripReport) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(report.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                report.period,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text("Итого потрачено", style = MaterialTheme.typography.labelMedium)
            Text(report.totalText, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            report.summaryLines.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Text("В отчёт войдут таблицы:", style = MaterialTheme.typography.labelMedium)
            report.tables.forEach { t ->
                Text("• ${t.title}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
