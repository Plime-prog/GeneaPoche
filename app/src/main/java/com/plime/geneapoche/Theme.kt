package com.plime.geneapoche

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Couleurs : blanc, verts pale et fonce, rouge doux pour "a verifier".
val CouleurVert = Color(0xFF2E7D32)
val CouleurVertFonce = Color(0xFF1F4F26)
val CouleurRouge = Color(0xFFB45046)
val CouleurVertPale = Color(0xFFE7F0E5)
val CouleurNeutrePale = Color(0xFFEFF4EE)

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
