package com.nichita.myvoyage.ui.vehicles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.ui.export.ReportExportScreen

/** Экран экспорта отчёта по автомобилю (общий UI — в [ReportExportScreen]). */
@Composable
fun VehicleExportScreen(
    onBack: () -> Unit,
    viewModel: VehicleExportViewModel = viewModel(factory = VehicleExportViewModel.Factory)
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    ReportExportScreen(report = report, onBack = onBack)
}
