package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lisanshazirlik.app.data.Paket
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.Hedef
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import kotlin.math.ceil

enum class Sekme(val ad: String, val ikon: ImageVector, val resim: Int? = null) {
    ANA("Ana sayfa", Icons.Filled.Home, com.lisanshazirlik.app.R.drawable.ic_ana),
    DERSLER("Eğitim", Icons.Filled.PlayArrow, com.lisanshazirlik.app.R.drawable.ic_egitim),
    DENEME("Deneme", Icons.Filled.CheckCircle, com.lisanshazirlik.app.R.drawable.ic_deneme),
    BASARI("Başarı", Icons.Filled.Star, com.lisanshazirlik.app.R.drawable.ic_basari),
    PROFIL("Profil", Icons.Filled.Person, com.lisanshazirlik.app.R.drawable.ic_profil),
}

/** Ekranlar arası geçişler; ekranlar birbirini doğrudan tanımaz. */
class Yonlendirici(
    val dersAc: (String) -> Unit,
    val coz: (baslik: String, sorular: List<Soru>, konuId: String?, mod: String) -> Unit,
    val genelDeneme: () -> Unit,
    val dersDenemesi: (String) -> Unit,
    val hedefAc: (String?) -> Unit,
    val gorus: () -> Unit,
    val aydinlatma: () -> Unit,
    val verilerim: () -> Unit,
    val premium: () -> Unit,
    val konuAc: (String) -> Unit,
    val sonuclar: () -> Unit,
    val lisans: () -> Unit,
    val hatirlatma: () -> Unit,
    val paketAc: (String) -> Unit,
    val paketAdimAc: (ders: String, adimId: String) -> Unit,
    val sekmeAc: (Sekme) -> Unit,
    val basariAc: (BasariBolumu) -> Unit,
)

