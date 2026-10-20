package com.lisanshazirlik.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ListenerRegistration
import com.lisanshazirlik.app.data.CanliDuello
import com.lisanshazirlik.app.data.CanliMac
import com.lisanshazirlik.app.data.Duello
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Odak
import com.lisanshazirlik.app.data.OyuncuDurumu
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

private sealed interface Asama {
    data object Araniyor : Asama
    data class Canli(val macId: String) : Asama
    data class Bot(val sorular: List<Soru>) : Asama
    data class Sonuc(val benim: OyuncuDurumu, val rakip: OyuncuDurumu?, val rakipAd: String, val bot: Boolean) : Asama
}

const val BOT_AD = "🤖 Antrenman rakibi"

/** Tek tuşla rakip bul: aynı derse çalışan biriyle eşleşir; 20 sn'de kimse yoksa bot gelir. */
@Composable
fun CanliDuelloEkrani(banka: SoruBankasi, depo: KullaniciDeposu, cevapla: (Soru, Boolean) -> Unit, geri: () -> Unit) {
    val uid = depo.uid
    val odak = Odak.ders(banka, depo)
    val ders = odak?.kod
    val ad = CanliDuello.anonimAd(depo.kullanici, uid)
    var asama by remember { mutableStateOf<Asama>(Asama.Araniyor) }
    var gecen by remember { mutableIntStateOf(0) }

    if (ders == null || banka.dersSorulari(ders).isEmpty()) {
        Scaffold(topBar = { UstCubuk("Canlı düello", geri) }, containerColor = Zemin) { ic ->
            Text("Bu dersin soruları henüz hazır değil.", modifier = Modifier.padding(ic).padding(24.dp))
        }
        return
    }

    when (val a = asama) {
        Asama.Araniyor -> {
            var dinleyici by remember { mutableStateOf<ListenerRegistration?>(null) }
            DisposableEffect(Unit) {
                onDispose { dinleyici?.remove() }
            }
            LaunchedEffect(Unit) {
                val sorular = Duello.sorulariSec(banka, listOf(ders)).map { it.id }
                // 1) Bekleyen biri varsa hemen kap.
                runCatching { CanliDuello.kapmayiDene(uid, ad, ders, depo.kullanici?.lisans, sorular) }.getOrNull()?.let {
                    asama = Asama.Canli(it); return@LaunchedEffect
                }
                // 2) Yoksa bekle; kapılırsan dinleyici haber verir. Arada tekrar kapmayı dene.
                val eslesme = CompletableDeferred<String>()
                runCatching { CanliDuello.bekle(uid, ad, ders, depo.kullanici?.lisans) }
                dinleyici = CanliDuello.beklemeyiDinle(uid) { eslesme.complete(it) }
                val bulunan = withTimeoutOrNull(CanliDuello.BEKLEME_SN * 1000L) {
                    while (!eslesme.isCompleted) {
                        delay(1000)
                        gecen++
                        if (gecen % 4 == 0) {
                            runCatching { CanliDuello.kapmayiDene(uid, ad, ders, depo.kullanici?.lisans, sorular) }.getOrNull()
                                ?.let { eslesme.complete(it) }
                        }
                    }
                    eslesme.await()
                }
                dinleyici?.remove()
                val mac = bulunan ?: CanliDuello.birak(uid)
                asama = if (mac != null) Asama.Canli(mac) else Asama.Bot(Duello.sorulariSec(banka, listOf(ders)))
            }
            AramaEkrani(odak?.kisaAd.orEmpty(), gecen) {
                dinleyici?.remove()
                birakArkaPlanda(uid)
                geri()
            }
        }
        is Asama.Canli -> {
            var mac by remember { mutableStateOf<CanliMac?>(null) }
            DisposableEffect(a.macId) {
                val d = CanliDuello.maciDinle(a.macId) { mac = it }
                onDispose { d.remove() }
            }
            val m = mac
            if (m == null) {
                AramaEkrani(odak?.kisaAd.orEmpty(), gecen, eslesti = true) { geri() }
            } else {
                val rakipId = m.oyuncular.firstOrNull { it != uid }.orEmpty()
                val sorular = remember(m.id) { m.sorular.mapNotNull { banka.bul(it) } }
                CanliOyun(
                    sorular = sorular,
                    rakipAd = m.adlar[rakipId] ?: "Rakip",
                    rakip = m.ilerleme[rakipId] ?: OyuncuDurumu(),
                    cevapla = cevapla,
                    ilerle = { CanliDuello.ilerlemeYaz(m.id, uid, it) },
                    bitti = { ben ->
                        asama = Asama.Sonuc(ben, mac?.ilerleme?.get(rakipId), m.adlar[rakipId] ?: "Rakip", false)
                    },
                    rakipBekle = true,
                    canliRakip = { mac?.ilerleme?.get(rakipId) },
                    vazgec = geri,
                )
            }
        }
        is Asama.Bot -> {
            var bot by remember { mutableStateOf(OyuncuDurumu()) }
            LaunchedEffect(Unit) {
                var t = 0
                for (i in a.sorular.indices) {
                    val bekleme = Random.nextInt(6, 15)
                    repeat(bekleme) { delay(1000); t++; bot = bot.copy(sure = t) }
                    val dg = Random.nextFloat() < 0.65f
                    bot = bot.copy(cevaplanan = i + 1, dogru = bot.dogru + if (dg) 1 else 0, sure = t, bitti = i == a.sorular.lastIndex)
                }
            }
            CanliOyun(
                sorular = a.sorular,
                rakipAd = BOT_AD,
                rakip = bot,
                cevapla = cevapla,
                ilerle = {},
                bitti = { ben -> asama = Asama.Sonuc(ben, bot, BOT_AD, true) },
                rakipBekle = true,
                canliRakip = { bot },
                vazgec = geri,
            )
        }
        is Asama.Sonuc -> {
            LaunchedEffect(Unit) {
                val s = sonucHesapla(a.benim, a.rakip)
                val xp = when (s) { 1 -> if (a.bot) 15 else 40; 0 -> if (a.bot) 8 else 20; else -> 5 }
                depo.xpEkle(xp)
            }
            CanliSonuc(a, tekrar = { gecen = 0; asama = Asama.Araniyor }, geri = geri)
        }
    }
}

