package com.nichita.myvoyage.ui.offices

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.ui.export.ReportExportScreen

/** Экран экспорта отчёта по офису (общий UI — в [ReportExportScreen]). */
@Composable
fun OfficeExportScreen(
    onBack: () -> Unit,
    viewModel: OfficeExportViewModel = viewModel(factory = OfficeExportViewModel.Factory)
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    ReportExportScreen(report = report, onBack = onBack)
}
