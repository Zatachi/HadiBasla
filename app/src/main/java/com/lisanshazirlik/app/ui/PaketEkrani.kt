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
) {
    val ders = banka.ders(dersKod) ?: return
    val adimlar = Paket.adimlar(banka, dersKod)
    val bitti = Paket.tamamlananlar(depo, dersKod)
    val siradaki = adimlar.firstOrNull { it.id !in bitti }
    val tamam = adimlar.count { it.id in bitti }
    val hazirlaniyor = ders.konular.filter { k -> adimlar.none { it.konu?.id == k.id } }
    var sifirlaSor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { UstCubuk("Eğitim paketi", geri) },
        bottomBar = {
            Column(Modifier.background(Kart).navigationBarsPadding()) {
                HorizontalDivider(color = Cizgi)
                if (siradaki != null) {
                    Button(onClick = { adimAc(siradaki) }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp)) {
                        Text(if (tamam == 0) "Pakete başla" else "Devam et: ${siradaki.tur.ad}")
                    }
                } else {
                    OutlinedButton(onClick = { sifirlaSor = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp)) {
                        Text("Paketi baştan başlat")
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
                Text("📦 EĞİTİM PAKETİ", style = MaterialTheme.typography.labelMedium, color = Altin)
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
                    if (siradaki == null && adimlar.isNotEmpty()) "Paketi bitirdin 🎉" else "${adimlar.size} adımdan $tamam tamamlandı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KremSoluk,
                )
            }
            Text(
                "Her konu için önce video ya da özetle öğren, bilgi kartlarıyla tekrar et, sonra soruları çöz. Paket ders denemesiyle biter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
            )

            var sonKonu: String? = null
            adimlar.forEach { a ->
                val konuAdi = a.konu?.ad ?: "Final"
                if (konuAdi != sonKonu) {
                    sonKonu = konuAdi
                    Etiket(konuAdi, Modifier.padding(start = 4.dp, top = 8.dp))
                }
                AdimSatiri(a, a.id in bitti, a == siradaki) { adimAc(a) }
            }
            if (hazirlaniyor.isNotEmpty()) {
                Etiket("Hazırlanıyor", Modifier.padding(start = 4.dp, top = 8.dp))
                Text(
                    hazirlaniyor.joinToString(" · ") { it.ad } + ". İçerikleri eklendikçe pakete otomatik olarak katılır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinSoluk,
                )
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
