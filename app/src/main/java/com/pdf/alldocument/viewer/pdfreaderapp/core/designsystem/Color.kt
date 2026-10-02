package com.pdf.alldocument.viewer.pdfreaderapp.core.designsystem

import androidx.compose.ui.graphics.Color

/*
 * Brand palette — PURPLE (was red).
 *
 * The brand gradient is #7B61FF -> #A259FF. Note that #7B61FF only reaches
 * ~3.9:1 against white, which FAILS WCAG AA for text and small icons, so it is
 * used for decorative fills and gradients only. `BrandPurpleDeep` (#6244E8,
 * ~6.0:1 on white) is the token used for anything that carries meaning:
 * M3 `primary`, icons, labels, tints.
 */

// ---- Brand gradient (decorative) ----
val BrandPurple = Color(0xFF7B61FF)
val BrandPurpleEnd = Color(0xFFA259FF)

// ---- Brand primary, contrast-safe ----
val BrandPurpleDeep = Color(0xFF6244E8)
val BrandPurpleContainer = Color(0xFFE7DEFF)
val OnBrandPurpleContainer = Color(0xFF1B0B4D)

// ---- Surfaces ----
val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF5F5F7)
val OnSurfaceLight = Color(0xFF1A1A2E)
val OnSurfaceVariantLight = Color(0xFF8A8A9E)

// ---- Dark surfaces: purple-tinted rather than neutral grey ----
val BackgroundDark = Color(0xFF14121F)
val SurfaceDark = Color(0xFF1C1930)
val SurfaceVariantDark = Color(0xFF2A2640)
val OnSurfaceDark = Color(0xFFEDEBF5)
val OnSurfaceVariantDark = Color(0xFFA9A3C4)

// ---- Secondary (analogous violet) ----
val SecondaryPurple = Color(0xFF6D4FD8)
val SecondaryPurpleContainer = Color(0xFFE6DEFF)
val OnSecondaryPurpleContainer = Color(0xFF1E1147)

// ---- Error ----
val ErrorLight = Color(0xFFB3261E)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
val ErrorDark = Color(0xFFF2B8B5)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

// ---- Document type identity (semantic — stays visually distinct) ----
val ColorPdf = Color(0xFFFF2D55)
val ColorWord = Color(0xFF2979FF)
val ColorExcel = Color(0xFF2E7D32)
val ColorPpt = Color(0xFFFF7A00)
val ColorTxt = Color(0xFF7B61FF)
val ColorAll = Color(0xFFA259FF)

// ---- Accents ----
val AccentGold = Color(0xFFFFA726)
val AccentGoldContainer = Color(0xFFFFF3E0)
val DangerRed = Color(0xFFD32F2F)
val SuccessGreen = Color(0xFF2E7D32)

/*
 * ---------------------------------------------------------------------------
 * Legacy aliases
 * ---------------------------------------------------------------------------
 * These predate the purple rebrand and still carry "Red"/"Teal" in their names.
 * They are kept so the ~90 existing call sites keep compiling and pick up the
 * new purple automatically. A rename sweep can be done separately.
 */
val PrimaryRed = BrandPurpleDeep
val PrimaryDarkRed = Color(0xFF4A2FB8)
val PrimaryRedLight = OnBrandPurpleContainer
val PrimaryRedContainer = BrandPurpleContainer
val OnPrimaryRedContainer = OnBrandPurpleContainer
val SecondaryTeal = SecondaryPurple
val SecondaryTealContainer = SecondaryPurpleContainer