@Composable
fun Kabuk(
    sekme: Sekme,
    sekmeSec: (Sekme) -> Unit,
    menuGoster: Boolean = true,
    icerik: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Zemin,
        bottomBar = {
            if (menuGoster) NavigationBar(containerColor = Kart) {
                Sekme.entries.forEach { s ->
                    NavigationBarItem(
                        selected = s == sekme,
                        onClick = { sekmeSec(s) },
                        icon = {
                            if (s.resim != null) {
                                Icon(
                                    androidx.compose.ui.res.painterResource(s.resim),
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                                    modifier = Modifier.size(28.dp).alpha(if (s == sekme) 1f else 0.6f),
                                )
                            } else Icon(s.ikon, contentDescription = null)
                        },
                        label = { Text(s.ad, maxLines = 1, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Yesil,
                            selectedTextColor = Yesil,
                            indicatorColor = YesilAcik,
                            unselectedIconColor = MetinSoluk,
                            unselectedTextColor = MetinSoluk,
                        ),
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { ic -> icerik(ic) }
}

// ---------------------------------------------------------------- Hesaplamalar

/** Aktif hedeflere göre bugün çözülmesi önerilen soru sayısı. */
fun gunlukHedef(banka: SoruBankasi, depo: KullaniciDeposu): Int {
    val cozulen = depo.cozulenler()
    // Tarihi geçmiş hedefler hesaba katılmaz; tarihsiz hedefler için 30 gün varsayılır.
    val aktif = depo.aktifHedefler().filter { (it.kalanGun() ?: 0L) >= 0 }
    if (aktif.isEmpty()) return 20
    val toplam = aktif.sumOf { h ->
        val sorular = banka.dersSorulari(h.ders)
        val kalan = sorular.count { it.id !in cozulen }
        val gun = maxOf(1L, h.kalanGun() ?: 30L)
        maxOf(ceil(kalan / gun.toDouble()).toInt(), minOf(10, sorular.size))
    }
    return toplam.coerceIn(5, 100)
}

data class ZayifKonu(val dersKod: String, val konuId: String, val konuAd: String, val oran: Int, val sorular: List<Soru>)

fun zayifKonu(banka: SoruBankasi, depo: KullaniciDeposu): ZayifKonu? {
    val il = depo.ilerleme
    return banka.dersler.flatMap { d -> d.konular.map { d to it } }.mapNotNull { (d, k) ->
        val sorular = banka.konuSorulari(k.id)
        val cevaplanan = sorular.filter { it.id in il }
        if (cevaplanan.size < 3) return@mapNotNull null
        val oran = yuzde(cevaplanan.count { il[it.id] == 1 }, cevaplanan.size)
        if (oran >= 60) null else ZayifKonu(d.kod, k.id, k.ad, oran, sorular)
    }.minByOrNull { it.oran }
}

/** Çözülmemiş sorular önce, sonra yanlışlar, en son doğrular. */
fun calismaSirasi(sorular: List<Soru>, depo: KullaniciDeposu): List<Soru> {
    val il = depo.ilerleme
    return sorular.shuffled().sortedBy { s -> when (il[s.id]) { null -> 0; 0 -> 1; else -> 2 } }
}

// ---------------------------------------------------------------- Ana sayfa

/** Ana sayfanın kalbi: seçili dersin adımları yukarıdan aşağıya bir yol gibi. */
@Composable
private fun OgrenmeYolu(
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    dersKod: String,
    y: Yonlendirici,
    icerikliBaska: Pair<String, () -> Unit>?,
) {
    val ders = banka.ders(dersKod) ?: return
    val adimlar = Paket.adimlar(banka, dersKod)
    val bitti = Paket.tamamlananlar(depo, dersKod)
    val siradaki = adimlar.firstOrNull { it.id !in bitti }
    val tamam = adimlar.count { it.id in bitti }

    KartKutu {
        Text(ders.ad, style = MaterialTheme.typography.titleMedium)
        if (ders.aciklama.isNotBlank()) Text(ders.aciklama, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)

        if (adimlar.isEmpty()) {
            Bosluk(10)
            Text("Bu dersin anlatımları hazırlanıyor.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            if (icerikliBaska != null) {
                Bosluk(10)
                Button(onClick = icerikliBaska.second, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("${icerikliBaska.first} dersiyle başla")
                }
            }
            return@KartKutu
        }

        Bosluk(10)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ilerleme(tamam / adimlar.size.toFloat(), Modifier.weight(1f))
            Text("  $tamam/${adimlar.size} adım", style = MaterialTheme.typography.labelLarge, color = Yesil)
        }
        Bosluk(8)

        var sonKonu: String? = "-"
        var konuNo = 0
        adimlar.forEach { a ->
            val konuId = a.konu?.id
            if (konuId != sonKonu) {
                sonKonu = konuId
                Bosluk(10)
                Text(
                    if (a.konu != null) "${++konuNo}. ${a.konu.ad}" else "Son adım",
                    style = MaterialTheme.typography.labelLarge,
                    color = MetinSoluk,
                )
                Bosluk(4)
            }
            YolAdimi(a, durum = when {
                a.id in bitti -> 2
                a == siradaki -> 1
                else -> 0
            }) { y.paketAdimAc(dersKod, a.id) }
        }

        val hazirlaniyor = ders.konular.filter { kn -> adimlar.none { it.konu?.id == kn.id } }
        if (hazirlaniyor.isNotEmpty()) {
            Bosluk(12)
            Text(
                "Hazırlanıyor: " + hazirlaniyor.joinToString(", ") { it.ad },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = MetinSoluk,
            )
        }

        Bosluk(14)
        if (siradaki != null) {
            Button(onClick = { y.paketAdimAc(dersKod, siradaki.id) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (tamam == 0) "▶  Başla: ${siradaki.tur.ad}" else "▶  Devam et: ${siradaki.tur.ad}")
            }
        } else {
            Text("🏅 Bu paketi tamamladın!", style = MaterialTheme.typography.titleMedium, color = DogruYesil)
            if (icerikliBaska != null) {
                Bosluk(8)
                OutlinedButton(onClick = icerikliBaska.second, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Sıradaki ders: ${icerikliBaska.first}")
                }
            }
        }
    }
}

/** durum: 0 sırada bekliyor, 1 şimdi, 2 bitti. */
@Composable
private fun YolAdimi(a: Paket.Adim, durum: Int, tikla: () -> Unit) {
    val simdi = durum == 1
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (simdi) YesilAcik else Kart)
            .clickable(onClick = tikla)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    when (durum) {
                        2 -> DogruYesil
                        1 -> Yesil
                        else -> Zemin
                    }
                )
                .border(1.dp, if (durum == 0) Cizgi else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when (durum) { 2 -> "✓"; 1 -> "▶"; else -> "" },
                color = Krem,
                fontSize = 12.sp,
            )
        }
        Text("${a.tur.simge}  ${a.tur.ad}", style = MaterialTheme.typography.bodyMedium, color = if (durum == 0) MetinSoluk else Metin,
            modifier = Modifier.weight(1f).padding(start = 10.dp))
        if (simdi) Text("şimdi", style = MaterialTheme.typography.labelMedium, color = Yesil)
    }
}

@Composable
private fun UstKutu(modifier: Modifier, etiket: String, deger: String, renk: androidx.compose.ui.graphics.Color) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(YesilKoyu).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(etiket, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = KremSoluk)
        Text(deger, style = MaterialTheme.typography.titleLarge, color = renk)
    }
}

