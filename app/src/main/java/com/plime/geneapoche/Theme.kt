package com.plime.geneapoche

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Couleurs : blanc, verts pale et fonce, rouge doux pour "a verifier".
val CouleurVert = Color(0xFF2E7D32)
val CouleurVertFonce = Color(0xFF1F4F26)
val CouleurRouge = Color(0xFFB45046)
val CouleurVertPale = Color(0xFFE7F0E5)
val CouleurNeutrePale = Color(0xFFEFF4EE)

// G3-06 : couleurs metier centralisees (seule source de Color(...) en dehors
// des palettes claire et sombre ci-dessus).
val CouleurRose = Color(0xFFE9C4D2)
val CouleurBleu = Color(0xFFBFD4E8)
val CouleurNeutreDoux = Color(0xFFD9E4D6)
val CouleurRosePale = Color(0xFFF8DDE7)
val CouleurBleuPale = Color(0xFFDDE9F6)
val CouleurBordVert = Color(0xFF9CBF9A)
val CouleurOrange = Color(0xFFE8A33D)
val CouleurSurvol = Color(0xFFE3F1DF)
val CouleurInactif = Color(0xFFB9C2AE)
val CouleurCarte = Color(0xFFF6FAF5)
val CouleurGrisBord = Color(0xFFE0E0E0)
val GrisManquant = Color(0xFF9AA099)
val CouleurLiaison = Color(0xFF4C7A50)

val PaletteGeneaPoche = lightColorScheme(
    primary = Color(0xFF2E5D3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8EBD4),
    onPrimaryContainer = Color(0xFF10240F),
    secondary = Color(0xFF4E6B52),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1EDE0),
    onSecondaryContainer = Color(0xFF152418),
    tertiary = Color(0xFF3A6B4A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE8F0E7),
    onSurfaceVariant = Color(0xFF44503F),
    outline = Color(0xFF798577),
    // Conteneurs thirdaires et d'erreur : verts/rouge doux, AUCUN violet Material3 par defaut.
    outlineVariant = Color(0xFFC9D2C2),
    tertiaryContainer = Color(0xFFD8EBD4),
    onTertiaryContainer = Color(0xFF10240F),
    error = Color(0xFFB45046),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF6DDD8),
    onErrorContainer = Color(0xFF3A1110),
    surfaceTint = Color(0xFF2E5D3A),
    // Surfaces "container" : blancs verdatres (les defauts Material3 sont VIOLETS :
    // NavigationBar du bas, dialogues, cartes). Tout est remplace ici.
    surfaceDim = Color(0xFFDCE2D5),
    surfaceBright = Color(0xFFFDFEF8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFCF7),
    surfaceContainer = Color(0xFFF4F8F1),
    surfaceContainerHigh = Color(0xFFF7FAF4),
    surfaceContainerHighest = Color(0xFFEFF3E9)
)

// G3-02 : palette SOMBRE deduite de la meme palette verte ; contrastes
// verifies (texte clair E2E5DC sur fond 11150E > 12:1).
val PaletteGeneaPocheSombre = darkColorScheme(
    primary = Color(0xFF9CD2A0),
    onPrimary = Color(0xFF0E2412),
    primaryContainer = Color(0xFF24452B),
    onPrimaryContainer = Color(0xFFD8EBD4),
    secondary = Color(0xFFB3CCB6),
    onSecondary = Color(0xFF1D2B20),
    secondaryContainer = Color(0xFF334738),
    onSecondaryContainer = Color(0xFFE1EDE0),
    tertiary = Color(0xFFA5D4AF),
    onTertiary = Color(0xFF0F2A17),
    tertiaryContainer = Color(0xFF275032),
    onTertiaryContainer = Color(0xFFD8EBD4),
    background = Color(0xFF11150E),
    onBackground = Color(0xFFE2E5DC),
    surface = Color(0xFF11150E),
    onSurface = Color(0xFFE2E5DC),
    surfaceVariant = Color(0xFF252B22),
    onSurfaceVariant = Color(0xFFC0CABC),
    outline = Color(0xFF8B988A),
    outlineVariant = Color(0xFF4A5445),
    error = Color(0xFFF2B8B2),
    onError = Color(0xFF3A1110),
    errorContainer = Color(0xFF5C1F1B),
    onErrorContainer = Color(0xFFF6DDD8),
    surfaceTint = Color(0xFF9CD2A0),
    surfaceDim = Color(0xFF0C0F0A),
    surfaceBright = Color(0xFF31372D),
    surfaceContainerLowest = Color(0xFF0C0F0A),
    surfaceContainerLow = Color(0xFF191E15),
    surfaceContainer = Color(0xFF1D2219),
    surfaceContainerHigh = Color(0xFF272C22),
    surfaceContainerHighest = Color(0xFF31372D)
)

val FormesGeneaPoche = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// G3-07 : typographie Material 3 complete ; serif pour les noms et les
// titres, sans-serif (police du systeme) pour le texte. Aucun telechargement :
// l'application n'a pas Internet ; des polices OFL embarquees dans res/font
// pourront remplacer FontFamily.Serif quand l'auteur fournira les .ttf.
val TypographieGeneaPoche = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)
)
