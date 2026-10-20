package com.lisanshazirlik.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.BilgiKarti
import com.lisanshazirlik.app.data.Ders
import com.lisanshazirlik.app.data.Konu
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.SoruBankasi

/** Bir konunun dört bölümü: özet, bilgi kartları, soru çözme ve süreli konu testi. */
@Composable
fun KonuEkrani(
    ders: Ders,
    konu: Konu,
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    ozetAc: () -> Unit,
    kartlarAc: () -> Unit,
    videoAc: () -> Unit,
    coz: () -> Unit,
    test: () -> Unit,
    geri: () -> Unit,
) {
    val sorular = banka.konuSorulari(konu.id)
    val il = depo.ilerleme
    val cozulen = sorular.count { it.id in il }
    val dogru = sorular.count { il[it.id] == 1 }
    val testAdet = minOf(10, sorular.size)

    Scaffold(topBar = { UstCubuk(konu.ad, geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(ic)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(ders.ad, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            KartKutu {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$cozulen", style = MaterialTheme.typography.headlineSmall)
                    Text(" / ${sorular.size} Soru Çözüldü", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.padding(bottom = 3.dp))
                }
                Bosluk(10)
                Ilerleme(if (sorular.isEmpty()) 0f else cozulen / sorular.size.toFloat())
                if (cozulen > 0) {
                    Bosluk(8)
                    BilgiSatiri("Doğru Oranın", "%${yuzde(dogru, cozulen)}")
                }
            }
            if (konu.video.isNotBlank()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Yesil)
                        .clickableIf(true, videoAc)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(Altin), contentAlignment = Alignment.Center) {
                        Text("▶", color = YesilKoyu, fontSize = 22.sp)
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Video Anlatım", style = MaterialTheme.typography.titleMedium, color = Krem)
                        Text("Konuyu Önce İzleyerek Öğren", style = MaterialTheme.typography.bodyMedium, color = KremSoluk)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Bolum(Modifier.weight(1f), "📖", "Konu Özeti", if (konu.ozet.isNotBlank()) "Önce Oku" else null, ozetAc)
                Bolum(Modifier.weight(1f), "🃏", "Bilgi Kartları", if (konu.kartlar.isNotEmpty()) "${konu.kartlar.size} Kart" else null, kartlarAc)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Bolum(Modifier.weight(1f), "✏️", "Soru Çöz", if (sorular.isNotEmpty()) "${sorular.size} Soru, Açıklamalı" else null, coz)
                Bolum(Modifier.weight(1f), "⏱", "Konu Testi", if (testAdet > 0) "$testAdet Soru, ${denemeSuresiDk(testAdet)} dk" else null, test)
            }
            Text(
                "Önerilen sıra: videoyu izle ya da özeti oku, kartlarla tekrar et, soruları çöz, son olarak konu testiyle kendini ölç.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
            )
        }
    }
}

@Composable
private fun Bolum(modifier: Modifier, simge: String, baslik: String, alt: String?, tikla: () -> Unit) {
    val hazir = alt != null
    val sekil = RoundedCornerShape(14.dp)
    Column(
        modifier
            .heightIn(min = 128.dp)
            .clip(sekil)
            .background(if (hazir) Kart else Zemin)
            .border(1.dp, Cizgi, sekil)
            .clickableIf(hazir, tikla)
            .padding(14.dp),
    ) {
        Text(simge, fontSize = 26.sp)
        Bosluk(8)
        Text(baslik, style = MaterialTheme.typography.titleMedium, color = if (hazir) Metin else MetinSoluk)
        Text(alt ?: "Hazırlanıyor", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
    }
}

private data class OzetSayfasi(val baslik: String, val satirlar: List<String>, val akildaKalsin: Boolean)

private fun ozetSayfalari(ozet: String): List<OzetSayfasi> {
    val sayfalar = mutableListOf<OzetSayfasi>()
    var baslik = ""
    var satirlar = mutableListOf<String>()
    fun kapat() {
        if (baslik.isNotBlank() || satirlar.isNotEmpty()) {
            sayfalar += OzetSayfasi(baslik, satirlar.toList(), baslik.lowercase().contains("akılda kalsın"))
        }
    }
    ozet.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { s ->
        if (s.startsWith("## ") || s.startsWith("# ")) {
            kapat(); baslik = s.trimStart('#', ' '); satirlar = mutableListOf()
        } else satirlar += s
    }
    kapat()
    return sayfalar
}

/** Sayı, oran ve T+2 gibi ezberlenecek ifadeleri kalın ve yeşil yapar. */
private fun vurgula(metin: String): androidx.compose.ui.text.AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        var son = 0
        Regex("""[^\s,.;:()]*\d[^\s,.;:()]*""").findAll(metin).forEach { m ->
            append(metin.substring(son, m.range.first))
            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Yesil))
            append(m.value)
            pop()
            son = m.range.last + 1
        }
        append(metin.substring(son))
    }

