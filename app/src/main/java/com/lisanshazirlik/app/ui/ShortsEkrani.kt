package com.lisanshazirlik.app.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lisanshazirlik.app.data.BilgiKarti
import com.lisanshazirlik.app.data.Kisa
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import kotlinx.coroutines.delay

sealed interface AkisOgesi {
    val anahtar: String
    data class Video(val kisa: Kisa) : AkisOgesi { override val anahtar = "v" + kisa.id }
    data class SoruKarti(val soru: Soru) : AkisOgesi { override val anahtar = "s" + soru.id }
    data class Bilgi(val konuAd: String, val kart: BilgiKarti, val no: Int) : AkisOgesi { override val anahtar = "b$konuAd$no" }
}

/**
 * Akış: her kısa videonun ardından aynı konudan bir bilgi kartı ve bir soru; sonra lisansın
 * diğer soruları ve kartları karışık. Video yoksa sadece soru ve kartlarla çalışır.
 */
fun akisOlustur(banka: SoruBankasi, depo: KullaniciDeposu, dersler: Collection<String>): List<AkisOgesi> {
    val liste = mutableListOf<AkisOgesi>()
    // Kaldığın konunun videoları önce gelsin.
    val siradakiKonu = dersler.firstNotNullOfOrNull { com.lisanshazirlik.app.data.Paket.siradaki(banka, depo, it)?.konu?.id }
    val kullanilanSoru = mutableSetOf<String>()
    val kullanilanKart = mutableSetOf<String>()
    banka.kisalar.filter { it.ders in dersler }.sortedByDescending { it.konu == siradakiKonu }.forEachIndexed { i, k ->
        liste += AkisOgesi.Video(k)
        val konu = banka.konu(k.konu)?.second
        konu?.kartlar?.getOrNull(i % maxOf(1, konu.kartlar.size))?.let { kart ->
            liste += AkisOgesi.Bilgi(konu.ad, kart, konu.kartlar.indexOf(kart)); kullanilanKart += konu.ad + konu.kartlar.indexOf(kart)
        }
        banka.konuSorulari(k.konu).firstOrNull { it.id !in kullanilanSoru }?.let {
            liste += AkisOgesi.SoruKarti(it); kullanilanSoru += it.id
        }
    }
    val kartlar = banka.dersler.filter { it.kod in dersler }.flatMap { d ->
        d.konular.flatMap { k -> k.kartlar.mapIndexed { i, kart -> AkisOgesi.Bilgi(k.ad, kart, i) } }
    }.filter { (it.konuAd + it.no) !in kullanilanKart }.shuffled()
    val sorular = banka.sorular.filter { it.ders in dersler && it.id !in kullanilanSoru }.shuffled()
        .map { AkisOgesi.SoruKarti(it) }
    // İki soruda bir bilgi kartı
    var ki = 0
    sorular.forEachIndexed { i, s ->
        liste += s
        if (i % 2 == 1 && ki < kartlar.size) liste += kartlar[ki++]
    }
    while (ki < kartlar.size) liste += kartlar[ki++]
    return liste.take(60)
}

@Composable
fun ShortsEkrani(
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    cevapla: (Soru, Boolean) -> Unit,
    konuAc: (String) -> Unit,
    geri: () -> Unit,
) {
    val odak = com.lisanshazirlik.app.data.Odak.ders(banka, depo)?.kod
    val akis = remember(odak) { akisOlustur(banka, depo, listOfNotNull(odak)) }
    val pager = rememberPagerState { akis.size }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F1F18))) {
        if (akis.isEmpty()) {
            Text(
                "Bu lisans için kısa içerikler hazırlanıyor.",
                color = Krem,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            VerticalPager(pager, modifier = Modifier.fillMaxSize(), key = { akis[it].anahtar }) { i ->
                val aktif = pager.settledPage == i
                when (val o = akis[i]) {
                    is AkisOgesi.Video -> KisaVideo(o.kisa, banka.ders(o.kisa.ders)?.kisaAd.orEmpty(), aktif) { konuAc(o.kisa.konu) }
                    is AkisOgesi.SoruKarti -> AkisSorusu(o.soru, banka.ders(o.soru.ders)?.kisaAd.orEmpty(), cevapla)
                    is AkisOgesi.Bilgi -> AkisKarti(o)
                }
            }
        }
        // Üst çubuk
        Column(Modifier.fillMaxWidth().background(Color(0x66000000)).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "✕",
                color = Krem,
                fontSize = 22.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = geri).padding(10.dp),
            )
            Text("Shorts", color = Krem, fontFamily = Baslik, fontSize = 22.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
            if (akis.isNotEmpty()) Text("${pager.currentPage + 1}/${akis.size}", color = KremSoluk, fontSize = 13.sp)
        }
        OdakSecici(banka, depo, odak, koyu = true)
        Bosluk(8)
        }
    }
}

