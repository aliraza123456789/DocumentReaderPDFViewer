package com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.repository.PdfRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CompressWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: PdfRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // TODO: inputData se source/output le kar repo.compressPdf() call karein
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
