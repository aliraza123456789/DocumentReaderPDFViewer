package com.pdf.alldocument.office.pdfreaderapp.data.repository

import android.graphics.Bitmap
import com.pdf.alldocument.office.pdfreaderapp.core.datastore.UserPreferencesManager
import com.pdf.alldocument.office.pdfreaderapp.data.pdf.PdfToolsManager
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.PdfToolsRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.UserPrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfToolsRepositoryImpl @Inject constructor(
    private val pdfToolsManager: PdfToolsManager
) : PdfToolsRepository {

    override suspend fun mergePdfs(pdfPaths: List<String>, outputFileName: String): Result<String> {
        return pdfToolsManager.mergePdfs(pdfPaths, outputFileName)
    }

    override suspend fun saveSignedPdf(
        originalPdfPath: String,
        signedPages: List<Bitmap>,
        outputFileName: String
    ): Result<String> {
        return pdfToolsManager.saveSignedPdf(originalPdfPath, signedPages, outputFileName)
    }

    override suspend fun organizeAndExportPdf(
        originalPdfPath: String,
        selectedPages: List<Int>,
        outputFileName: String
    ): Result<String> {
        return pdfToolsManager.organizeAndExportPdf(originalPdfPath, selectedPages, outputFileName)
    }

    override suspend fun renderAllPagesToBitmaps(filePath: String): List<Bitmap> {
        return pdfToolsManager.renderAllPagesToBitmaps(filePath)
    }

    override suspend fun lockPdf(
        originalPdfPath: String,
        password: String,
        outputFileName: String
    ): Result<String> {
        return pdfToolsManager.lockPdf(originalPdfPath, password, outputFileName)
    }
}

@Singleton
class UserPrefsRepositoryImpl @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : UserPrefsRepository {

    override val themeMode: Flow<String> = preferencesManager.themeModeFlow

    override val sortOrder: Flow<SortOrder> = preferencesManager.sortOrderFlow.map {
        try {
            SortOrder.valueOf(it)
        } catch (e: Exception) {
            SortOrder.DATE_DESC
        }
    }

    override val languageCode: Flow<String> = preferencesManager.languageFlow
    override val isNightMode: Flow<Boolean> = preferencesManager.nightModeFlow
    override val keepScreenOn: Flow<Boolean> = preferencesManager.keepScreenOnFlow
    override val isHorizontalScroll: Flow<Boolean> = preferencesManager.horizontalScrollFlow
    override val isOnboardingCompleted: Flow<Boolean> = preferencesManager.onboardingCompletedFlow
    override val isGridView: Flow<Boolean> = preferencesManager.isGridViewFlow
    override val isPrivacyPolicyAccepted: Flow<Boolean> = preferencesManager.privacyPolicyAcceptedFlow

    override suspend fun setThemeMode(mode: String) {
        preferencesManager.setThemeMode(mode)
    }

    override suspend fun setSortOrder(order: SortOrder) {
        preferencesManager.setSortOrder(order.name)
    }

    override suspend fun setLanguageCode(code: String) {
        preferencesManager.setLanguage(code)
    }

    override suspend fun setNightMode(enabled: Boolean) {
        preferencesManager.setNightMode(enabled)
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        preferencesManager.setKeepScreenOn(enabled)
    }

    override suspend fun setHorizontalScroll(enabled: Boolean) {
        preferencesManager.setHorizontalScroll(enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }

    override suspend fun setGridView(isGrid: Boolean) {
        preferencesManager.setGridView(isGrid)
    }

    override suspend fun setPrivacyPolicyAccepted(accepted: Boolean) {
        preferencesManager.setPrivacyPolicyAccepted(accepted)
    }
}