@Composable
private fun HedefKarti(h: Hedef, banka: SoruBankasi, cozulen: Set<String>, y: Yonlendirici) {
    val ders = banka.ders(h.ders) ?: return
    val sorular = banka.dersSorulari(h.ders)
    val c = sorular.count { it.id in cozulen }
    val gun = h.kalanGun()
    KartKutu(Modifier.padding(horizontal = 16.dp), tikla = { y.dersAc(h.ders) }) {
        if (gun == null) {
            Text(ders.kisaAd, style = MaterialTheme.typography.titleMedium)
            Text("Sınav tarihi seçilmedi", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            Bosluk(10)
            Ilerleme(if (sorular.isEmpty()) 0f else c / sorular.size.toFloat())
            Bosluk(6)
            Row {
                Text("${sorular.size} sorudan $c çözüldü", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.weight(1f))
                Text("Tarih ekle", style = MaterialTheme.typography.labelLarge, color = Yesil, modifier = Modifier.clickableIf(true) { y.hedefAc(h.ders) })
            }
            return@KartKutu
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ders.kisaAd, style = MaterialTheme.typography.titleMedium)
                Text("Sınav: ${uzunTarih(h.sinavTarihi!!)}", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (gun >= 0) "$gun" else "–",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (gun in 0..7) YanlisKirmizi else Yesil,
                )
                Text(if (gun >= 0) "gün kaldı" else "tarih geçti", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = MetinSoluk)
            }
        }
        Bosluk(10)
        Ilerleme(if (sorular.isEmpty()) 0f else c / sorular.size.toFloat())
        Bosluk(6)
        Row {
            Text("${sorular.size} sorudan $c çözüldü", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.weight(1f))
            Text(
                "Düzenle",
                style = MaterialTheme.typography.labelLarge,
                color = Yesil,
                modifier = Modifier.clickableIf(true) { y.hedefAc(h.ders) },
            )
        }
        if (gun < 0) {
            Bosluk(6)
            Text("Sınav tarihi geçti. Geçtiysen \"Düzenle\"den işaretle, yeni tarihin varsa güncelle.", style = MaterialTheme.typography.bodyMedium, color = YanlisKirmizi)
        }
    }
}

// ---------------------------------------------------------------- Dersler

@Composable
fun DerslerSekme(banka: SoruBankasi, depo: KullaniciDeposu, y: Yonlendirici, ic: PaddingValues) {
    val lisans = banka.lisans(depo.kullanici?.lisans)
    val dersler = banka.lisansDersleri(depo.kullanici?.lisans)
    val renkler = listOf(Yesil, androidx.compose.ui.graphics.Color(0xFF24476B), androidx.compose.ui.graphics.Color(0xFF7A2E3A), androidx.compose.ui.graphics.Color(0xFF7A5A12))
    Column(Modifier.fillMaxSize()) {
        SekmeBasligi("Eğitim")
        LazyColumn(
            Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Hangi derse çalışacaksın?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text(lisans?.kisaAd ?: "", style = MaterialTheme.typography.labelLarge, color = Yesil, modifier = Modifier.clickableIf(true, y.lisans))
                }
            }
            items(dersler.withIndex().toList(), key = { "k" + it.value.kod }) { (i, d) ->
                KursKarti(banka, depo, d, renkler[i % renkler.size]) { y.paketAc(d.kod) }
            }
        }
    }
}