/** Konu özeti: her sayfada tek fikir, kaydırarak okunur; son sayfa "Akılda Kalsın". */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OzetEkrani(ders: Ders, konu: Konu, coz: (() -> Unit)?, kartlar: (() -> Unit)?, geri: () -> Unit, bitti: (() -> Unit)? = null) {
    val sayfalar = remember(konu.id) { ozetSayfalari(konu.ozet) }
    val durum = androidx.compose.foundation.pager.rememberPagerState { sayfalar.size }
    val kapsam = androidx.compose.runtime.rememberCoroutineScope()
    val son = durum.currentPage == sayfalar.lastIndex

    Scaffold(
        topBar = {
            UstCubuk(konu.ad, geri) {
                Text("${durum.currentPage + 1}/${sayfalar.size}", color = Krem, modifier = Modifier.padding(end = 16.dp))
            }
        },
        bottomBar = {
            Column(Modifier.background(Kart)) {
                HorizontalDivider(color = Cizgi)
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!son) {
                        OutlinedButton(
                            onClick = { kapsam.launch { durum.animateScrollToPage(durum.currentPage - 1) } },
                            enabled = durum.currentPage > 0,
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text("Geri") }
                        Button(
                            onClick = { kapsam.launch { durum.animateScrollToPage(durum.currentPage + 1) } },
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text("İleri") }
                    } else if (bitti != null) {
                        Button(onClick = bitti, modifier = Modifier.weight(1f).height(52.dp)) { Text("Okudum, Devam Et") }
                    } else {
                        if (kartlar != null) {
                            OutlinedButton(onClick = kartlar, modifier = Modifier.weight(1f).height(52.dp)) { Text("Bilgi Kartları") }
                        }
                        if (coz != null) {
                            Button(onClick = coz, modifier = Modifier.weight(1f).height(52.dp)) { Text("Soru Çöz") }
                        }
                        if (kartlar == null && coz == null) {
                            Button(onClick = geri, modifier = Modifier.weight(1f).height(52.dp)) { Text("Konuya Dön") }
                        }
                    }
                }
            }
        },
        containerColor = Zemin,
    ) { ic ->
        Column(Modifier.fillMaxSize().padding(ic)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                sayfalar.indices.forEach { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (i == durum.currentPage) 22.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i == durum.currentPage) Yesil else Cizgi),
                    )
                }
            }
            androidx.compose.foundation.pager.HorizontalPager(
                state = durum,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) { i ->
                val s = sayfalar[i]
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (s.akildaKalsin) AkildaKalsin(s) else OzetSayfasiGorunumu(s, if (i == 0) ders.kisaAd else null)
                }
            }
        }
    }
}

