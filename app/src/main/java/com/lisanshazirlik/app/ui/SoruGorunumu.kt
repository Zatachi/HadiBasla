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
import com.lisanshazirlik.app.data.GeriBildirim
import com.lisanshazirlik.app.data.Soru

enum class SikDurumu { NORMAL, SECILI, DOGRU, YANLIS, SOLUK }

@Composable
fun SoruMetni(soru: Soru, ust: String) {
    Etiket(ust)
    Bosluk(8)
    Text(soru.metin, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun Sik(harf: String, metin: String, durum: SikDurumu, tikla: (() -> Unit)?) {
    val (zemin, kenar, rozet, rozetYazi) = when (durum) {
        SikDurumu.NORMAL -> Dortlu(Kart, Cizgi, Zemin, Metin)
        SikDurumu.SECILI -> Dortlu(YesilAcik, Yesil, Yesil, Krem)
        SikDurumu.DOGRU -> Dortlu(DogruZemin, DogruYesil, DogruYesil, Color.White)
        SikDurumu.YANLIS -> Dortlu(YanlisZemin, YanlisKirmizi, YanlisKirmizi, Color.White)
        SikDurumu.SOLUK -> Dortlu(Kart, Cizgi, Zemin, MetinSoluk)
    }
    val sekil = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(sekil)
            .background(zemin)
            .border(if (durum == SikDurumu.NORMAL || durum == SikDurumu.SOLUK) 1.dp else 2.dp, kenar, sekil)
            .then(if (tikla != null) Modifier.clickable(onClick = tikla) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(rozet), contentAlignment = Alignment.Center) {
            Text(harf, style = MaterialTheme.typography.labelLarge, color = rozetYazi)
        }
        Text(
            metin,
            style = MaterialTheme.typography.bodyMedium,
            color = if (durum == SikDurumu.SOLUK) MetinSoluk else Metin,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private data class Dortlu(val a: Color, val b: Color, val c: Color, val d: Color)

@Composable
fun Aciklama(soru: Soru, verilen: Int?) {
    val dogruMu = verilen == soru.dogru
    KartKutu {
        Text(
            when {
                verilen == null -> "Boş Bıraktın"
                dogruMu -> "Doğru Cevap"
                else -> "Yanlış Cevap"
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (dogruMu) DogruYesil else YanlisKirmizi,
        )
        Bosluk(4)
        Text(
            "Doğru Şık: ${HARFLER[soru.dogru]}) ${soru.secenekler[soru.dogru]}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (soru.aciklama.isNotBlank()) {
            Bosluk(10)
            Text(soru.aciklama, style = MaterialTheme.typography.bodyMedium)
        }
        if (soru.kaynak.isNotBlank()) {
            Bosluk(10)
            Text("Dayanak: ${soru.kaynak}", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
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
