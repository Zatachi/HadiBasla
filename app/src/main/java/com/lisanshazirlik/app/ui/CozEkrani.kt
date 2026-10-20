package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.GeriBildirim
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.YerelKayit

/** Pratik modu: her sorudan hemen sonra doğru cevap ve açıklama gösterilir. */
@Composable
fun CozEkrani(
    baslik: String,
    sorular: List<Soru>,
    mod: String,
    banka: SoruBankasi,
    kayit: YerelKayit,
    cevapla: (Soru, Boolean) -> Unit,
    geri: () -> Unit,
    /** Eğitim paketinde: sonuç ekranında "Devam Et" gösterilir. */
    adimBitti: ((dogru: Int, toplam: Int) -> Unit)? = null,
) {
    var liste by remember { mutableStateOf(sorular) }
    var sira by remember { mutableIntStateOf(0) }
    var secim by remember { mutableStateOf<Int?>(null) }
    var dogruSayi by remember { mutableIntStateOf(0) }
    var bitti by remember { mutableStateOf(false) }
    val sonuclar = remember(liste) { androidx.compose.runtime.mutableStateListOf<Boolean?>().apply { repeat(liste.size) { add(null) } } }
    kayit.degisim

    if (liste.isEmpty()) {
        Scaffold(topBar = { UstCubuk(baslik, geri) }, containerColor = Zemin) { ic ->
            Text("Burada Çözülecek Soru Kalmadı.", modifier = Modifier.padding(ic).padding(24.dp))
        }
        return
    }

    val soru = liste[sira]
    val sonMu = sira == liste.lastIndex

    Scaffold(
        topBar = {
            Column {
                UstCubuk(baslik, geri) {
                    if (!bitti) Text("${sira + 1}/${liste.size}", color = Krem, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 16.dp))
                }
                if (!bitti) IlerlemeSeridi(sonuclar, sira)
            }
        },
        bottomBar = {
            if (!bitti && secim != null) {
                Column(Modifier.background(Kart).navigationBarsPadding()) {
                    HorizontalDivider(color = Cizgi)
                    Button(
                        onClick = {
                            if (sonMu) bitti = true else { sira++; secim = null }
                        },
                        enabled = secim != null,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (secim == soru.dogru) DogruYesil else Yesil,
                            disabledContainerColor = Cizgi,
                            disabledContentColor = MetinSoluk,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(52.dp),
                    ) {
                        Text(
                            when {
                                secim == null -> "Bir Şık Seç"
                                sonMu -> "Sonucu Gör  →"
                                else -> "Sonraki Soru  →"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        },
        containerColor = Zemin,
    ) { ic ->
        if (bitti) {
            Sonuc(
                dogru = dogruSayi,
                toplam = liste.size,
                modifier = Modifier.padding(ic),
                tekrar = {
                    liste = liste.shuffled(); sira = 0; secim = null; dogruSayi = 0; bitti = false
                },
                geri = geri,
                devam = adimBitti?.let { f -> { f(dogruSayi, liste.size) } },
            )
            return@Scaffold
        }
        key(sira) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(ic)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val dersAdi = banka.ders(soru.ders)?.kisaAd ?: soru.ders
                val konuAdi = banka.ders(soru.ders)?.konular?.firstOrNull { it.id == soru.konu }?.ad
                SoruMetni(soru, listOfNotNull(dersAdi, konuAdi).joinToString(" · "))
                Bosluk(4)
                soru.secenekler.forEachIndexed { i, metin ->
                    val durum = when {
                        secim == null -> SikDurumu.NORMAL
                        i == soru.dogru -> SikDurumu.DOGRU
                        i == secim -> SikDurumu.YANLIS
                        else -> SikDurumu.SOLUK
                    }
                    Sik(HARFLER[i], metin, durum, tikla = if (secim == null) ({
                        secim = i
                        val dogruMu = i == soru.dogru
                        if (dogruMu) dogruSayi++
                        sonuclar[sira] = dogruMu
                        cevapla(soru, dogruMu)
                        GeriBildirim.cozum(soru.id, i, dogruMu, mod)
                    }) else null)
                }
                if (secim != null) {
                    Bosluk(6)
                    Aciklama(soru, secim)
                    Bosluk(4)
                    SoruGeriBildirimi(soru, kayit.oy(soru.id)) { deger ->
                        kayit.oyVer(soru.id, deger)
                        GeriBildirim.oy(soru.id, deger)
                    }
                }
            }
        }
    }
}

/** Üst şerit: her soru bir parça (doğru yeşil, yanlış kırmızı, sıradaki altın), sağda doğru/yanlış sayısı ve seri. */
/** Üst şerit: ince parçalar (doğru yeşil, yanlış kırmızı, sıradaki altın), altında sade sayaç. */
@Composable
private fun IlerlemeSeridi(sonuclar: List<Boolean?>, sira: Int) {
    val dogru = sonuclar.count { it == true }
    val yanlis = sonuclar.count { it == false }
    val seri = sonuclar.take(sira + 1).reversed().dropWhile { it == null }.takeWhile { it == true }.size
    Column(Modifier.fillMaxWidth().background(Yesil).padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
        if (sonuclar.size <= 30) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                sonuclar.forEachIndexed { i, r ->
                    androidx.compose.foundation.layout.Box(
                        Modifier.weight(1f).height(4.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(
                            when {
                                r == true -> androidx.compose.ui.graphics.Color(0xFF6FCF97)
                                r == false -> androidx.compose.ui.graphics.Color(0xFFE57F6E)
                                i == sira -> Altin
                                else -> androidx.compose.ui.graphics.Color(0x2EFFFFFF)
                            }
                        )
                    )
                }
            }
        } else {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (sira + 1f) / sonuclar.size },
                color = Altin, trackColor = androidx.compose.ui.graphics.Color(0x2EFFFFFF),
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
            )
        }
        if (dogru + yanlis > 0) {
            Bosluk(6)
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$dogru Doğru · $yanlis Yanlış", color = KremSoluk, style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                if (seri >= 3) Text("🔥 $seri Üst Üste", color = Altin, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SayiCipi(metin: String, renk: androidx.compose.ui.graphics.Color) {
    Text(
        metin,
        color = renk,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(androidx.compose.ui.graphics.Color(0x26FFFFFF))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun Sonuc(dogru: Int, toplam: Int, modifier: Modifier, tekrar: () -> Unit, geri: () -> Unit, devam: (() -> Unit)? = null) {
    val oran = yuzde(dogru, toplam)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.fillMaxWidth().background(Yesil).padding(top = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when {
                    oran >= 80 -> "🏆 Harika İş"
                    oran >= 60 -> "👏 İyi Gidiyorsun"
                    else -> "💪 Biraz Daha Çalışalım"
                },
                color = Altin, style = MaterialTheme.typography.titleMedium,
            )
            Bosluk(14)
            SkorHalkasi(oran, 150.dp, 12.dp, androidx.compose.ui.graphics.Color(0x33FFFFFF), Krem)
            Bosluk(6)
            Text("Başarı Oranı", color = KremSoluk, style = MaterialTheme.typography.bodyMedium)
        }
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SonucKutusu(Modifier.weight(1f), "✓", "$dogru", "Doğru", DogruYesil, DogruZemin)
                SonucKutusu(Modifier.weight(1f), "✕", "${toplam - dogru}", "Yanlış", YanlisKirmizi, YanlisZemin)
                SonucKutusu(Modifier.weight(1f), "∑", "$toplam", "Soru", Yesil, YesilAcik)
            }
            Bosluk(16)
            Text(
                when {
                    oran >= 80 -> "Bu Konuyu Büyük Ölçüde Oturtmuşsun."
                    oran >= 60 -> "Fena Değil. Yanlışlarını Tekrar Çözerek Pekiştirebilirsin."
                    else -> "Yanlışların \"Yanlışlarım\" Bölümünde Seni Bekliyor."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
                textAlign = TextAlign.Center,
            )
            Bosluk(24)
            val sekil = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            if (devam != null) {
                Button(onClick = devam, shape = sekil, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Devam Et  →", style = MaterialTheme.typography.titleMedium) }
                Bosluk(10)
                OutlinedButton(onClick = tekrar, shape = sekil, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Tekrar Çöz") }
            } else {
                Button(onClick = tekrar, shape = sekil, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Tekrar Çöz", style = MaterialTheme.typography.titleMedium) }
                Bosluk(10)
                OutlinedButton(onClick = geri, shape = sekil, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Geri Dön") }
            }
        }
    }
}

@Composable
private fun SonucKutusu(modifier: Modifier, simge: String, sayi: String, ad: String, renk: androidx.compose.ui.graphics.Color, zemin: androidx.compose.ui.graphics.Color) {
    Column(
        modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(zemin).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(simge, color = renk, style = MaterialTheme.typography.titleMedium)
        Text(sayi, color = renk, fontFamily = Baslik, fontSize = 26.sp)
        Text(ad, color = renk, style = MaterialTheme.typography.labelMedium)
    }
}
