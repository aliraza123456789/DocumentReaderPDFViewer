package com.pdf.alldocument.office.pdfreaderapp.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.pdf.alldocument.office.reader.officereader.ActivityFilesReader
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.presentation.favorites.FavoritesScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.recent.RecentScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.filemanager.FileManagerRoute
import kotlinx.serialization.ExperimentalSerializationApi
import com.pdf.alldocument.office.pdfreaderapp.presentation.home.HomeScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.language.LanguageScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.more.MoreScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.onboarding.IntroSlidesScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.privacy.PrivacyPolicyScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.premium.PremiumScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.search.GlobalSearchScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.splash.SplashScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.ToolsHomeScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.navigation.PdfToolsNavHost
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.theme.PdfToolsTheme
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.merge.MergePdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.organize.OrganizePdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.sign.SignPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.TextToPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.PdfToImageScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.PdfToLongImageScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.lock.LockPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.unlock.UnlockPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.split.SplitPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.CompressScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.imagetopdf.ImageToPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.scantopdf.ScanToPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.imagetotext.ImageToTextScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.idcardscanner.IdCardScannerScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.idphotomaker.IdPhotoScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.aireading.AiReadingScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.scantoexcel.ScanToExcelScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.officetopdf.OfficeToPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.wordtopdf.WordToPdfScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.pdftoword.PdfToWordScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.aipaywall.AiPaywallScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.viewer.office.OfficeViewerScreen
import com.pdf.alldocument.office.pdfreaderapp.presentation.viewer.pdf.PdfViewerScreen

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedBottomDestination = when (currentRoute) {
        Screen.Favorites.serializer().descriptor.serialName -> AppBottomDestination.FAVOURITES
        Screen.Home.serializer().descriptor.serialName -> AppBottomDestination.HOME
        Screen.Recent.serializer().descriptor.serialName -> AppBottomDestination.RECENT
        Screen.ToolsHome.serializer().descriptor.serialName -> AppBottomDestination.TOOLS
        else -> null
    }

    val openOfficeDoc: (DocumentItem) -> Unit = { doc ->
        try {
            val intent = ActivityFilesReader.launchDocFromPath(context, doc.path)
            context.startActivity(intent)
        } catch (e: Exception) {
            navController.navigate(Screen.OfficeViewer(filePath = doc.path, title = doc.name))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val selectedDestination = selectedBottomDestination
            if (selectedDestination != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Small Banner Ad placed above the bottom navigation bar
                    com.pdf.alldocument.office.pdfreaderapp.core.ads.BannerAdView(
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppBottomNavigation(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = { destination ->
                            if (destination != selectedDestination) {
                                navController.navigate(destination.route) {
                                    popUpTo(Screen.Home) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash,
            // Edge-to-edge: the status bar and any display cutout are handled
            // once here for every destination, then consumed so nested
            // TopAppBars / Scaffolds below do not pad themselves a second time.
            // Bottom stays with the bottom bar above.
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable<Screen.Splash> {
                SplashScreen(
                    onNavigateToLanguage = {
                        navController.navigate(Screen.LanguageSelection(isFirstLaunch = true)) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToPrivacyPolicy = {
                        navController.navigate(Screen.PrivacyPolicy(isFirstLaunch = true)) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }

            composable<Screen.LanguageSelection> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.LanguageSelection>()
                LanguageScreen(
                    isFirstLaunch = route.isFirstLaunch,
                    onBack = if (route.isFirstLaunch) null else ({ navController.popBackStack() }),
                    onLanguageSelected = {
                        if (route.isFirstLaunch) {
                            navController.navigate(Screen.Onboarding) {
                                popUpTo(Screen.LanguageSelection(isFirstLaunch = true)) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable<Screen.Onboarding> {
                IntroSlidesScreen(
                    onFinish = {
                        navController.navigate(Screen.PrivacyPolicy(isFirstLaunch = true)) {
                            popUpTo(Screen.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            composable<Screen.PrivacyPolicy> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.PrivacyPolicy>()
                PrivacyPolicyScreen(
                    isFirstLaunch = route.isFirstLaunch,
                    onAgreeAndContinue = {
                        if (route.isFirstLaunch) {
                            navController.navigate(Screen.Premium(isFromOnboarding = true)) {
                                popUpTo(Screen.PrivacyPolicy(isFirstLaunch = true)) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Home> {
                HomeScreen(
                    onNavigateToPdfViewer = { doc ->
                        navController.navigate(Screen.PdfViewer(filePath = doc.path, title = doc.name))
                    },
                    onNavigateToOfficeViewer = openOfficeDoc,
                    onNavigateToSearch = { navController.navigate(Screen.GlobalSearch()) },
                    onNavigateToTools = { navController.navigate(Screen.PdfToolsHome) },
                    onNavigateToPremium = { navController.navigate(Screen.Premium()) },
                    onNavigateToLanguage = { navController.navigate(Screen.LanguageSelection(isFirstLaunch = false)) }
                )
            }

            composable<Screen.FileManager> {
                FileManagerRoute(
                    onNavigateToPdfViewer = { doc ->
                        navController.navigate(Screen.PdfViewer(filePath = doc.path, title = doc.name))
                    },
                    onNavigateToOfficeViewer = openOfficeDoc,
                    onNavigateToSearch = { navController.navigate(Screen.GlobalSearch()) },
                    onNavigateToTools = { navController.navigate(Screen.ToolsHome) },
                    onNavigateToPremium = { navController.navigate(Screen.Premium()) },
                    onOpenMore = { navController.navigate(Screen.More) },
                    onOpenFavourites = { navController.navigate(Screen.Favorites) }
                )
            }

            composable<Screen.Favorites> {
                FavoritesScreen(
                    onNavigateToPdfViewer = { doc ->
                        navController.navigate(Screen.PdfViewer(filePath = doc.path, title = doc.name))
                    },
                    onNavigateToOfficeViewer = openOfficeDoc
                )
            }

            composable<Screen.Recent> {
                RecentScreen(
                    onNavigateToPdfViewer = { doc ->
                        navController.navigate(Screen.PdfViewer(filePath = doc.path, title = doc.name))
                    },
                    onNavigateToOfficeViewer = openOfficeDoc
                )
            }

            composable<Screen.More> {
                MoreScreen(
                    onNavigateToFileManager = { navController.navigate(Screen.FileManager) },
                    onNavigateToSearch = { navController.navigate(Screen.GlobalSearch()) },
                    onNavigateToTools = { navController.navigate(Screen.PdfToolsHome) },
                    onNavigateToPremium = { navController.navigate(Screen.Premium()) },
                    onNavigateToLanguage = { navController.navigate(Screen.LanguageSelection(isFirstLaunch = false)) },
                    onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy(isFirstLaunch = false)) }
                )
            }

            composable<Screen.GlobalSearch> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.GlobalSearch>()
                GlobalSearchScreen(
                    initialType = route.initialType,
                    onNavigateBack = { navController.popBackStack() },
                    onDocumentClick = { doc ->
                        if (doc.extension.equals("pdf", ignoreCase = true)) {
                            navController.navigate(Screen.PdfViewer(filePath = doc.path, title = doc.name))
                        } else {
                            openOfficeDoc(doc)
                        }
                    }
                )
            }

            composable<Screen.PdfViewer> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.PdfViewer>()
                PdfViewerScreen(
                    filePath = route.filePath,
                    title = route.title,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.OfficeViewer> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.OfficeViewer>()
                OfficeViewerScreen(
                    filePath = route.filePath,
                    title = route.title,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.PdfToolsHome> {
                PdfToolsTheme {
                    PdfToolsNavHost(
                        modifier = Modifier.fillMaxSize(),
                        onExit = { navController.popBackStack() }
                    )
                }
            }

            composable<Screen.ToolsHome> {
                ToolsHomeScreen(
                    onNavigateToMerge = { navController.navigate(Screen.MergePdf) },
                    onNavigateToSign = { path, title -> navController.navigate(Screen.SignPdf(filePath = path, title = title)) },
                    onNavigateToOrganize = { path, title -> navController.navigate(Screen.OrganizePdf(filePath = path, title = title)) },
                    onNavigateToTextToPdf = { navController.navigate(Screen.TextToPdf) },
                    onNavigateToPdfToImage = { navController.navigate(Screen.PdfToImage) },
                    onNavigateToPdfToLongImage = { navController.navigate(Screen.PdfToLongImage) },
                    onNavigateToLockPdf = { navController.navigate(Screen.LockPdf) },
                    onNavigateToUnlockPdf = { navController.navigate(Screen.UnlockPdf) },
                    onNavigateToSplitPdf = { navController.navigate(Screen.SplitPdf) },
                    onNavigateToImageToPdf = { navController.navigate(Screen.ImageToPdf) },
                    onNavigateToScanToPdf = { navController.navigate(Screen.ScanToPdf) },
                    onNavigateToImageToText = { navController.navigate(Screen.ImageToText) },
                    onNavigateToIdCardScanner = { navController.navigate(Screen.IdCardScanner) },
                    onNavigateToIdPhotoMaker = { navController.navigate(Screen.IdPhotoMaker) },
                    // onNavigateToAiReading = { navController.navigate(Screen.AiPaywall) }, // Fake subscription screen bypassed
                    onNavigateToAiReading = { navController.navigate(Screen.AiReading) },
                    onNavigateToScanToExcel = { navController.navigate(Screen.ScanToExcel) },
                    onNavigateToOfficeToPdf = { navController.navigate(Screen.OfficeToPdf) },
                    onNavigateToWordToPdf = { navController.navigate(Screen.WordToPdf) },
                    onNavigateToPdfToWord = { navController.navigate(Screen.PdfToWord) },
                    onNavigateToCompress = { navController.navigate(Screen.CompressPdf) }
                )
            }

            composable<Screen.ImageToPdf> {
                ImageToPdfScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.ScanToPdf> {
                ScanToPdfScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.ImageToText> {
                ImageToTextScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.IdCardScanner> {
                IdCardScannerScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.IdPhotoMaker> {
                IdPhotoScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.AiPaywall> {
                AiPaywallScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAiReading = {
                        navController.navigate(Screen.AiReading)
                    },
                    onNavigateToPrivacy = {
                        navController.navigate(Screen.PrivacyPolicy())
                    }
                )
            }

            composable<Screen.AiReading> {
                AiReadingScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.ScanToExcel> {
                ScanToExcelScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.OfficeToPdf> {
                OfficeToPdfScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.WordToPdf> {
                WordToPdfScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { path, title ->
                        navController.navigate(Screen.PdfViewer(filePath = path, title = title))
                    }
                )
            }

            composable<Screen.PdfToWord> {
                PdfToWordScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.TextToPdf> {
                TextToPdfScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.PdfToImage> {
                PdfToImageScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.PdfToLongImage> {
                PdfToLongImageScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.LockPdf> {
                LockPdfScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { path, title ->
                        navController.navigate(Screen.PdfViewer(filePath = path, title = title))
                    }
                )
            }

            composable<Screen.UnlockPdf> {
                UnlockPdfScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { path, title ->
                        navController.navigate(Screen.PdfViewer(filePath = path, title = title))
                    }
                )
            }

            composable<Screen.SplitPdf> {
                SplitPdfScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { path, title ->
                        navController.navigate(Screen.PdfViewer(filePath = path, title = title))
                    }
                )
            }

            composable<Screen.CompressPdf> {
                CompressScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.MergePdf> {
                MergePdfScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { path, title ->
                        navController.navigate(Screen.PdfViewer(filePath = path, title = title))
                    }
                )
            }

            composable<Screen.SignPdf> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.SignPdf>()
                SignPdfScreen(
                    filePath = route.filePath,
                    title = route.title,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { path, title ->
                        navController.navigate(Screen.PdfViewer(filePath = path, title = title))
                    }
                )
            }

            composable<Screen.OrganizePdf> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.OrganizePdf>()
                OrganizePdfScreen(
                    filePath = route.filePath,
                    title = route.title,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { path, title ->
                        navController.navigate(Screen.PdfViewer(filePath = path, title = title))
                    }
                )
            }

            composable<Screen.Premium> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.Premium>()
                PremiumScreen(
                    isFromOnboarding = route.isFromOnboarding,
                    onNavigateBack = {
                        if (route.isFromOnboarding) {
                            navController.navigate(Screen.Home) {
                                popUpTo(Screen.Premium(isFromOnboarding = true)) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
