package com.lisanshazirlik.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Yesil = Color(0xFF1D4D3A)
val YesilKoyu = Color(0xFF133528)
val YesilAcik = Color(0xFFDCE9E1)
val Krem = Color(0xFFF3E7CC)
val KremSoluk = Color(0xFFCBBF9F)
val Altin = Color(0xFFD9A441)
val Zemin = Color(0xFFF7F3EA)
val Kart = Color(0xFFFFFFFF)
val Metin = Color(0xFF1F2A24)
val MetinSoluk = Color(0xFF5E6B63)
val Cizgi = Color(0xFFE4DCC9)
val DogruYesil = Color(0xFF2E7D4F)
val DogruZemin = Color(0xFFE3F2E8)
val YanlisKirmizi = Color(0xFFB3402F)
val YanlisZemin = Color(0xFFF8E3DF)

val Baslik = FontFamily.Serif

private val renkler = lightColorScheme(
    primary = Yesil,
    onPrimary = Krem,
    primaryContainer = YesilAcik,
    onPrimaryContainer = YesilKoyu,
    secondary = Altin,
    onSecondary = Color(0xFF2B2006),
    background = Zemin,
    onBackground = Metin,
    surface = Kart,
    onSurface = Metin,
    surfaceVariant = Color(0xFFEDE5D3),
    onSurfaceVariant = MetinSoluk,
    outline = Cizgi,
    error = YanlisKirmizi,
)

private val yazilar = Typography(
    headlineSmall = TextStyle(fontFamily = Baslik, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Baslik, fontWeight = FontWeight.Normal, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp),
)

@Composable
fun LisanslaTema(icerik: @Composable () -> Unit) {
    MaterialTheme(colorScheme = renkler, typography = yazilar, content = icerik)
}