/** Ekran kapansa da bekleme kaydını silmeyi tamamlar. */
@OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
private fun birakArkaPlanda(uid: String) {
    kotlinx.coroutines.GlobalScope.launch { CanliDuello.birak(uid) }
}

private fun sonucHesapla(ben: OyuncuDurumu, o: OyuncuDurumu?): Int {
    if (o == null || !o.bitti) return 1
    return when {
        ben.dogru != o.dogru -> if (ben.dogru > o.dogru) 1 else -1
        ben.sure != o.sure -> if (ben.sure < o.sure) 1 else -1
        else -> 0
    }
}

@Composable
private fun AramaEkrani(dersAd: String, gecen: Int, eslesti: Boolean = false, vazgec: () -> Unit) {
    val nabiz = rememberInfiniteTransition(label = "nabiz")
    val olcek by nabiz.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "olcek")
    Column(
        Modifier.fillMaxSize().background(YesilKoyu).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(140.dp).scale(olcek).clip(CircleShape).background(Yesil).border(3.dp, Altin, CircleShape), contentAlignment = Alignment.Center) {
            Text("⚔️", fontSize = 60.sp)
        }
        Bosluk(24)
        Text(if (eslesti) "Rakip bulundu!" else "Rakip aranıyor…", fontFamily = Baslik, fontSize = 26.sp, color = Krem)
        Text(dersAd, color = Altin, style = MaterialTheme.typography.labelLarge)
        Bosluk(8)
        if (!eslesti) {
            Text("0:${gecen.toString().padStart(2, '0')}", color = KremSoluk, fontSize = 18.sp)
            Text("${CanliDuello.BEKLEME_SN} saniyede kimse gelmezse antrenman rakibiyle oynarsın.", color = KremSoluk, fontSize = 13.sp, textAlign = TextAlign.Center)
            Bosluk(24)
            OutlinedButton(onClick = vazgec) { Text("Vazgeç", color = Krem) }
        }
    }
}

