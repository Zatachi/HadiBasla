package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import com.lisanshazirlik.app.data.GeriBildirim
import com.lisanshazirlik.app.data.Soru

enum class SikDurumu { NORMAL, SECILI, DOGRU, YANLIS, SOLUK }

/** Soru kartı: koyu yeşil zemin, altın etiket, krem metin (Profil kimlik kartıyla aynı dil). */
@Composable
fun SoruMetni(soru: Soru, ust: String, @Suppress("UNUSED_PARAMETER") renk: Color = Yesil) {
    val sekil = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(sekil)
            .background(YesilKoyu)
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ust.uppercase(java.util.Locale.forLanguageTag("tr")),
                style = MaterialTheme.typography.labelMedium,
                color = Altin,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (soru.senaryo) {
                Text(
                    "  ·  VAKA",
                    style = MaterialTheme.typography.labelMedium,
                    color = KremSoluk,
                )
            }
        }
        Bosluk(8)
        Box(Modifier.width(28.dp).height(2.dp).background(Altin))
        Bosluk(12)
        Text(soru.metin, style = MaterialTheme.typography.bodyLarge, fontSize = 17.sp, lineHeight = 25.sp, color = Krem)
    }
}

/** Şık: ince çerçeveli sade satır, harf yuvarlağı başlık yazı tipinde. Cevaptan sonra sağda ✓ / ✕. */
@Composable
fun Sik(harf: String, metin: String, durum: SikDurumu, tikla: (() -> Unit)?) {
    val (zemin, kenar, rozet, rozetYazi) = when (durum) {
        SikDurumu.NORMAL -> Dortlu(Kart, Cizgi, Zemin, Yesil)
        SikDurumu.SECILI -> Dortlu(YesilAcik, Yesil, Yesil, Krem)
        SikDurumu.DOGRU -> Dortlu(DogruZemin, DogruYesil, DogruYesil, Color.White)
        SikDurumu.YANLIS -> Dortlu(YanlisZemin, YanlisKirmizi, YanlisKirmizi, Color.White)
        SikDurumu.SOLUK -> Dortlu(Kart, Cizgi, Zemin, MetinSoluk)
    }
    val zeminA by animateColorAsState(zemin, tween(220), label = "zemin")
    val kenarA by animateColorAsState(kenar, tween(220), label = "kenar")
    val rozetA by animateColorAsState(rozet, tween(220), label = "rozet")
    val sekil = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(sekil)
            .background(zeminA)
            .border(if (durum == SikDurumu.NORMAL || durum == SikDurumu.SOLUK) 1.dp else 1.5.dp, kenarA, sekil)
            .then(if (tikla != null) Modifier.clickable(onClick = tikla) else Modifier)
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(rozetA)
                .then(if (durum == SikDurumu.NORMAL || durum == SikDurumu.SOLUK) Modifier.border(1.dp, Cizgi, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(harf, fontFamily = Baslik, fontSize = 15.sp, color = rozetYazi)
        }
        Text(
            metin,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = if (durum == SikDurumu.DOGRU) FontWeight.SemiBold else FontWeight.Normal,
            color = if (durum == SikDurumu.SOLUK) MetinSoluk else Metin,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        when (durum) {
            SikDurumu.DOGRU -> IsaretRozeti("✓", DogruYesil)
            SikDurumu.YANLIS -> IsaretRozeti("✕", YanlisKirmizi)
            else -> {}
        }
    }
}

@Composable
private fun IsaretRozeti(simge: String, renk: Color) {
    Box(Modifier.size(22.dp).clip(CircleShape).background(renk), contentAlignment = Alignment.Center) {
        Text(simge, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private data class Dortlu(val a: Color, val b: Color, val c: Color, val d: Color)

/** Cevap sonrası açıklama: renkli başlık şeridi, doğru şık, açıklama ve dayanak. */
@Composable
fun Aciklama(soru: Soru, verilen: Int?) {
    val dogruMu = verilen == soru.dogru
    val renk = when {
        verilen == null -> Color(0xFF7A5A12)
        dogruMu -> DogruYesil
        else -> YanlisKirmizi
    }
    val sekil = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(sekil).background(Kart).border(1.dp, renk.copy(alpha = 0.35f), sekil)) {
        Row(
            Modifier.fillMaxWidth().background(renk).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when {
                    verilen == null -> "○  Boş Bıraktın"
                    dogruMu -> "✓  Doğru Cevap"
                    else -> "✕  Yanlış Cevap"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (!dogruMu) {
                Text(
                    "Doğrusu ${HARFLER[soru.dogru]}",
                    style = MaterialTheme.typography.labelLarge,
                    color = renk,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(DogruYesil), contentAlignment = Alignment.Center) {
                    Text(HARFLER[soru.dogru], color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    soru.secenekler[soru.dogru],
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            if (soru.aciklama.isNotBlank()) {
                Bosluk(14)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Zemin).padding(12.dp),
                ) {
                    Text("💡", fontSize = 16.sp)
                    Text(soru.aciklama, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (soru.kaynak.isNotBlank()) {
                Bosluk(10)
                Text(
                    "📘 Dayanak: ${soru.kaynak}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetinSoluk,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(YesilAcik.copy(alpha = 0.6f)).padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** Her sorunun altındaki "İşine Yaradı Mı?" ve "Hata Bildir" bölümü. */
@Composable
fun SoruGeriBildirimi(soru: Soru, mevcutOy: Int, oyVer: (Int) -> Unit) {
    var hataAcik by remember { mutableStateOf(false) }
    var hataGonderildi by remember(soru.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bu Soru İşine Yaradı Mı?", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OySecenegi("👍 Evet", mevcutOy == 1) { oyVer(if (mevcutOy == 1) 0 else 1) }
            OySecenegi("👎 Hayır", mevcutOy == -1) { oyVer(if (mevcutOy == -1) 0 else -1) }
            Box(Modifier.weight(1f))
            TextButton(onClick = { hataAcik = true }, enabled = !hataGonderildi) {
                Text(if (hataGonderildi) "Bildirildi" else "Hata Bildir", color = if (hataGonderildi) MetinSoluk else YanlisKirmizi)
            }
        }
    }
    if (hataAcik) {
        HataPenceresi(
            kapat = { hataAcik = false },
            gonder = { metin ->
                GeriBildirim.hata(soru.id, metin)
                hataGonderildi = true
                hataAcik = false
            },
        )
    }
}

@Composable
private fun OySecenegi(yazi: String, secili: Boolean, tikla: () -> Unit) {
    val sekil = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .clip(sekil)
            .background(if (secili) Yesil else Kart)
            .border(1.dp, if (secili) Yesil else Cizgi, sekil)
            .clickable(onClick = tikla)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(yazi, style = MaterialTheme.typography.labelLarge, color = if (secili) Krem else Metin)
    }
}

@Composable
private fun HataPenceresi(kapat: () -> Unit, gonder: (String) -> Unit) {
    var metin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = kapat,
        title = { Text("Hata Bildir") },
        text = {
            Column {
                Text("Neyin yanlış olduğunu kısaca yaz. Örneğin: \"Doğru cevap B olmalı\" ya da \"Bu madde değişti\".")
                Bosluk(12)
                OutlinedTextField(
                    value = metin,
                    onValueChange = { metin = it.take(500) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Açıklaman") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { gonder(metin.trim()) }, enabled = metin.isNotBlank()) { Text("Gönder") }
        },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç") } },
    )
}
