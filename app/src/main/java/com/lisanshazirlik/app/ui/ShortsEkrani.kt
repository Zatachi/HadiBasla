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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.lisanshazirlik.app.data.Kisa
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Odak
import com.lisanshazirlik.app.data.SoruBankasi
import kotlinx.coroutines.delay

private val KOYU_ZEMIN = Color(0xFF0F1F18)

/** Dizi bölümleri sırayla, sonra diğer kısa videolar. */
fun shortsSirasi(banka: SoruBankasi, ders: String?): List<Kisa> =
    banka.kisalar.filter { it.ders == ders }
        .sortedWith(compareBy<Kisa>({ it.dizi.isEmpty() }, { it.dizi }, { it.bolum }))

/** Shorts: sadece video. Kaydır, izle; isteyen sondaki tek soruyu cevaplar. */
@Composable
fun ShortsEkrani(banka: SoruBankasi, depo: KullaniciDeposu, konuAc: (String) -> Unit, geri: () -> Unit) {
    val odak = Odak.ders(banka, depo)?.kod
    var kayitliMod by remember { mutableStateOf(false) }
    val k = depo.kullanici
    val tum = remember(odak, banka) { shortsSirasi(banka, odak) }
    val liste = if (kayitliMod) tum.filter { it.id in k?.kaydedilenler.orEmpty() } else tum
    // İzlenmemiş ilk bölümden başla.
    val baslangic = remember(odak, kayitliMod) { liste.indexOfFirst { it.id !in k?.izlenenShort.orEmpty() }.coerceAtLeast(0) }
    val pager = rememberPagerState(initialPage = baslangic) { liste.size }
    val bugun = depo.bugunShort()

    Box(Modifier.fillMaxSize().background(KOYU_ZEMIN)) {
        if (liste.isEmpty()) {
            Text(
                if (kayitliMod) "Henüz Kaydettiğin Video Yok. Beğendiğin Videoda 🔖'ye Dokun."
                else "Bu Dersin Kısa Videoları Hazırlanıyor. Üstten Başka Bir Ders Seçebilirsin.",
                color = Krem,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            VerticalPager(pager, modifier = Modifier.fillMaxSize(), key = { liste[it].id }) { i ->
                val kisa = liste[i]
                KisaVideo(
                    kisa = kisa,
                    dersAd = banka.ders(kisa.ders)?.kisaAd.orEmpty(),
                    aktif = pager.settledPage == i,
                    begenildi = kisa.id in k?.begeniler.orEmpty(),
                    kaydedildi = kisa.id in k?.kaydedilenler.orEmpty(),
                    begen = { depo.begen(kisa.id, it) },
                    kaydet = { depo.kaydet(kisa.id, it) },
                    izlendi = { depo.shortIzlendi(kisa.id) },
                    konuyaGit = { konuAc(kisa.konu) },
                    sonMu = i == liste.lastIndex,
                )
            }
        }

        // Üst çubuk: kapat, başlık, günlük halka, kaydedilenler; altında ders çipleri
        Column(Modifier.fillMaxWidth().background(Color(0x66000000)).windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✕", color = Krem, fontSize = 22.sp, modifier = Modifier.clip(CircleShape).clickable(onClick = geri).padding(10.dp))
                Text(if (kayitliMod) "Kaydettiklerim" else "Shorts", color = Krem, fontFamily = Baslik, fontSize = 22.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
                // Günlük hedef halkası
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                    CircularProgressIndicator(
                        progress = { (bugun / KullaniciDeposu.GUNLUK_SHORT.toFloat()).coerceAtMost(1f) },
                        color = Altin,
                        trackColor = Color(0x33FFFFFF),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(38.dp),
                    )
                    Text("$bugun", color = Krem, fontSize = 12.sp)
                }
                Text(
                    "🔖",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 8.dp).clip(CircleShape)
                        .background(if (kayitliMod) Altin else Color(0x22FFFFFF))
                        .clickable { kayitliMod = !kayitliMod }.padding(8.dp),
                )
            }
            if (!kayitliMod) OdakSecici(banka, depo, odak, koyu = true)
            Bosluk(8)
        }
    }
}