@Composable
private fun CanliOyun(
    sorular: List<Soru>,
    rakipAd: String,
    rakip: OyuncuDurumu,
    cevapla: (Soru, Boolean) -> Unit,
    ilerle: (OyuncuDurumu) -> Unit,
    bitti: (OyuncuDurumu) -> Unit,
    rakipBekle: Boolean,
    canliRakip: () -> OyuncuDurumu?,
    vazgec: () -> Unit,
) {
    var sira by remember { mutableIntStateOf(0) }
    var secim by remember { mutableStateOf<Int?>(null) }
    var ben by remember { mutableStateOf(OyuncuDurumu()) }
    var saniye by remember { mutableIntStateOf(0) }
    var bekliyor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { while (!ben.bitti) { delay(1000); saniye++ } }
    LaunchedEffect(secim) {
        val s = secim ?: return@LaunchedEffect
        delay(800)
        val dg = s == sorular[sira].dogru
        ben = ben.copy(cevaplanan = sira + 1, dogru = ben.dogru + if (dg) 1 else 0, sure = saniye, bitti = sira == sorular.lastIndex)
        ilerle(ben)
        if (ben.bitti) {
            bekliyor = true
            if (rakipBekle) {
                // Rakip bitirene kadar en çok 60 sn bekle.
                withTimeoutOrNull(60_000) { while (canliRakip()?.bitti != true) delay(500) }
            }
            bitti(ben)
        } else {
            sira++; secim = null
        }
    }

    Scaffold(
        topBar = { UstCubuk("⚔️ ${minOf(sira + 1, sorular.size)}/${sorular.size}", vazgec) { Text("⏱ $saniye", color = Krem, modifier = Modifier.padding(end = 16.dp)) } },
        containerColor = Zemin,
    ) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Canlı skor tabelası
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkorKutusu(Modifier.weight(1f), "Sen", ben, sorular.size, vurgu = true)
                SkorKutusu(Modifier.weight(1f), rakipAd, rakip, sorular.size, vurgu = false)
            }
            if (bekliyor) {
                Bosluk(24)
                Text(
                    if (rakip.bitti) "Sonuçlar hesaplanıyor…" else "Bitirdin! $rakipAd'in bitirmesi bekleniyor…",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                val soru = sorular[sira]
                Text(soru.metin, style = MaterialTheme.typography.titleMedium)
                soru.secenekler.forEachIndexed { i, m ->
                    val d = when {
                        secim == null -> SikDurumu.NORMAL
                        i == soru.dogru -> SikDurumu.DOGRU
                        i == secim -> SikDurumu.YANLIS
                        else -> SikDurumu.SOLUK
                    }
                    Sik(HARFLER[i], m, d, if (secim == null) ({ secim = i; cevapla(soru, i == soru.dogru) }) else null)
                }
            }
        }
    }
}

@Composable
private fun SkorKutusu(modifier: Modifier, ad: String, d: OyuncuDurumu, toplam: Int, vurgu: Boolean) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (vurgu) Yesil else Kart).border(1.dp, Cizgi, RoundedCornerShape(14.dp)).padding(10.dp),
    ) {
        Text(ad, color = if (vurgu) Krem else Metin, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${d.dogru}", fontFamily = Baslik, fontSize = 26.sp, color = if (vurgu) Altin else Yesil)
            Text(" doğru", color = if (vurgu) KremSoluk else MetinSoluk, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        Bosluk(4)
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (vurgu) YesilKoyu else Zemin)) {
            Box(Modifier.fillMaxWidth(d.cevaplanan / toplam.toFloat()).height(5.dp).background(Altin))
        }
        Text(if (d.bitti) "bitirdi ✓" else "${d.cevaplanan}/$toplam soru", color = if (vurgu) KremSoluk else MetinSoluk, fontSize = 11.sp)
    }
}

@Composable
private fun CanliSonuc(a: Asama.Sonuc, tekrar: () -> Unit, geri: () -> Unit) {
    val s = sonucHesapla(a.benim, a.rakip)
    Scaffold(topBar = { UstCubuk("Düello sonucu", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(when (s) { 1 -> "🏆"; 0 -> "🤝"; else -> "😤" }, fontSize = 64.sp)
            Text(
                when {
                    a.rakip?.bitti != true && !a.bot -> "Rakip yarıda bıraktı, kazandın!"
                    s == 1 -> "Kazandın!"
                    s == 0 -> "Berabere!"
                    else -> "Bu sefer kaybettin"
                },
                fontFamily = Baslik, fontSize = 26.sp, textAlign = TextAlign.Center,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkorKutusu(Modifier.weight(1f), "Sen", a.benim, 5, vurgu = true)
                SkorKutusu(Modifier.weight(1f), a.rakipAd, a.rakip ?: OyuncuDurumu(), 5, vurgu = false)
            }
            Text(
                "+" + when (s) { 1 -> if (a.bot) 15 else 40; 0 -> if (a.bot) 8 else 20; else -> 5 } + " XP",
                color = Yesil, style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = tekrar, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("⚔️ Yeni rakip bul") }
            OutlinedButton(onClick = geri, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Kapat") }
        }
    }
}
