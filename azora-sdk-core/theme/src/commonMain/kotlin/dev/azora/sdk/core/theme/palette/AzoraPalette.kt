@file:UseSerializers(ColorSerializer::class)

package dev.azora.sdk.core.theme.palette

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.*

/**
 * The complete Azora color palette.
 *
 * Contains all base colors used throughout the Azora design system, organized into:
 * - **Base colors**: White, Black, Transparent
 * - **Neutral scale**: Grayscale from Neutral10 (lightest) to Neutral95 (darkest)
 * - **Primary scale**: Brand primary colors from Primary10 to Primary95
 * - **Secondary scale**: Brand secondary colors from Secondary10 to Secondary95
 * - **Accent colors**: Vibrant accent colors for highlights and emphasis
 * - **Pastel colors**: Softer, muted versions of accent colors
 *
 * Use these colors to build custom [Palette] instances or reference directly for specific UI needs.
 */
@Serializable
object AzoraPalette {

    // region Base Colors
    /** Pure white (#FFFFFF). */
    val White = Color(0xFFFFFFFF)
    /** Pure black (#000000). */
    val Black = Color(0xFF000000)
    /** Fully transparent. */
    val Transparent = Color(0x00000000)
    // endregion

    // region Neutral Scale
    /** Neutral 10 - Lightest neutral (#FBFBFB). */
    val Neutral10 = Color(0xFFFBFBFB)
    /** Neutral 15 (#F9F9F9). */
    val Neutral15 = Color(0xFFF9F9F9)
    /** Neutral 20 (#ECECEC). */
    val Neutral20 = Color(0xFFECECEC)
    /** Neutral 25 (#E2E2E2). */
    val Neutral25 = Color(0xFFE2E2E2)
    /** Neutral 30 (#D9D9D9). */
    val Neutral30 = Color(0xFFD9D9D9)
    /** Neutral 35 (#C4C4C4). */
    val Neutral35 = Color(0xFFC4C4C4)
    /** Neutral 40 (#B2B3B3). */
    val Neutral40 = Color(0xFFB2B3B3)
    /** Neutral 45 (#A6A6A6). */
    val Neutral45 = Color(0xFFA6A6A6)
    /** Neutral 50 (#9B9B9B). */
    val Neutral50 = Color(0xFF9B9B9B)
    /** Neutral base - Mid-tone neutral (#818181). */
    val Neutral = Color(0xFF818181)
    /** Neutral 60 (#676767). */
    val Neutral60 = Color(0xFF676767)
    /** Neutral 65 (#4C4C4C). */
    val Neutral65 = Color(0xFF4C4C4C)
    /** Neutral 70 (#313131). */
    val Neutral70 = Color(0xFF313131)
    /** Neutral 75 (#2C2C2C). */
    val Neutral75 = Color(0xFF2C2C2C)
    /** Neutral 80 (#262626). */
    val Neutral80 = Color(0xFF262626)
    /** Neutral 85 (#202020). */
    val Neutral85 = Color(0xFF202020)
    /** Neutral 90 (#1A1A1A). */
    val Neutral90 = Color(0xFF1A1A1A)
    /** Neutral 95 - Darkest neutral (#141414). */
    val Neutral95 = Color(0xFF141414)
    // endregion

    // region Primary Scale
    /** Primary 10 - Lightest primary (#FBF5FE). */
    val Primary10 = Color(0xFFFBF5FE)
    /** Primary 15 (#F8EBFD). */
    val Primary15 = Color(0xFFF8EBFD)
    /** Primary 20 (#F1D6FB). */
    val Primary20 = Color(0xFFF1D6FB)
    /** Primary 25 (#EAC2F9). */
    val Primary25 = Color(0xFFEAC2F9)
    /** Primary 30 (#E3ADF7). */
    val Primary30 = Color(0xFFE3ADF7)
    /** Primary 35 (#DC99F5). */
    val Primary35 = Color(0xFFDC99F5)
    /** Primary 40 (#D584F3). */
    val Primary40 = Color(0xFFD584F3)
    /** Primary 45 (#CE6FF1). */
    val Primary45 = Color(0xFFCE6FF1)
    /** Primary 50 (#C75BEE). */
    val Primary50 = Color(0xFFC75BEE)
    /** Primary base - Brand primary color (#D14EEA). */
    val Primary = Color(0xFFD14EEA)
    /** Primary 60 (#BB3ED8). */
    val Primary60 = Color(0xFFBB3ED8)
    /** Primary 65 (#A536C6). */
    val Primary65 = Color(0xFFA536C6)
    /** Primary 70 (#8F2EB4). */
    val Primary70 = Color(0xFF8F2EB4)
    /** Primary 75 (#792896). */
    val Primary75 = Color(0xFF792896)
    /** Primary 80 (#632278). */
    val Primary80 = Color(0xFF632278)
    /** Primary 85 (#4D1C5A). */
    val Primary85 = Color(0xFF4D1C5A)
    /** Primary 90 (#37163C). */
    val Primary90 = Color(0xFF37163C)
    /** Primary 95 - Darkest primary (#21101E). */
    val Primary95 = Color(0xFF21101E)
    // endregion

