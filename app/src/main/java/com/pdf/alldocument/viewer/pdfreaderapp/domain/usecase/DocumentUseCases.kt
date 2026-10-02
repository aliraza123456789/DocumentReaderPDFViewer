package com.pdf.alldocument.viewer.pdfreaderapp.domain.usecase

import android.graphics.Bitmap
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.DocumentRepository
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.PdfToolsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(
        type: DocumentType,
        sortOrder: SortOrder,
        forceRefresh: Boolean = false
    ): Flow<List<DocumentItem>> {
        return repository.getDocuments(type, sortOrder, forceRefresh)
    }
}

class SearchDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(query: String, type: DocumentType): Flow<List<DocumentItem>> {
        return repository.searchDocuments(query, type)
    }
}

class MergePdfUseCase @Inject constructor(
    private val toolsRepository: PdfToolsRepository
) {
    suspend operator fun invoke(pdfPaths: List<String>, outputFileName: String): Result<String> {
        return toolsRepository.mergePdfs(pdfPaths, outputFileName)
    }
}

class SignPdfUseCase @Inject constructor(
    private val toolsRepository: PdfToolsRepository
) {
    suspend operator fun invoke(
        originalPdfPath: String,
        signedPages: List<Bitmap>,
        outputFileName: String
    ): Result<String> {
        return toolsRepository.saveSignedPdf(originalPdfPath, signedPages, outputFileName)
    }
}

class OrganizePdfUseCase @Inject constructor(
    private val toolsRepository: PdfToolsRepository
) {
    suspend operator fun invoke(
        originalPdfPath: String,
        selectedPages: List<Int>,
        outputFileName: String
    ): Result<String> {
        return toolsRepository.organizeAndExportPdf(originalPdfPath, selectedPages, outputFileName)
    }
}
