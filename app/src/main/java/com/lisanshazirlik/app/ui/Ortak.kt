package com.lisanshazirlik.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UstCubuk(baslik: String, geri: () -> Unit, sag: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(baslik, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = geri) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
        },
        actions = { sag() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Yesil,
            titleContentColor = Krem,
            navigationIconContentColor = Krem,
            actionIconContentColor = Krem,
        ),
    )
}

@Composable
fun KartKutu(
    modifier: Modifier = Modifier,
    tikla: (() -> Unit)? = null,
    icerik: @Composable () -> Unit,
) {
    val sekil = RoundedCornerShape(14.dp)
    val renk = CardDefaults.cardColors(containerColor = Kart)
    val kenar = BorderStroke(1.dp, Cizgi)
    if (tikla != null) {
        Card(onClick = tikla, modifier = modifier.fillMaxWidth(), shape = sekil, colors = renk, border = kenar) {
            Column(Modifier.padding(16.dp)) { icerik() }
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = sekil, colors = renk, border = kenar) {
            Column(Modifier.padding(16.dp)) { icerik() }
        }
    }
}

@Composable
fun Ilerleme(oran: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { oran.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = Yesil,
        trackColor = Cizgi,
        drawStopIndicator = {},
    )
}

@Composable
fun Etiket(metin: String, modifier: Modifier = Modifier) {
    Text(
        metin.uppercase(java.util.Locale.forLanguageTag("tr")),
        style = MaterialTheme.typography.labelMedium,
        color = MetinSoluk,
        modifier = modifier,
    )
}

@Composable
fun BilgiSatiri(sol: String, sag: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(sol, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
        Text(sag, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Bosluk(y: Int) = Spacer(Modifier.height(y.dp))

val HARFLER = listOf("A", "B", "C", "D", "E")

fun yuzde(pay: Int, payda: Int): Int = if (payda == 0) 0 else Math.round(pay * 100f / payda)

private val TR = java.util.Locale.forLanguageTag("tr")
private val UZUN_TARIH = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", TR)
private val KISA_TARIH = java.time.format.DateTimeFormatter.ofPattern("d MMMM", TR)

fun uzunTarih(t: java.time.LocalDate): String = t.format(UZUN_TARIH)
fun kisaTarih(t: java.time.LocalDate): String = t.format(KISA_TARIH)

fun kalanGunYazisi(gun: Long): String = when {
    gun > 1 -> "$gun Gün Kaldı"
    gun == 1L -> "Yarın"
    gun == 0L -> "Bugün"
    else -> "Sınav Tarihi Geçti"
}

fun basHarf(ad: String): String = ad.trim().firstOrNull()?.uppercase(TR) ?: "?"
