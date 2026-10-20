package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Paket
import com.lisanshazirlik.app.data.SoruBankasi

/** Bir dersin eğitim paketi: adımların listesi ve "devam et" düğmesi. */
@Composable
fun PaketEkrani(
    dersKod: String,
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    adimAc: (Paket.Adim) -> Unit,
    geri: () -> Unit,
    denemelerAc: (() -> Unit)? = null,
) {
    val ders = banka.ders(dersKod) ?: return
    val adimlar = Paket.adimlar(banka, dersKod)
    val bitti = Paket.tamamlananlar(depo, dersKod)
    val siradaki = adimlar.firstOrNull { it.id !in bitti }
    val tamam = adimlar.count { it.id in bitti }
    val hazirlaniyor = ders.konular.filter { k -> adimlar.none { it.konu?.id == k.id } }
    var sifirlaSor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { UstCubuk(ders.kisaAd, geri) },
        bottomBar = {
            Column(Modifier.background(Kart).navigationBarsPadding()) {
                HorizontalDivider(color = Cizgi)
                if (siradaki != null) {
                    Button(onClick = { adimAc(siradaki) }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp)) {
                        Text(if (tamam == 0) "▶  Başla: ${siradaki.tur.ad}" else "▶  Devam et: ${siradaki.tur.ad}")
                    }
                } else {
                    OutlinedButton(onClick = { sifirlaSor = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp)) {
                        Text("Dersi baştan başlat")
                    }
                }
            }
        },
        containerColor = Zemin,
    ) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Yesil).padding(18.dp)) {
                Text("DERS ${ders.kod}", style = MaterialTheme.typography.labelMedium, color = Altin)
                Bosluk(4)
                Text(ders.ad, fontFamily = Baslik, fontSize = 22.sp, lineHeight = 28.sp, color = Krem)
                Bosluk(12)
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(YesilKoyu)) {
                    Box(
                        Modifier
                            .fillMaxWidth(if (adimlar.isEmpty()) 0f else (tamam / adimlar.size.toFloat()).coerceAtLeast(0.02f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Altin),
                    )
                }
                Bosluk(8)
                Text(
                    if (siradaki == null && adimlar.isNotEmpty()) "Paketi bitirdin 🎉" else if (adimlar.isEmpty()) "Anlatımlar hazırlanıyor" else "${adimlar.size} adımdan $tamam tamamlandı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KremSoluk,
                )
            }
            ders.konular.forEachIndexed { i, kn ->
                val konuAdimlari = adimlar.filter { it.konu?.id == kn.id }
                BolumKarti(i + 1, kn.ad, konuAdimlari, bitti, siradaki, adimAc)
            }
            adimlar.firstOrNull { it.tur == Paket.Tur.DENEME }?.let { d ->
                BolumKarti(0, "Final · Ders denemesi", listOf(d), bitti, siradaki, adimAc)
            }
            if (denemelerAc != null) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(YesilAcik).clickableIf(true, denemelerAc).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⏱", fontSize = 22.sp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Bu dersin denemeleri", style = MaterialTheme.typography.titleMedium)
                        Text("10 deneme · gerçek sınav gibi 25 soru, 45 dk", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    }
                    Text("›", fontSize = 24.sp, color = Yesil)
                }
            }
        }
    }
    if (sifirlaSor) {
        AlertDialog(
            onDismissRequest = { sifirlaSor = false },
            title = { Text("Paketi baştan başlat") },
            text = { Text("Paket adımlarının işaretleri sıfırlanır. Çözdüğün sorular, puanların ve rozetlerin silinmez.") },
            confirmButton = { TextButton(onClick = { depo.paketSifirla(dersKod); sifirlaSor = false }) { Text("Baştan başlat") } },
            dismissButton = { TextButton(onClick = { sifirlaSor = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun AdimSatiri(a: Paket.Adim, bitti: Boolean, siradaki: Boolean, tikla: () -> Unit) {
    val sekil = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(sekil)
            .background(if (siradaki) YesilAcik else Kart)
            .border(if (siradaki) 2.dp else 1.dp, if (siradaki) Yesil else Cizgi, sekil)
            .clickableIf(true, tikla)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(if (bitti) DogruYesil else if (siradaki) Yesil else Zemin),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (bitti) "✓" else a.tur.simge, color = if (bitti || siradaki) Krem else Metin, fontSize = 17.sp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(a.tur.ad, style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    bitti -> "Tamamlandı"
                    siradaki -> "Sıradaki adım"
                    else -> if (a.tur == Paket.Tur.DENEME) "Gerçek sınav formatında" else a.baslik
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (bitti) DogruYesil else MetinSoluk,
            )
        }
        Text("›", fontSize = 24.sp, color = MetinSoluk)
    }
}

/** Bir adım bitince: tebrik ve sıradaki adım. */
@Composable
fun PaketGecisEkrani(
    dersKod: String,
    bitenId: String,
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    sonuc: String?,
    adimAc: (Paket.Adim) -> Unit,
    pakete: () -> Unit,
) {
    val biten = Paket.adim(banka, dersKod, bitenId)
    val siradaki = Paket.siradaki(banka, depo, dersKod)
    val adimlar = Paket.adimlar(banka, dersKod)
    val tamam = Paket.tamamlananlar(depo, dersKod).size.coerceAtMost(adimlar.size)

    Column(
        Modifier.fillMaxSize().background(Zemin).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(DogruYesil), contentAlignment = Alignment.Center) {
            Text("✓", color = Krem, fontSize = 40.sp)
        }
        Bosluk(16)
        Text("${biten?.tur?.ad ?: "Adım"} tamamlandı", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        biten?.konu?.let { Text(it.ad, style = MaterialTheme.typography.bodyLarge, color = MetinSoluk, textAlign = TextAlign.Center) }
        sonuc?.let { Bosluk(6); Text(it, style = MaterialTheme.typography.titleMedium, color = Yesil, textAlign = TextAlign.Center) }
        Bosluk(8)
        Text("$tamam / ${adimlar.size} adım", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
        Bosluk(28)
        if (siradaki != null) {
            KartKutu {
                Etiket("Sıradaki")
                Bosluk(4)
                Text("${siradaki.tur.simge} ${siradaki.tur.ad}", style = MaterialTheme.typography.titleMedium)
                Text(siradaki.baslik, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            }
            Bosluk(16)
            Button(onClick = { adimAc(siradaki) }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Devam et") }
            Bosluk(8)
            OutlinedButton(onClick = pakete, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Pakete dön") }
        } else {
            Text("Paketi bitirdin 🎉", style = MaterialTheme.typography.titleLarge, color = DogruYesil)
            Bosluk(16)
            Button(onClick = pakete, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Pakete dön") }
        }
    }
}

private fun Paket.Tur.kisa(): String = when (this) {
    Paket.Tur.VIDEO -> "Video"
    Paket.Tur.OZET -> "Özet"
    Paket.Tur.KARTLAR -> "Kartlar"
    Paket.Tur.SORU -> "Sorular"
    Paket.Tur.DENEME -> "Deneme"
}

/** Bir konu = bir bölüm. Adımlar küçük kutucuklar halinde; sıradaki adım vurgulanır. */
@Composable
private fun BolumKarti(
    no: Int,
    ad: String,
    adimlar: List<Paket.Adim>,
    bitti: Set<String>,
    siradaki: Paket.Adim?,
    adimAc: (Paket.Adim) -> Unit,
) {
    val tamam = adimlar.isNotEmpty() && adimlar.all { it.id in bitti }
    val aktif = siradaki != null && adimlar.any { it.id == siradaki.id }
    val sekil = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(sekil)
            .background(Kart)
            .border(if (aktif) 2.dp else 1.dp, if (aktif) Yesil else Cizgi, sekil)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(
                    when {
                        tamam -> DogruYesil
                        aktif -> Yesil
                        else -> Zemin
                    }
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (tamam) "✓" else if (no == 0) "🏁" else "$no",
                    color = if (tamam || aktif) Krem else MetinSoluk,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                if (no > 0) Text("BÖLÜM $no", style = MaterialTheme.typography.labelMedium, color = MetinSoluk)
                Text(ad, style = MaterialTheme.typography.titleMedium, color = if (adimlar.isEmpty()) MetinSoluk else Metin)
            }
            when {
                tamam -> Text("Tamam", style = MaterialTheme.typography.labelMedium, color = DogruYesil)
                aktif -> Text("Şimdi", style = MaterialTheme.typography.labelMedium, color = Yesil)
                adimlar.isEmpty() -> Text("🔜 Yakında", style = MaterialTheme.typography.labelMedium, color = MetinSoluk)
            }
        }
        if (adimlar.isNotEmpty() && !(adimlar.size == 1 && adimlar[0].tur == Paket.Tur.DENEME && !aktif && !tamam)) {
            Bosluk(12)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                adimlar.forEach { a ->
                    val b = a.id in bitti
                    val s = a.id == siradaki?.id
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    s -> Yesil
                                    b -> YesilAcik
                                    else -> Zemin
                                }
                            )
                            .clickableIf(true) { adimAc(a) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (b) "✓" else a.tur.simge, fontSize = 18.sp, color = if (s) Krem else DogruYesil)
                        Text(a.tur.kisa(), style = MaterialTheme.typography.labelMedium, fontSize = 11.sp, color = if (s) Krem else Metin, maxLines = 1)
                    }
                }
            }
        }
    }
}