@Composable
private fun KursKarti(banka: SoruBankasi, depo: KullaniciDeposu, d: com.lisanshazirlik.app.data.Ders, renk: androidx.compose.ui.graphics.Color, ac: () -> Unit) {
    val adimlar = Paket.adimlar(banka, d.kod)
    val bitti = Paket.tamamlananlar(depo, d.kod)
    val tamam = adimlar.count { it.id in bitti }
    val oran = if (adimlar.isEmpty()) 0f else tamam / adimlar.size.toFloat()
    val videolar = d.konular.count { it.video.isNotBlank() }
    val sekil = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(sekil).background(Kart).border(1.dp, Cizgi, sekil).clickable(onClick = ac),
    ) {
        Row(
            Modifier.fillMaxWidth().background(renk).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                d.kod,
                style = MaterialTheme.typography.labelLarge,
                color = renk,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Krem).padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Text(
                d.kisaAd,
                style = MaterialTheme.typography.titleMedium,
                color = Krem,
                maxLines = 2,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
        }
        Column(Modifier.padding(16.dp)) {
            Text(
                listOfNotNull("${d.konular.size} konu", if (videolar > 0) "$videolar video" else null).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
            )
            Bosluk(10)
            if (adimlar.isEmpty()) {
                Text("🔜 Anlatımlar hazırlanıyor", style = MaterialTheme.typography.labelLarge, color = MetinSoluk)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ilerleme(oran, Modifier.weight(1f))
                    Text("  %${(oran * 100).toInt()}", style = MaterialTheme.typography.labelLarge, color = Yesil)
                }
                Bosluk(12)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (tamam == adimlar.size) DogruYesil else Yesil).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        when {
                            tamam == adimlar.size -> "🏅 Tamamlandı · Tekrar et"
                            tamam == 0 -> "▶  Başla"
                            else -> "▶  Devam et"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Krem,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Deneme

@Composable
fun DenemeSekme(banka: SoruBankasi, depo: KullaniciDeposu, y: Yonlendirici, ic: PaddingValues) {
    val dersler = banka.lisansDersleri(depo.kullanici?.lisans)
    val denemeler = depo.denemeler.filter { !it.kapsam.startsWith("konu:") }
    Column(Modifier.fillMaxSize()) {
        SekmeBasligi("Deneme")
        LazyColumn(
            Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text("Gerçek sınavın ölçüleri", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
                    Bosluk(8)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OlcuKutusu(Modifier.weight(1f), "25", "soru", Yesil)
                        OlcuKutusu(Modifier.weight(1f), "45", "dakika", Yesil)
                        OlcuKutusu(Modifier.weight(1f), "50", "ders barajı", YanlisKirmizi)
                        OlcuKutusu(Modifier.weight(1f), "60", "ortalama", Altin)
                    }
                    Bosluk(8)
                    Text(
                        "📌  Her ders ayrı bir sınav. Denemeler de öyle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinSoluk,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            item { Etiket("Ders denemeleri", Modifier.padding(start = 4.dp, top = 8.dp)) }
            items(dersler, key = { "d" + it.kod }) { d ->
                val adet = minOf(SINAV_SORU, banka.dersSorulari(d.kod).size)
                val son = denemeler.filter { it.kapsam == d.kod }.maxByOrNull { it.zaman }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Kart)
                        .border(1.dp, Cizgi, RoundedCornerShape(16.dp))
                        .clickableIf(adet > 0) { y.dersDenemesi(d.kod) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(YesilAcik),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(d.kod, style = MaterialTheme.typography.labelLarge, color = Yesil)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(d.kisaAd, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Bosluk(2)
                        Text(
                            "📋 $DENEME_ADEDI deneme · her biri 25 soru, 45 dk",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = MetinSoluk,
                        )
                        if (son != null) {
                            Text(
                                "Son denemen: ${son.ortalama} · ${if (son.gecti) "Geçer ✓" else "Kalır"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (son.gecti) DogruYesil else YanlisKirmizi,
                            )
                        }
                    }
                    if (adet > 0) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Yesil), contentAlignment = Alignment.Center) {
                            Text("▶", color = Krem, fontSize = 16.sp)
                        }
                    }
                }
            }
            item { Etiket("Geçmiş denemelerin", Modifier.padding(start = 4.dp, top = 8.dp)) }
            if (denemeler.isEmpty()) {
                item { Text("Henüz deneme çözmedin. İlk denemenden sonra sonuçların burada listelenir.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.padding(horizontal = 4.dp)) }
            }
            items(denemeler, key = { it.id }) { dk ->
                val tarih = java.time.Instant.ofEpochMilli(dk.zaman).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                KartKutu {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (dk.kapsam == "genel") "Genel deneme" else (banka.ders(dk.kapsam)?.kisaAd ?: dk.kapsam),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text("${uzunTarih(tarih)} · ${dk.soruSayisi} soru", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${dk.ortalama}", style = MaterialTheme.typography.headlineSmall, color = if (dk.gecti) DogruYesil else YanlisKirmizi)
                            Text(if (dk.gecti) "Geçer" else "Kalır", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = if (dk.gecti) DogruYesil else YanlisKirmizi)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OlcuKutusu(modifier: Modifier, sayi: String, etiket: String, renk: androidx.compose.ui.graphics.Color) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Kart).border(1.dp, Cizgi, RoundedCornerShape(14.dp)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(sayi, fontFamily = Baslik, fontSize = 26.sp, color = renk)
        Text(etiket, style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, color = MetinSoluk, maxLines = 1)
    }
}

@Composable
fun SekmeBasligi(baslik: String) {
    Text(
        baslik,
        fontFamily = Baslik,
        fontSize = 26.sp,
        color = Krem,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(Yesil)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    )
}

fun Modifier.clickableIf(etkin: Boolean, tikla: () -> Unit): Modifier =
    if (etkin) this.clickable(onClick = tikla) else this
