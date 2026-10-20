package com.lisanshazirlik.app.ui

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.Duello
import com.lisanshazirlik.app.data.DuelloKaydi
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface DuelloDurumu {
    data object Ana : DuelloDurumu
    data class Oyun(val sorular: List<Soru>, val katilinan: DuelloKaydi?) : DuelloDurumu
    data class Sonuc(val kayit: DuelloKaydi) : DuelloDurumu
}

@Composable
fun DuelloEkrani(banka: SoruBankasi, depo: KullaniciDeposu, uid: String?, cevapla: (Soru, Boolean) -> Unit, geri: () -> Unit) {
    var durum by remember { mutableStateOf<DuelloDurumu>(DuelloDurumu.Ana) }
    var hata by remember { mutableStateOf<String?>(null) }
    var bekle by remember { mutableStateOf(false) }
    var kod by remember { mutableStateOf("") }
    var liste by remember { mutableStateOf<List<DuelloKaydi>?>(null) }
    val kapsam = rememberCoroutineScope()
    val ad = Duello.gorunenAd(depo.kullanici)
    val dersler = banka.lisansDersleri(depo.kullanici?.lisans).map { it.kod }

    LaunchedEffect(durum) {
        if (durum == DuelloDurumu.Ana && uid != null) liste = runCatching { Duello.benimkiler(uid) }.getOrNull()
    }

    when (val d = durum) {
        is DuelloDurumu.Oyun -> {
            DuelloOyunu(d.sorular, cevapla, vazgec = { durum = DuelloDurumu.Ana }) { dogru, sure ->
                if (uid == null) { durum = DuelloDurumu.Ana; return@DuelloOyunu }
                kapsam.launch {
                    try {
                        val kayit = if (d.katilinan == null) {
                            val yeniKod = Duello.olustur(uid, ad, depo.kullanici?.lisans, d.sorular.map { it.id }, dogru, sure)
                            Duello.getir(yeniKod)
                        } else {
                            Duello.sonucYaz(d.katilinan.kod, uid, ad, dogru, sure)
                            Duello.getir(d.katilinan.kod)
                        }
                        durum = if (kayit != null) DuelloDurumu.Sonuc(kayit) else DuelloDurumu.Ana
                    } catch (e: Exception) {
                        hata = e.message ?: "Bağlantı sorunu"; durum = DuelloDurumu.Ana
                    }
                }
            }
            return
        }
        is DuelloDurumu.Sonuc -> {
            DuelloSonucu(d.kayit, uid.orEmpty(), geri = { durum = DuelloDurumu.Ana })
            return
        }
        DuelloDurumu.Ana -> Unit
    }

    Scaffold(topBar = { UstCubuk("Düello", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(YesilKoyu).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("⚔️", fontSize = 44.sp)
                Text("Arkadaşınla yarış", fontFamily = Baslik, fontSize = 22.sp, color = Krem)
                Text(
                    "5 soru · en çok doğru yapan kazanır · eşitlikte hızlı olan",
                    color = KremSoluk,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Bosluk(14)
                Button(
                    onClick = {
                        val s = Duello.sorulariSec(banka, dersler)
                        if (s.isEmpty()) hata = "Bu lisans için henüz soru yok." else durum = DuelloDurumu.Oyun(s, null)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("⚔️ Yeni düello başlat") }
            }

            KartKutu {
                Text("Arkadaşın kod mu gönderdi?", style = MaterialTheme.typography.titleMedium)
                Bosluk(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = kod,
                        onValueChange = { kod = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6); hata = null },
                        placeholder = { Text("Kod (6 harf)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            if (uid == null) return@Button
                            bekle = true
                            kapsam.launch {
                                try {
                                    val k = Duello.katilKontrol(kod, uid)
                                    val s = k.sorular.mapNotNull { banka.bul(it) }
                                    if (s.isEmpty()) throw Duello.Hata("Bu düellonun soruları bulunamadı. Uygulamayı güncelle.")
                                    durum = DuelloDurumu.Oyun(s, k)
                                } catch (e: Duello.Hata) {
                                    hata = e.message
                                } catch (e: Exception) {
                                    hata = "Bağlantı kurulamadı."
                                }
                                bekle = false
                            }
                        },
                        enabled = kod.length == 6 && !bekle,
                        modifier = Modifier.padding(start = 8.dp).height(56.dp),
                    ) { Text("Katıl") }
                }
            }
            hata?.let { Text(it, color = YanlisKirmizi, style = MaterialTheme.typography.bodyMedium) }

            Text("Düellolarım", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            val l = liste
            when {
                l == null -> CircularProgressIndicator(color = Yesil, modifier = Modifier.align(Alignment.CenterHorizontally))
                l.isEmpty() -> Text("Henüz düellon yok. İlkini başlat!", color = MetinSoluk)
                else -> l.forEach { k -> DuelloSatiri(k, uid.orEmpty()) { durum = DuelloDurumu.Sonuc(k) } }
            }
        }
    }
}

@Composable
private fun DuelloSatiri(k: DuelloKaydi, uid: String, tikla: () -> Unit) {
    val rakipAd = if (uid == k.kurucu) k.rakipAd.ifBlank { "?" } else k.kurucuAd
    val s = k.sonuc(uid)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kart).border(1.dp, Cizgi, RoundedCornerShape(14.dp))
            .clickable(onClick = tikla).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(when (s) { 1 -> "🏆"; 0 -> "🤝"; -1 -> "😤"; else -> "⏳" }, fontSize = 26.sp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(if (k.bitti) "Sen vs $rakipAd" else "Rakip bekleniyor", style = MaterialTheme.typography.titleMedium)
            Text(if (k.bitti) "" else "Kod: ${k.kod}", color = MetinSoluk, fontSize = 13.sp)
        }
        if (k.bitti) {
            val ben = if (uid == k.kurucu) k.kurucuDogru else k.rakipDogru
            val o = if (uid == k.kurucu) k.rakipDogru else k.kurucuDogru
            Text("$ben - $o", style = MaterialTheme.typography.titleLarge, color = when (s) { 1 -> DogruYesil; -1 -> YanlisKirmizi; else -> Metin })
        }
    }
}

