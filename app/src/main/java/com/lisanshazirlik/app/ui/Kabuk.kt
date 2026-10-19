package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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

enum class Sekme(val ad: String, val ikon: ImageVector) {
    ANA("Ana sayfa", Icons.Filled.Home),
    CALIS("Çalış", Icons.AutoMirrored.Filled.List),
    DENEME("Deneme", Icons.Filled.CheckCircle),
    PROFIL("Profil", Icons.Filled.Person),
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
)

@Composable
fun Kabuk(
    sekme: Sekme,
    sekmeSec: (Sekme) -> Unit,
    icerik: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Zemin,
        bottomBar = {
            NavigationBar(containerColor = Kart) {
                Sekme.entries.forEach { s ->
                    NavigationBarItem(
                        selected = s == sekme,
                        onClick = { sekmeSec(s) },
                        icon = { Icon(s.ikon, contentDescription = null) },
                        label = { Text(s.ad) },
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
    val aktif = depo.aktifHedefler().filter { it.kalanGun() >= 0 }
    if (aktif.isEmpty()) return 20
    val toplam = aktif.sumOf { h ->
        val sorular = banka.dersSorulari(h.ders)
        val kalan = sorular.count { it.id !in cozulen }
        val gun = maxOf(1L, h.kalanGun())
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

@Composable
fun AnaSekme(banka: SoruBankasi, depo: KullaniciDeposu, y: Yonlendirici, ic: PaddingValues) {
    val k = depo.kullanici
    val cozulen = depo.cozulenler()
    val aktif = depo.aktifHedefler()
    val gecilen = depo.hedefler.filter { it.gecti }
    val enYakin = aktif.filter { it.kalanGun() >= 0 }.minByOrNull { it.sinavTarihi }
    val hedefSayi = gunlukHedef(banka, depo)
    val bugun = depo.bugunCozulen
    val zayif = zayifKonu(banka, depo)
    val sonKonu = k?.sonKonu?.let { id ->
        banka.dersler.firstNotNullOfOrNull { d -> d.konular.firstOrNull { it.id == id }?.let { d to it } }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Yesil)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Krem), contentAlignment = Alignment.Center) {
                        Text(basHarf(k?.ad.orEmpty()), color = Yesil, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Merhaba ${k?.ad.orEmpty()}", fontFamily = Baslik, fontSize = 24.sp, color = Krem)
                        Text(
                            when {
                                aktif.isEmpty() -> "Düzey 1 · Henüz hedef seçmedin"
                                else -> "Düzey 1 · ${aktif.size} derse hazırlanıyorsun"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = KremSoluk,
                        )
                    }
                }
                Bosluk(16)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UstKutu(
                        Modifier.weight(1f),
                        "En yakın sınav",
                        when {
                            enYakin == null -> "–"
                            enYakin.kalanGun() == 0L -> "Bugün"
                            else -> "${enYakin.kalanGun()} gün"
                        },
                        Altin,
                    )
                    UstKutu(Modifier.weight(1f), "Çalışma serisi", "${depo.seri()} gün", Krem)
                }
            }
        }

        item {
            KartKutu(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bugünkü hedef", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("$bugun / $hedefSayi", style = MaterialTheme.typography.titleMedium, color = if (bugun >= hedefSayi) DogruYesil else Yesil)
                }
                Bosluk(10)
                Ilerleme(bugun / hedefSayi.toFloat())
                Bosluk(8)
                Text(
                    when {
                        bugun >= hedefSayi -> "Bugünkü hedefini tamamladın. Harika!"
                        aktif.isEmpty() -> "Hedef eklersen günlük soru sayın sınav tarihine göre hesaplanır."
                        else -> "Sınav tarihine göre hesaplandı. ${hedefSayi - bugun} soru kaldı."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinSoluk,
                )
            }
        }

        if (sonKonu != null) {
            item {
                val (d, kn) = sonKonu
                val sorular = banka.konuSorulari(kn.id)
                val c = sorular.count { it.id in cozulen }
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Yesil)
                        .clickableIf(sorular.isNotEmpty()) { y.coz(kn.ad, calismaSirasi(sorular, depo), kn.id, "pratik") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Kaldığın yerden devam", style = MaterialTheme.typography.bodyMedium, color = KremSoluk)
                        Text(kn.ad, style = MaterialTheme.typography.titleMedium, color = Krem)
                        Text("${d.kisaAd} · ${sorular.size} sorudan $c çözüldü", style = MaterialTheme.typography.bodyMedium, color = KremSoluk)
                    }
                    Text("▶", color = Altin, fontSize = 22.sp)
                }
            }
        }

        item { Etiket("Hedeflerin", Modifier.padding(start = 20.dp, top = 6.dp)) }

        if (aktif.isEmpty()) {
            item {
                KartKutu(Modifier.padding(horizontal = 16.dp), tikla = { y.hedefAc(null) }) {
                    Text("Hangi derse çalışıyorsun?", style = MaterialTheme.typography.titleMedium)
                    Bosluk(4)
                    Text("Ders ve sınav tarihi seç; kalan günleri ve günlük hedefini takip et.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    Bosluk(8)
                    Text("Hedef ekle", style = MaterialTheme.typography.labelLarge, color = Yesil)
                }
            }
        }
        items(aktif, key = { "h" + it.ders }) { h -> HedefKarti(h, banka, cozulen, y) }
        if (aktif.isNotEmpty() && depo.hedefler.size < banka.dersler.size) {
            item {
                Text(
                    "+ Başka bir ders ekle",
                    style = MaterialTheme.typography.labelLarge,
                    color = Yesil,
                    modifier = Modifier.padding(horizontal = 20.dp).clickableIf(true) { y.hedefAc(null) }.padding(vertical = 6.dp),
                )
            }
        }

        if (zayif != null) {
            item {
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(YanlisZemin)
                        .clickableIf(true) { y.coz(zayif.konuAd, calismaSirasi(zayif.sorular, depo), zayif.konuId, "pratik") }
                        .padding(16.dp),
                ) {
                    Text("Zayıf konun", style = MaterialTheme.typography.bodyMedium, color = YanlisKirmizi)
                    Text("${zayif.konuAd} · %${zayif.oran} doğru", style = MaterialTheme.typography.titleMedium, color = Metin)
                    Text("Dokun, bu konuyu tekrar çöz.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
        }

        if (gecilen.isNotEmpty()) {
            item {
                KartKutu(Modifier.padding(horizontal = 16.dp)) {
                    Text("Geçtiğin dersler", style = MaterialTheme.typography.titleMedium)
                    Bosluk(6)
                    gecilen.forEach { h ->
                        Text("✓  ${banka.ders(h.ders)?.kisaAd ?: h.ders}", style = MaterialTheme.typography.bodyMedium, color = DogruYesil)
                    }
                    Bosluk(4)
                    Text("${gecilen.size} / ${banka.dersler.size} ders tamamlandı", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ders.kisaAd, style = MaterialTheme.typography.titleMedium)
                Text("Sınav: ${uzunTarih(h.sinavTarihi)}", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
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

// ---------------------------------------------------------------- Çalış

@Composable
fun CalisSekme(banka: SoruBankasi, depo: KullaniciDeposu, y: Yonlendirici, ic: PaddingValues) {
    val cozulen = depo.cozulenler()
    val dogru = depo.dogrular()
    val yanlislar = depo.yanlislar().mapNotNull { banka.bul(it) }
    Column(Modifier.fillMaxSize()) {
        SekmeBasligi("Çalış")
        LazyColumn(
            Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                KartKutu(tikla = if (yanlislar.isNotEmpty()) ({ y.coz("Yanlışlarım", yanlislar.shuffled(), null, "yanlislar") }) else null) {
                    Text("Yanlışlarım", style = MaterialTheme.typography.titleMedium)
                    Bosluk(4)
                    Text(
                        if (yanlislar.isEmpty()) "Henüz yanlışın yok. Yanlış yaptığın sorular burada toplanır." else "${yanlislar.size} soruyu tekrar çöz. Doğru yaptıkça listeden çıkar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (yanlislar.isEmpty()) MetinSoluk else YanlisKirmizi,
                    )
                }
            }
            item { Etiket("Dersler", Modifier.padding(start = 4.dp, top = 8.dp)) }
            items(banka.dersler, key = { it.kod }) { d ->
                val sorular = banka.dersSorulari(d.kod)
                val c = sorular.count { it.id in cozulen }
                val dg = sorular.count { it.id in dogru }
                val h = depo.hedef(d.kod)
                KartKutu(tikla = { y.dersAc(d.kod) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.kisaAd, style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    h?.gecti == true -> "Geçtin ✓"
                                    h != null -> "Sınav: ${kisaTarih(h.sinavTarihi)} · ${kalanGunYazisi(h.kalanGun())}"
                                    else -> "${sorular.size} soru · ${d.konular.size} konu"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (h?.gecti == true) DogruYesil else MetinSoluk,
                            )
                        }
                        Text("%${yuzde(c, sorular.size)}", style = MaterialTheme.typography.labelLarge, color = Yesil)
                    }
                    Bosluk(10)
                    Ilerleme(if (sorular.isEmpty()) 0f else c / sorular.size.toFloat())
                    if (c > 0) {
                        Bosluk(6)
                        Text("Doğru oranın %${yuzde(dg, c)}", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Deneme

@Composable
fun DenemeSekme(banka: SoruBankasi, depo: KullaniciDeposu, y: Yonlendirici, ic: PaddingValues) {
    val genelAdet = denemeSoruSayisi(banka)
    Column(Modifier.fillMaxSize()) {
        SekmeBasligi("Deneme")
        LazyColumn(
            Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                KartKutu(tikla = if (genelAdet > 0) y.genelDeneme else null) {
                    Text("Genel deneme", style = MaterialTheme.typography.titleMedium)
                    Bosluk(4)
                    Text("Dört dersten $genelAdet soru, ${denemeSuresiDk(genelAdet)} dakika. Her dersten en az 50, ortalama en az 60.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
            item { Etiket("Ders denemeleri", Modifier.padding(start = 4.dp, top = 8.dp)) }
            items(banka.dersler, key = { "d" + it.kod }) { d ->
                val adet = minOf(20, banka.dersSorulari(d.kod).size)
                KartKutu(tikla = if (adet > 0) ({ y.dersDenemesi(d.kod) }) else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.kisaAd, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("$adet soru · ${denemeSuresiDk(adet)} dk", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    }
                }
            }
            item { Etiket("Geçmiş denemelerin", Modifier.padding(start = 4.dp, top = 8.dp)) }
            if (depo.denemeler.isEmpty()) {
                item { Text("Henüz deneme çözmedin. İlk denemenden sonra sonuçların burada listelenir.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.padding(horizontal = 4.dp)) }
            }
            items(depo.denemeler, key = { it.id }) { dk ->
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
