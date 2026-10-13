package com.pdf.alldocument.office.pdfreaderapp.presentation.filemanager

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Parses an SVG `d` attribute into Compose path nodes. */
private fun nodes(d: String) = PathParser().parsePathString(d).toNodes()

/**
 * Icons transcribed 1:1 from the supplied SVGs.
 *
 * All fills/strokes are baked white so a single `Icon(tint = ...)` ColorFilter
 * recolours them; that keeps one vector usable for both selected and unselected
 * bottom-nav states.
 */
object FmIcons {

    private fun icon(
        name: String,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()

    /** <path d="M3 8 L7 11 L12 5 L17 11 L21 8 L19 18 L5 18 Z" fill="#FFA726"/> */
    val Crown: ImageVector = icon("FmCrown") {
        addPath(
            pathData = nodes("M3,8 L7,11 L12,5 L17,11 L21,8 L19,18 L5,18 Z"),
            fill = SolidColor(Color.White),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineJoin = StrokeJoin.Round
        )
    }

    /** Two stacked chevrons — the centre FAB glyph. */
    val ChevronUpDouble: ImageVector = icon("FmChevronUpDouble") {
        listOf(
            "M6,11.5 L12,5.5 L18,11.5",
            "M6,17.5 L12,11.5 L18,17.5"
        ).forEach { chevron ->
            addPath(
                pathData = nodes(chevron),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }

    /** <path d="M4 11 L12 4 L20 11 L20 20 L14 20 L14 15 L10 15 L10 20 L4 20 Z"/> */
    val Home: ImageVector = icon("FmHome") {
        addPath(
            pathData = nodes("M4,11 L12,4 L20,11 L20,20 L14,20 L14,15 L10,15 L10,20 L4,20 Z"),
            fill = SolidColor(Color.White)
        )
    }

    /** Clock: circle r=8 built from two arcs, plus the L-shaped hand. */
    val Recent: ImageVector = icon("FmRecent") {
        addPath(
            pathData = nodes("M12,4 A8,8 0 1,0 12,20 A8,8 0 1,0 12,4"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f
        )
        addPath(
            pathData = nodes("M12,8 L12,12 L15,14"),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }

    /** <path d="M12 3 L14.7 8.6 L21 9.3 L16.5 13.4 L17.8 19.7 L12 16.5 L6.2 19.7 L7.5 13.4 L3 9.3 L9.3 8.6 Z"/> */
    val Favourite: ImageVector = icon("FmFavourite") {
        addPath(
            pathData = nodes(
                "M12,3 L14.7,8.6 L21,9.3 L16.5,13.4 L17.8,19.7 L12,16.5 " +
                    "L6.2,19.7 L7.5,13.4 L3,9.3 L9.3,8.6 Z"
            ),
            fill = SolidColor(Color.White)
        )
    }

    /** Four stroked rounded squares. */
    val Tools: ImageVector = icon("FmTools") {
        listOf(
            "M5,5 L9,5 L9,9 L5,9 Z",
            "M15,5 L19,5 L19,9 L15,9 Z",
            "M5,15 L9,15 L9,19 L5,19 Z",
            "M15,15 L19,15 L19,19 L15,19 Z"
        ).forEach { rect ->
            addPath(
                pathData = nodes(rect),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }

    /**
     * Page-with-folded-corner outline. Used as the glyph inside the filter
     * tabs, where the file type is carried by tint rather than by shape.
     */
    val DocGlyph: ImageVector = icon("FmDocGlyph") {
        listOf(
            "M6,3 L14,3 L18,7 L18,21 L6,21 Z",
            "M14,3 L14,7 L18,7"
        ).forEach { path ->
            addPath(
                pathData = nodes(path),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }
}
