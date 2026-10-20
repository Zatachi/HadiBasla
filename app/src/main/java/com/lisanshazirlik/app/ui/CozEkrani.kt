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
    /** Eğitim paketinde: sonuç ekranında "Devam et" gösterilir. */
    bitti: ((dogru: Int, toplam: Int) -> Unit)? = null,
) {
    var liste by remember { mutableStateOf(sorular) }
    var sira by remember { mutableIntStateOf(0) }
    var secim by remember { mutableStateOf<Int?>(null) }
    var dogruSayi by remember { mutableIntStateOf(0) }
    var bitti by remember { mutableStateOf(false) }
    kayit.degisim

    if (liste.isEmpty()) {
        Scaffold(topBar = { UstCubuk(baslik, geri) }, containerColor = Zemin) { ic ->
            Text("Burada çözülecek soru kalmadı.", modifier = Modifier.padding(ic).padding(24.dp))
        }
        return
    }

    val soru = liste[sira]
    val sonMu = sira == liste.lastIndex

    Scaffold(
        topBar = {
            UstCubuk(baslik, geri) {
                if (!bitti) Text("${sira + 1}/${liste.size}", color = Krem, modifier = Modifier.padding(end = 16.dp))
            }
        },
        bottomBar = {
            if (!bitti) {
                Column(Modifier.background(Kart).navigationBarsPadding()) {
                    HorizontalDivider(color = Cizgi)
                    Button(
                        onClick = {
                            if (sonMu) bitti = true else { sira++; secim = null }
                        },
                        enabled = secim != null,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    ) {
                        Text(
                            when {
                                secim == null -> "Bir şık seç"
                                sonMu -> "Sonucu gör"
                                else -> "Sonraki soru"
                            }
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
                devam = if (bitti != null) ({ bitti(dogruSayi, liste.size) }) else null,
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val dersAdi = banka.ders(soru.ders)?.kisaAd ?: soru.ders
                val konuAdi = banka.ders(soru.ders)?.konular?.firstOrNull { it.id == soru.konu }?.ad
                SoruMetni(soru, listOfNotNull(dersAdi, konuAdi).joinToString(" · "))
                Bosluk(6)
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

@Composable
private fun Sonuc(dogru: Int, toplam: Int, modifier: Modifier, tekrar: () -> Unit, geri: () -> Unit, devam: (() -> Unit)? = null) {
    val oran = yuzde(dogru, toplam)
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Etiket("Sonuç")
        Bosluk(8)
        Text("%$oran", fontFamily = Baslik, fontSize = 56.sp, color = if (oran >= 60) DogruYesil else YanlisKirmizi)
        Bosluk(4)
        Text("$toplam sorudan $dogru doğru", style = MaterialTheme.typography.titleMedium)
        Bosluk(8)
        Text(
            when {
                oran >= 80 -> "Çok iyi gidiyorsun. Bu konuyu büyük ölçüde oturtmuşsun."
                oran >= 60 -> "Fena değil. Yanlışlarını tekrar çözerek pekiştirebilirsin."
                else -> "Bu konuya biraz daha çalışmak iyi olur. Yanlışların \"Yanlışlarım\" bölümünde seni bekliyor."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MetinSoluk,
            textAlign = TextAlign.Center,
        )
        Bosluk(28)
        if (devam != null) {
            Button(onClick = devam, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Devam et") }
            Bosluk(10)
            OutlinedButton(onClick = tekrar, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Tekrar çöz") }
        } else {
            Button(onClick = tekrar, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Tekrar çöz") }
            Bosluk(10)
            OutlinedButton(onClick = geri, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Geri dön") }
        }
    }
}