@Composable
private fun KisaVideo(
    kisa: Kisa,
    dersAd: String,
    aktif: Boolean,
    begenildi: Boolean,
    kaydedildi: Boolean,
    begen: (Boolean) -> Unit,
    kaydet: (Boolean) -> Unit,
    izlendi: () -> Unit,
    konuyaGit: () -> Unit,
    sonMu: Boolean,
) {
    var oynatici by remember { mutableStateOf<VideoView?>(null) }
    var hazir by remember { mutableStateOf(false) }
    var durdu by remember { mutableStateOf(false) }
    var bitti by remember { mutableStateOf(false) }
    var sayildi by remember { mutableStateOf(false) }
    var ilerleme by remember { mutableStateOf(0f) }
    var cevap by remember { mutableStateOf<Int?>(null) }
    var tekrarSayac by remember { mutableStateOf(0) }
    DisposableEffect(Unit) { onDispose { oynatici?.stopPlayback() } }

    LaunchedEffect(aktif, hazir, tekrarSayac) {
        val v = oynatici ?: return@LaunchedEffect
        if (!hazir) return@LaunchedEffect
        if (!aktif) { v.pause(); return@LaunchedEffect }
        v.seekTo((kisa.bas * 1000).toInt()); v.start(); durdu = false; bitti = false
        while (true) {
            delay(200)
            val p = v.currentPosition
            ilerleme = ((p - kisa.bas * 1000) / ((kisa.son - kisa.bas) * 1000)).toFloat().coerceIn(0f, 1f)
            if (!sayildi && ilerleme >= 0.7f) { sayildi = true; izlendi() }
            if (p >= kisa.son * 1000) { v.pause(); bitti = true; ilerleme = 1f; break }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize().clickable {
                if (bitti) return@clickable
                oynatici?.let { if (it.isPlaying) { it.pause(); durdu = true } else { it.start(); durdu = false } }
            },
            factory = { c ->
                VideoView(c).apply {
                    setOnPreparedListener { hazir = true }
                    setVideoURI(Uri.parse(kisa.video))
                    oynatici = this
                }
            },
        )
        if (!hazir) CircularProgressIndicator(color = Altin, modifier = Modifier.align(Alignment.Center))
        if (durdu && !bitti) Text("▶", color = Krem, fontSize = 64.sp, modifier = Modifier.align(Alignment.Center))

        // Sağ taraf: beğen, kaydet, konuya git
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 10.dp, top = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            YanDugme(if (begenildi) "❤️" else "🤍", "Beğen") { begen(!begenildi) }
            YanDugme(if (kaydedildi) "🔖" else "📑", if (kaydedildi) "Kayıtlı" else "Kaydet") { kaydet(!kaydedildi) }
            YanDugme("📖", "Konu", konuyaGit)
        }

        // Alt bilgi: dizi, başlık, ilerleme
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0x99000000))
                .windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp),
        ) {
            Text(
                if (kisa.dizi.isNotBlank()) "🎬 ${kisa.dizi} · Bölüm ${kisa.bolum}/${kisa.toplam}" else "🎬 $dersAd",
                color = Altin, fontSize = 13.sp,
            )
            Text(kisa.baslik, color = Krem, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 56.dp))
            Bosluk(8)
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x55FFFFFF))) {
                Box(Modifier.fillMaxWidth(ilerleme).height(3.dp).background(Altin))
            }
        }

        // Bitiş: mini soru (isteğe bağlı) + merak cümlesi
        if (bitti) {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)).background(Color(0xEE10241B)).border(1.dp, Altin, RoundedCornerShape(22.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val m = kisa.miniSoru
                if (m != null) {
                    Text("Hızlı Soru", color = Altin, fontSize = 12.sp)
                    Text(m.metin, color = Krem, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        m.secenekler.forEachIndexed { i, s ->
                            val renk = when {
                                cevap == null -> Color(0x33FFFFFF)
                                i == m.dogru -> DogruYesil
                                i == cevap -> YanlisKirmizi
                                else -> Color(0x22FFFFFF)
                            }
                            Text(
                                s, color = Krem, textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(renk)
                                    .clickable(enabled = cevap == null) { cevap = i }.padding(vertical = 14.dp, horizontal = 6.dp),
                            )
                        }
                    }
                    cevap?.let { Text(if (it == m.dogru) "✓ Doğru!" else "✗ Doğrusu: ${m.secenekler[m.dogru]}", color = if (it == m.dogru) DogruYesil else YanlisKirmizi) }
                }
                if (kisa.merak.isNotBlank()) {
                    Text("Sıradaki Bölümde: ${kisa.merak}", color = KremSoluk, textAlign = TextAlign.Center, fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "⟲ Tekrar İzle", color = Krem,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF))
                            .clickable { cevap = null; tekrarSayac++ }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                Text(if (sonMu) "Bu Dersin Son Videosu" else "↑ Sıradaki İçin Yukarı Kaydır", color = KremSoluk, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun YanDugme(simge: String, yazi: String, tikla: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = tikla)) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0x55000000)), contentAlignment = Alignment.Center) {
            Text(simge, fontSize = 22.sp)
        }
        Text(yazi, color = Krem, fontSize = 11.sp)
    }
}