@Composable
private fun OzetSayfasiGorunumu(s: OzetSayfasi, ust: String?) {
    if (ust != null) Etiket(ust)
    Text(s.baslik, style = MaterialTheme.typography.headlineSmall, fontSize = 27.sp, lineHeight = 34.sp)
    var madde = 0
    s.satirlar.forEach { satir ->
        if (satir.startsWith("- ")) {
            madde++
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Kart)
                    .border(1.dp, Cizgi, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(15.dp)).background(YesilAcik),
                    contentAlignment = Alignment.Center,
                ) { Text("$madde", style = MaterialTheme.typography.labelLarge, color = YesilKoyu) }
                Text(
                    vurgula(satir.removePrefix("- ")),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 19.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        } else {
            Text(vurgula(satir), style = MaterialTheme.typography.bodyLarge, fontSize = 19.sp, lineHeight = 29.sp)
        }
    }
    Bosluk(4)
    Text("Sonraki Sayfa İçin Sola Kaydır", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
}

@Composable
private fun AkildaKalsin(s: OzetSayfasi) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Yesil)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("AKILDA KALSIN", style = MaterialTheme.typography.labelMedium, color = Altin)
        s.satirlar.flatMap { it.removePrefix("- ").split("·") }.map { it.trim() }.filter { it.isNotEmpty() }.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Altin))
                Text(p, color = Krem, fontFamily = Baslik, fontSize = 24.sp, lineHeight = 30.sp, modifier = Modifier.padding(start = 14.dp))
            }
        }
    }
    Text(
        "Bu özet, ilgili mevzuat esas alınarak Lisansla tarafından hazırlanmıştır. Sınavdan önce güncel resmi kaynakları da kontrol et.",
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 12.sp,
        color = MetinSoluk,
    )
}

/** Dokununca dönen bilgi kartları. "Biliyorum" desteden çıkarır, "Tekrar Et" sona atar. */
@Composable
fun KartlarEkrani(konu: Konu, geri: () -> Unit, bitti: (() -> Unit)? = null) {
    var deste by remember { mutableStateOf(konu.kartlar.shuffled()) }
    var ters by remember { mutableStateOf(false) }
    var bilinen by remember { mutableStateOf(0) }
    val donus by animateFloatAsState(if (ters) 180f else 0f, tween(350), label = "kart")

    Scaffold(
        topBar = {
            UstCubuk(konu.ad, geri) {
                if (deste.isNotEmpty()) Text("${deste.size} Kart", color = Krem, modifier = Modifier.padding(end = 16.dp))
            }
        },
        containerColor = Zemin,
    ) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (deste.isEmpty()) {
                Bosluk(40)
                Text("Desteyi Bitirdin", style = MaterialTheme.typography.headlineSmall)
                Text("${konu.kartlar.size} Kartın Hepsini Bildin.", style = MaterialTheme.typography.bodyLarge, color = MetinSoluk)
                Bosluk(12)
                if (bitti != null) {
                    Button(onClick = bitti, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Devam Et") }
                    OutlinedButton(onClick = { deste = konu.kartlar.shuffled(); bilinen = 0; ters = false }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Baştan Başla")
                    }
                } else {
                    Button(onClick = { deste = konu.kartlar.shuffled(); bilinen = 0; ters = false }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Baştan Başla")
                    }
                    OutlinedButton(onClick = geri, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Konuya Dön") }
                }
                return@Column
            }
            val kart: BilgiKarti = deste.first()
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = donus; cameraDistance = 12f * density }
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (donus <= 90f) Yesil else Kart)
                    .border(1.dp, if (donus <= 90f) Yesil else Cizgi, RoundedCornerShape(20.dp))
                    .clickable { ters = !ters }
                    .padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (donus <= 90f) {
                    Text(kart.on, color = Krem, fontFamily = Baslik, fontSize = 22.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
                } else {
                    Text(
                        kart.arka,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { rotationY = 180f },
                    )
                }
            }
            Text(if (ters) "Ne Kadar İyi Bildin?" else "Cevabı Görmek İçin Karta Dokun", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { deste = deste.drop(1) + kart; ters = false },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Tekrar Et") }
                Button(
                    onClick = { deste = deste.drop(1); bilinen++; ters = false },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Biliyorum") }
            }
        }
    }
}