@Composable
private fun DuelloOyunu(sorular: List<Soru>, cevapla: (Soru, Boolean) -> Unit, vazgec: () -> Unit, bitti: (dogru: Int, sure: Int) -> Unit) {
    var sira by remember { mutableIntStateOf(0) }
    var secim by remember { mutableStateOf<Int?>(null) }
    var dogru by remember { mutableIntStateOf(0) }
    var saniye by remember { mutableIntStateOf(0) }
    var bitirildi by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (!bitirildi) { delay(1000); saniye++ } }
    LaunchedEffect(secim) {
        if (secim == null) return@LaunchedEffect
        delay(900)
        if (sira == sorular.lastIndex) { bitirildi = true; bitti(dogru, saniye) } else { sira++; secim = null }
    }
    val soru = sorular[sira]
    Scaffold(topBar = { UstCubuk("⚔️ ${sira + 1}/${sorular.size}", vazgec) { Text("⏱ $saniye sn", color = Krem, modifier = Modifier.padding(end = 16.dp)) } }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Ilerleme(sira / sorular.size.toFloat())
            if (bitirildi) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Yesil) }
            } else {
                Text(soru.metin, style = MaterialTheme.typography.titleMedium)
                soru.secenekler.forEachIndexed { i, m ->
                    val d = when {
                        secim == null -> SikDurumu.NORMAL
                        i == soru.dogru -> SikDurumu.DOGRU
                        i == secim -> SikDurumu.YANLIS
                        else -> SikDurumu.SOLUK
                    }
                    Sik(HARFLER[i], m, d, if (secim == null) ({
                        secim = i
                        val dg = i == soru.dogru
                        if (dg) dogru++
                        cevapla(soru, dg)
                    }) else null)
                }
            }
        }
    }
}

@Composable
private fun DuelloSonucu(k: DuelloKaydi, uid: String, geri: () -> Unit) {
    val ctx = LocalContext.current
    val benKurucu = uid == k.kurucu
    val s = k.sonuc(uid)
    Scaffold(topBar = { UstCubuk("Düello sonucu", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(when (s) { 1 -> "🏆"; 0 -> "🤝"; -1 -> "😤"; else -> "⏳" }, fontSize = 64.sp)
            Text(
                when (s) { 1 -> "Kazandın!"; 0 -> "Berabere!"; -1 -> "Bu sefer kaybettin"; else -> "Rakibini bekliyorsun" },
                fontFamily = Baslik, fontSize = 28.sp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OyuncuKutusu(Modifier.weight(1f), if (benKurucu) "Sen" else k.kurucuAd, k.kurucuDogru, k.kurucuSure)
                OyuncuKutusu(Modifier.weight(1f), if (!benKurucu) "Sen" else k.rakipAd.ifBlank { "?" }, k.rakipDogru, k.rakipSure)
            }
            if (!k.bitti && benKurucu) {
                KartKutu {
                    Text("Kodu arkadaşına gönder", style = MaterialTheme.typography.titleMedium)
                    Bosluk(6)
                    Text(k.kod, fontFamily = Baslik, fontSize = 40.sp, color = Yesil, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Bosluk(8)
                    Button(
                        onClick = {
                            val metin = "Lisansla'da seni düelloya davet ediyorum! ⚔️ 5 soruda ${k.kurucuDogru} doğru yaptım, geçebilir misin? Uygulamada Düello → Kodla katıl: ${k.kod}"
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, metin), "Düello kodunu paylaş"))
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("📤 Paylaş") }
                }
            }
            OutlinedButton(onClick = geri, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Düellolara dön") }
        }
    }
}

@Composable
private fun OyuncuKutusu(modifier: Modifier, ad: String, dogru: Int?, sure: Int?) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Kart).border(1.dp, Cizgi, RoundedCornerShape(16.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(YesilAcik), contentAlignment = Alignment.Center) {
            Text(basHarf(ad), color = Yesil, style = MaterialTheme.typography.titleMedium)
        }
        Text(ad, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(if (dogru != null) "$dogru/5" else "–", fontFamily = Baslik, fontSize = 30.sp, color = Yesil)
        Text(if (sure != null) "$sure sn" else "oynamadı", color = MetinSoluk, fontSize = 12.sp)
    }
}