    // region Secondary Scale
    /** Secondary 10 - Lightest secondary (#F5F9FE). */
    val Secondary10 = Color(0xFFF5F9FE)
    /** Secondary 15 (#EBF3FD). */
    val Secondary15 = Color(0xFFEBF3FD)
    /** Secondary 20 (#D6E7FB). */
    val Secondary20 = Color(0xFFD6E7FB)
    /** Secondary 25 (#C2DBF9). */
    val Secondary25 = Color(0xFFC2DBF9)
    /** Secondary 30 (#ADCFF7). */
    val Secondary30 = Color(0xFFADCFF7)
    /** Secondary 35 (#99C3F5). */
    val Secondary35 = Color(0xFF99C3F5)
    /** Secondary 40 (#84B7F3). */
    val Secondary40 = Color(0xFF84B7F3)
    /** Secondary 45 (#6FAAF1). */
    val Secondary45 = Color(0xFF6FAAF1)
    /** Secondary 50 (#5B9EEE). */
    val Secondary50 = Color(0xFF5B9EEE)
    /** Secondary base - Brand secondary color (#4E93EA). */
    val Secondary = Color(0xFF4E93EA)
    /** Secondary 60 (#3E82D8). */
    val Secondary60 = Color(0xFF3E82D8)
    /** Secondary 65 (#3671C6). */
    val Secondary65 = Color(0xFF3671C6)
    /** Secondary 70 (#2E60B4). */
    val Secondary70 = Color(0xFF2E60B4)
    /** Secondary 75 (#284F96). */
    val Secondary75 = Color(0xFF284F96)
    /** Secondary 80 (#223E78). */
    val Secondary80 = Color(0xFF223E78)
    /** Secondary 85 (#1C2D5A). */
    val Secondary85 = Color(0xFF1C2D5A)
    /** Secondary 90 (#161C3C). */
    val Secondary90 = Color(0xFF161C3C)
    /** Secondary 95 - Darkest secondary (#100B1E). */
    val Secondary95 = Color(0xFF100B1E)
    // endregion

    // region Accent Colors
    /** Accent blue for highlights and links (#0D8BD9). */
    val AccentBlue = Color(0xFF0D8BD9)
    /** Accent orange for warnings and attention (#FF8C00). */
    val AccentOrange = Color(0xFFFF8C00)
    /** Accent green for success states (#4EC962). */
    val AccentGreen = Color(0xFF4EC962)
    /** Accent red for errors and destructive actions (#E63946). */
    val AccentRed = Color(0xFFE63946)
    /** Accent yellow for caution and highlights (#FFC107). */
    val AccentYellow = Color(0xFFFFC107)
    /** Accent purple for special features (#9C27B0). */
    val AccentPurple = Color(0xFF9C27B0)
    /** Accent teal for secondary actions (#009688). */
    val AccentTeal = Color(0xFF009688)
    /** Accent pink for highlights (#E91E63). */
    val AccentPink = Color(0xFFE91E63)
    /** Accent cyan for informational elements (#00BCD4). */
    val AccentCyan = Color(0xFF00BCD4)
    // endregion

    // region Pastel Colors
    /** Pastel blue - Softer blue for backgrounds (#5BA3D0). */
    val PastelBlue = Color(0xFF5BA3D0)
    /** Pastel orange - Softer orange (#D4A574). */
    val PastelOrange = Color(0xFFD4A574)
    /** Pastel green - Softer green (#7DBF8A). */
    val PastelGreen = Color(0xFF7DBF8A)
    /** Pastel red - Softer red (#D17179). */
    val PastelRed = Color(0xFFD17179)
    /** Pastel yellow - Softer yellow (#E6C96B). */
    val PastelYellow = Color(0xFFE6C96B)
    /** Pastel purple - Softer purple (#B06FA8). */
    val PastelPurple = Color(0xFFB06FA8)
    /** Pastel teal - Softer teal (#5FA89F). */
    val PastelTeal = Color(0xFF5FA89F)
    /** Pastel pink - Softer pink (#D16B8E). */
    val PastelPink = Color(0xFFD16B8E)
    /** Pastel cyan - Softer cyan (#66BAC7). */
    val PastelCyan = Color(0xFF66BAC7)
    // endregion
}