@Composable
private fun KisaVideo(k: Kisa, dersAd: String, aktif: Boolean, konuyaGit: () -> Unit) {
    var oynatici by remember { mutableStateOf<VideoView?>(null) }
    var hazir by remember { mutableStateOf(false) }
    var durdu by remember { mutableStateOf(false) }
    var ilerleme by remember { mutableStateOf(0f) }
    DisposableEffect(Unit) { onDispose { oynatici?.stopPlayback() } }

    LaunchedEffect(aktif, hazir) {
        val v = oynatici ?: return@LaunchedEffect
        if (!hazir) return@LaunchedEffect
        if (aktif) {
            v.seekTo((k.bas * 1000).toInt()); v.start(); durdu = false
            while (true) {
                delay(200)
                val p = v.currentPosition
                ilerleme = ((p - k.bas * 1000) / ((k.son - k.bas) * 1000)).toFloat().coerceIn(0f, 1f)
                if (p >= k.son * 1000) v.seekTo((k.bas * 1000).toInt()) // başa dön (döngü)
            }
        } else {
            v.pause()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize().clickable {
                oynatici?.let { if (it.isPlaying) { it.pause(); durdu = true } else { it.start(); durdu = false } }
            },
            factory = { c ->
                VideoView(c).apply {
                    setOnPreparedListener { it.setVolume(1f, 1f); hazir = true }
                    setVideoURI(Uri.parse(k.video))
                    oynatici = this
                }
            },
        )
        if (!hazir) CircularProgressIndicator(color = Altin, modifier = Modifier.align(Alignment.Center))
        if (durdu) Text("▶", color = Krem, fontSize = 64.sp, modifier = Modifier.align(Alignment.Center))
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
        ) {
            Text("🎬 $dersAd", color = Altin, fontSize = 13.sp)
            Text(k.baslik, color = Krem, style = MaterialTheme.typography.titleMedium)
            Bosluk(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(3.dp).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(ilerleme).height(3.dp).background(Altin))
                }
                Text(
                    "Konuya git ›",
                    color = Krem,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp).clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF))
                        .clickable(onClick = konuyaGit).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Bosluk(4)
            Text("Yukarı kaydır: sıradaki ↑", color = KremSoluk, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AkisSorusu(soru: Soru, dersAd: String, cevapla: (Soru, Boolean) -> Unit) {
    var secim by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Zemin)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 104.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (soru.senaryo) "💼 VAKA · $dersAd" else "✏️ HIZLI SORU · $dersAd",
            style = MaterialTheme.typography.labelMedium,
            color = if (soru.senaryo) Color(0xFF7A2E3A) else Yesil,
        )
        Text(soru.metin, style = MaterialTheme.typography.titleMedium)
        soru.secenekler.forEachIndexed { i, m ->
            val durum = when {
                secim == null -> SikDurumu.NORMAL
                i == soru.dogru -> SikDurumu.DOGRU
                i == secim -> SikDurumu.YANLIS
                else -> SikDurumu.SOLUK
            }
            Sik(HARFLER[i], m, durum, if (secim == null) ({ secim = i; cevapla(soru, i == soru.dogru) }) else null)
        }
        if (secim != null) {
            Aciklama(soru, secim)
            Text("Devam için yukarı kaydır ↑", color = MetinSoluk, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun AkisKarti(o: AkisOgesi.Bilgi) {
    var arka by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Yesil).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🃏 BİLGİ KARTI · ${o.konuAd}", color = Altin, fontSize = 12.sp, textAlign = TextAlign.Center)
            Bosluk(16)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (arka) Krem else YesilKoyu)
                    .border(2.dp, Altin, RoundedCornerShape(24.dp))
                    .clickable { arka = !arka }
                    .padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (arka) o.kart.arka else o.kart.on,
                    color = if (arka) YesilKoyu else Krem,
                    fontFamily = Baslik,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Bosluk(16)
            Text(if (arka) "Yukarı kaydır: sıradaki ↑" else "Cevabı görmek için karta dokun", color = KremSoluk, fontSize = 13.sp)
        }
    }
}
