package com.lisanshazirlik.app.ui

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.Kisa
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import kotlinx.coroutines.delay
import java.util.Locale

private sealed interface Parca {
    data class Ses(val kisa: Kisa) : Parca
    data class Sesli(val soru: Soru) : Parca
}

private fun kulaklikListesi(banka: SoruBankasi, dersler: Collection<String>): List<Parca> {
    val liste = mutableListOf<Parca>()
    val kullanilan = mutableSetOf<String>()
    banka.kisalar.filter { it.ders in dersler }.forEach { k ->
        liste += Parca.Ses(k)
        banka.konuSorulari(k.konu).firstOrNull { it.id !in kullanilan }?.let { liste += Parca.Sesli(it); kullanilan += it.id }
    }
    banka.sorular.filter { it.ders in dersler && it.id !in kullanilan }.shuffled().take(8).forEach { liste += Parca.Sesli(it) }
    return liste
}

private val KOYU = Color(0xFF0F1F18)

/** Kulaklık modu: kısa videoların sesi podcast gibi çalar, aralarda sorular sesli okunur. */
@Composable
fun KulaklikEkrani(banka: SoruBankasi, depo: KullaniciDeposu, cevapla: (Soru, Boolean) -> Unit, geri: () -> Unit) {
    val ctx = LocalContext.current
    val dersler = banka.lisansDersleri(depo.kullanici?.lisans).map { it.kod }
    var liste by remember { mutableStateOf(kulaklikListesi(banka, dersler)) }
    var sira by remember { mutableIntStateOf(0) }
    var dogruSayi by remember { mutableIntStateOf(0) }
    var secim by remember { mutableStateOf<Int?>(null) }
    var duraklatildi by remember { mutableStateOf(false) }
    var ttsHazir by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val oynatici = remember { MediaPlayer() }

    val gorunum = LocalView.current
    DisposableEffect(Unit) {
        gorunum.keepScreenOn = true
        val tutucu = arrayOfNulls<TextToSpeech>(1)
        val t = TextToSpeech(ctx) { durum ->
            if (durum == TextToSpeech.SUCCESS) {
                tutucu[0]?.language = Locale.forLanguageTag("tr-TR")
                ttsHazir = true
            }
        }
        tutucu[0] = t
        tts = t
        onDispose {
            gorunum.keepScreenOn = false
            t.stop(); t.shutdown()
            runCatching { oynatici.release() }
        }
    }

    fun oku(metin: String) {
        if (ttsHazir) tts?.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "lisansla")
    }

    val parca = liste.getOrNull(sira)

    // Ses parçası: videonun ilgili bölümünü sadece ses olarak çal, bitince sıradakine geç.
    LaunchedEffect(sira, parca) {
        val p = parca as? Parca.Ses ?: run { runCatching { if (oynatici.isPlaying) oynatici.pause() }; return@LaunchedEffect }
        tts?.stop()
        duraklatildi = false
        var hazir = false
        runCatching {
            oynatici.reset()
            oynatici.setDataSource(p.kisa.video)
            oynatici.setOnPreparedListener { it.seekTo((p.kisa.bas * 1000).toInt()); it.start(); hazir = true }
            oynatici.prepareAsync()
        }.onFailure { sira++; return@LaunchedEffect }
        while (true) {
            delay(300)
            if (hazir && runCatching { oynatici.currentPosition }.getOrDefault(0) >= p.kisa.son * 1000) {
                runCatching { oynatici.pause() }
                sira++
                break
            }
        }
    }

    // Soru parçası: soruyu ve şıkları sesli oku.
    LaunchedEffect(sira, ttsHazir) {
        val p = parca as? Parca.Sesli ?: return@LaunchedEffect
        secim = null
        oku("Soru. " + p.soru.metin + ". " + p.soru.secenekler.mapIndexed { i, s -> "${HARFLER[i]}: $s" }.joinToString(". "))
    }

    // Cevaptan sonra geri bildirimi oku, bitince otomatik ilerle.
    LaunchedEffect(secim) {
        val p = parca as? Parca.Sesli ?: return@LaunchedEffect
        val s = secim ?: return@LaunchedEffect
        oku(if (s == p.soru.dogru) "Doğru!" else "Yanlış. Doğru cevap ${HARFLER[p.soru.dogru]}: ${p.soru.secenekler[p.soru.dogru]}")
        delay(1200)
        while (tts?.isSpeaking == true) delay(250)
        delay(600)
        sira++
    }

    Column(
        Modifier.fillMaxSize().background(KOYU).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✕", color = Krem, fontSize = 22.sp, modifier = Modifier.clip(CircleShape).clickable(onClick = geri).padding(10.dp))
            Text("🎧 Kulaklık modu", color = Krem, fontFamily = Baslik, fontSize = 22.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
            if (liste.isNotEmpty()) Text("${minOf(sira + 1, liste.size)}/${liste.size}", color = KremSoluk, fontSize = 13.sp)
        }

        when {
            liste.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bu lisans için sesli içerik hazırlanıyor.", color = Krem, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
            }
            parca == null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val soruSayisi = liste.count { it is Parca.Sesli }
                Text("🎉", fontSize = 56.sp)
                Text("Tur bitti", color = Krem, fontFamily = Baslik, fontSize = 28.sp)
                Text("$soruSayisi sorudan $dogruSayi doğru", color = Altin, fontSize = 18.sp)
                Bosluk(24)
                BuyukDugme("⟲ Yeni tur") { liste = kulaklikListesi(banka, dersler); sira = 0; dogruSayi = 0 }
                Bosluk(10)
                BuyukDugme("Kapat", ikincil = true, tikla = geri)
            }
            parca is Parca.Ses -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(160.dp).clip(CircleShape).background(YesilKoyu).border(3.dp, Altin, CircleShape), contentAlignment = Alignment.Center) {
                    Text("🎧", fontSize = 72.sp)
                }
                Bosluk(24)
                Text("DİNLİYORSUN", color = Altin, fontSize = 13.sp)
                Text(parca.kisa.baslik, color = Krem, fontFamily = Baslik, fontSize = 24.sp, textAlign = TextAlign.Center)
                Text(banka.ders(parca.kisa.ders)?.kisaAd.orEmpty(), color = KremSoluk, fontSize = 14.sp)
                Bosluk(32)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    YuvarlakDugme(if (duraklatildi) "▶" else "❚❚") {
                        runCatching { if (duraklatildi) oynatici.start() else oynatici.pause() }
                        duraklatildi = !duraklatildi
                    }
                    YuvarlakDugme("⏭") { runCatching { oynatici.pause() }; sira++ }
                }
            }
            parca is Parca.Sesli -> Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    if (parca.soru.senaryo) "💼 VAKA" else "🗣 SESLİ SORU",
                    color = Altin,
                    fontSize = 13.sp,
                )
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(parca.soru.metin, color = Krem, style = MaterialTheme.typography.titleMedium)
                    Bosluk(8)
                    parca.soru.secenekler.forEachIndexed { i, s ->
                        Text("${HARFLER[i]}) $s", color = KremSoluk, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    val sec = secim
                    if (sec != null) {
                        Bosluk(8)
                        Text(
                            if (sec == parca.soru.dogru) "✓ Doğru!" else "✗ Doğru cevap ${HARFLER[parca.soru.dogru]}",
                            color = if (sec == parca.soru.dogru) DogruYesil else YanlisKirmizi,
                            fontSize = 20.sp,
                        )
                    }
                }
                // Ekrana bakmadan basılabilen büyük şık düğmeleri
                val harfler = parca.soru.secenekler.indices.toList()
                harfler.chunked(3).forEach { satir ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        satir.forEach { i ->
                            val renk = when {
                                secim == null -> YesilKoyu
                                i == parca.soru.dogru -> DogruYesil
                                i == secim -> YanlisKirmizi
                                else -> Color(0xFF22352C)
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(renk)
                                    .clickable(enabled = secim == null) {
                                        secim = i
                                        val dogru = i == parca.soru.dogru
                                        if (dogru) dogruSayi++
                                        cevapla(parca.soru, dogru)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(HARFLER[i], color = Krem, fontSize = 34.sp, fontFamily = Baslik)
                            }
                        }
                        repeat(3 - satir.size) { Box(Modifier.weight(1f)) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "🔁 Tekrar oku",
                        color = Krem,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(YesilKoyu).clickable {
                            oku(parca.soru.metin + ". " + parca.soru.secenekler.mapIndexed { i, s -> "${HARFLER[i]}: $s" }.joinToString(". "))
                        }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                    Text(
                        "Geç ⏭",
                        color = KremSoluk,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { tts?.stop(); sira++ }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                if (!ttsHazir) Text("Sesli okuma için telefonunda Türkçe metin okuma motoru gerekli.", color = KremSoluk, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun YuvarlakDugme(simge: String, tikla: () -> Unit) {
    Box(
        Modifier.size(72.dp).clip(CircleShape).background(Altin).clickable(onClick = tikla),
        contentAlignment = Alignment.Center,
    ) { Text(simge, color = KOYU, fontSize = 26.sp) }
}

@Composable
private fun BuyukDugme(yazi: String, ikincil: Boolean = false, tikla: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(if (ikincil) YesilKoyu else Altin).clickable(onClick = tikla),
        contentAlignment = Alignment.Center,
    ) { Text(yazi, color = if (ikincil) Krem else KOYU, fontSize = 17.sp) }
}
