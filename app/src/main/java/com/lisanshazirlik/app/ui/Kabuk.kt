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
    DERSLER("Eğitim", Icons.Filled.PlayArrow),
    DENEME("Deneme", Icons.Filled.CheckCircle),
    BASARI("Başarı", Icons.Filled.Star),
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
    val konuAc: (String) -> Unit,
    val sonuclar: () -> Unit,
    val lisans: () -> Unit,
    val hatirlatma: () -> Unit,
    val paketAc: (String) -> Unit,
    val paketAdimAc: (ders: String, adimId: String) -> Unit,
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
                        icon = { Icon(s.ikon, contentDescription = null) },
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

@Composable
fun AnaSekme(banka: SoruBankasi, depo: KullaniciDeposu, y: Yonlendirici, ic: PaddingValues) {
    val k = depo.kullanici
    val cozulen = depo.cozulenler()
    val aktif = depo.aktifHedefler()
    val gecilen = depo.hedefler.filter { it.gecti }
    val enYakin = aktif.filter { (it.kalanGun() ?: -1L) >= 0 }.minByOrNull { it.kalanGun()!! }
    val hedefSayi = gunlukHedef(banka, depo)
    val bugun = depo.bugunCozulen
    val zayif = zayifKonu(banka, depo)
    val dersler = banka.lisansDersleri(k?.lisans)
    val lisansSorulari = dersler.flatMap { banka.dersSorulari(it.kod) }
    val gununSorusu = lisansSorulari.takeIf { it.isNotEmpty() }?.let {
        it[(java.time.LocalDate.now().toEpochDay() % it.size).toInt()]
    }
    // Yolun dersi: son çalışılan paket → içeriği olan ilk hedef dersi → içeriği olan ilk ders.
    val icerikli = dersler.filter { Paket.adimlar(banka, it.kod).isNotEmpty() }
    val varsayilan = k?.sonPaket?.takeIf { s -> dersler.any { it.kod == s } }
        ?: aktif.firstOrNull { h -> icerikli.any { it.kod == h.ders } }?.ders
        ?: icerikli.firstOrNull()?.kod
        ?: dersler.firstOrNull()?.kod
    var secili by rememberSaveable { mutableStateOf<String?>(null) }
    val dersKod = secili ?: varsayilan

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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Krem), contentAlignment = Alignment.Center) {
                        Text(basHarf(k?.ad.orEmpty()), color = Yesil, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Merhaba ${k?.ad.orEmpty()}",
                        fontFamily = Baslik,
                        fontSize = 22.sp,
                        color = Krem,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Bosluk(10)
                Text(
                    listOfNotNull(
                        "🔥 ${depo.seri()} gün seri",
                        "⭐ ${k?.xp ?: 0} XP",
                        enYakin?.let { if (it.kalanGun() == 0L) "📅 Sınav bugün" else "📅 Sınava ${it.kalanGun()} gün" },
                    ).joinToString("   "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = KremSoluk,
                )
            }
        }

        // ------------------------------------------------ Öğrenme yolu
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Ne öğreneceksin?", style = MaterialTheme.typography.titleLarge)
                Text("Dersini seç, adımları sırayla izle ve çöz.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(dersler, key = { "c" + it.kod }) { d ->
                    val sec = d.kod == dersKod
                    val bos = Paket.adimlar(banka, d.kod).isEmpty()
                    Text(
                        d.kisaAd,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (sec) Krem else if (bos) MetinSoluk else Yesil,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sec) Yesil else Kart)
                            .border(1.dp, if (sec) Yesil else Cizgi, RoundedCornerShape(50))
                            .clickable {
                                secili = d.kod
                                depo.alanYaz(mapOf("sonPaket" to d.kod))
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
        dersKod?.let { banka.ders(it) }?.let { ders ->
            item(key = "yol" + ders.kod) {
                OgrenmeYolu(banka, depo, ders.kod, y, icerikliBaska = icerikli.firstOrNull { it.kod != ders.kod }?.let { d ->
                    d.kisaAd to { secili = d.kod; depo.alanYaz(mapOf("sonPaket" to d.kod)) }
                })
            }
        }

        // ------------------------------------------------ Diğer
        item { Etiket("Bugün", Modifier.padding(start = 20.dp, top = 10.dp)) }
        item {
            KartKutu(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bugünkü hedef", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("$bugun / $hedefSayi soru", style = MaterialTheme.typography.titleMedium, color = if (bugun >= hedefSayi) DogruYesil else Yesil)
                }
                Bosluk(10)
                Ilerleme(bugun / hedefSayi.toFloat())
                if (bugun >= hedefSayi) {
                    Bosluk(6)
                    Text("Bugünkü hedefini tamamladın. Harika!", style = MaterialTheme.typography.bodyMedium, color = DogruYesil)
                }
            }
        }

        if (gununSorusu != null) {
            item {
                val cevap = depo.ilerleme[gununSorusu.id]
                KartKutu(Modifier.padding(horizontal = 16.dp), tikla = { y.coz("Günün sorusu", listOf(gununSorusu), null, "gunun") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Etiket("Günün sorusu", Modifier.weight(1f))
                        Text(
                            when (cevap) { null -> "Çöz"; 1 -> "✓ Doğru"; else -> "Tekrar dene" },
                            style = MaterialTheme.typography.labelLarge,
                            color = when (cevap) { null -> Yesil; 1 -> DogruYesil; else -> YanlisKirmizi },
                        )
                    }
                    Bosluk(6)
                    Text(gununSorusu.metin, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
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
                        .clickableIf(true) { y.konuAc(zayif.konuId) }
                        .padding(16.dp),
                ) {
                    Text("Zayıf konun", style = MaterialTheme.typography.bodyMedium, color = YanlisKirmizi)
                    Text("${zayif.konuAd} · %${zayif.oran} doğru", style = MaterialTheme.typography.titleMedium, color = Metin)
                    Text("Dokun, özeti oku ve soruları tekrar çöz.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
        }

        item { Etiket("Sınav hedeflerin", Modifier.padding(start = 20.dp, top = 10.dp)) }
        if (aktif.isEmpty()) {
            item {
                KartKutu(Modifier.padding(horizontal = 16.dp), tikla = { y.hedefAc(null) }) {
                    Text("Sınav tarihin belli mi?", style = MaterialTheme.typography.titleMedium)
                    Bosluk(4)
                    Text("Tarih eklersen kalan günleri ve günlük soru hedefini hesaplarız.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    Bosluk(8)
                    Text("Hedef ekle", style = MaterialTheme.typography.labelLarge, color = Yesil)
                }
            }
        }
        items(aktif, key = { "h" + it.ders }) { h -> HedefKarti(h, banka, cozulen, y) }
        if (aktif.isNotEmpty() && depo.hedefler.size < dersler.size) {
            item {
                Text(
                    "+ Başka bir ders ekle",
                    style = MaterialTheme.typography.labelLarge,
                    color = Yesil,
                    modifier = Modifier.padding(horizontal = 20.dp).clickableIf(true) { y.hedefAc(null) }.padding(vertical = 6.dp),
                )
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
                    Text("${gecilen.size} / ${dersler.size} ders tamamlandı", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
        }
    }
}

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

    KartKutu(Modifier.padding(horizontal = 16.dp)) {
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
    val cozulen = depo.cozulenler()
    val dogru = depo.dogrular()
    val yanlislar = depo.yanlislar().mapNotNull { banka.bul(it) }
    Column(Modifier.fillMaxSize()) {
        SekmeBasligi("Eğitim")
        LazyColumn(
            Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(start = 4.dp)) {
                    Etiket("Eğitim paketlerin")
                    Bosluk(2)
                    Text("Video → özet → kartlar → sorular, sırayla. Sonunda ders denemesi.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
            items(dersler, key = { "p" + it.kod }) { d -> PaketSatiri(banka, depo, d.kod, d.kisaAd, y) }
            item { Etiket("Serbest çalışma", Modifier.padding(start = 4.dp, top = 12.dp)) }
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
            item {
                KartKutu(tikla = y.sonuclar) {
                    Text("Sınav sonuçlarım", style = MaterialTheme.typography.titleMedium)
                    Bosluk(4)
                    Text("Girdiğin sınavların puanlarını kaydet, lisans durumunu gör.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
            item {
                Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Etiket(lisans?.ad ?: "Dersler", Modifier.weight(1f))
                    Text("Değiştir", style = MaterialTheme.typography.labelLarge, color = Yesil, modifier = Modifier.clickableIf(true, y.lisans))
                }
            }
            items(dersler, key = { it.kod }) { d ->
                val sorular = banka.dersSorulari(d.kod)
                val c = sorular.count { it.id in cozulen }
                val dg = sorular.count { it.id in dogru }
                val h = depo.hedef(d.kod)
                KartKutu(tikla = { y.dersAc(d.kod) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.ad, style = MaterialTheme.typography.titleMedium)
                            if (d.aciklama.isNotBlank()) {
                                Text(d.aciklama, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                            }
                            Bosluk(4)
                            Text(
                                when {
                                    h?.gecti == true -> "Geçtin ✓"
                                    h != null && h.sinavTarihi != null -> "Sınav: ${kisaTarih(h.sinavTarihi)} · ${kalanGunYazisi(h.kalanGun()!!)}"
                                    h != null -> "Hedefte · tarih seçilmedi"
                                    else -> "${sorular.size} soru · ${d.konular.size} konu"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (h?.gecti == true) DogruYesil else Yesil,
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

@Composable
private fun PaketSatiri(banka: SoruBankasi, depo: KullaniciDeposu, dersKod: String, ad: String, y: Yonlendirici) {
    val adimlar = Paket.adimlar(banka, dersKod)
    val bitti = Paket.tamamlananlar(depo, dersKod)
    val tamam = adimlar.count { it.id in bitti }
    val sonraki = adimlar.firstOrNull { it.id !in bitti }
    val bos = adimlar.isEmpty()
    val bitmis = !bos && sonraki == null
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (bos) Kart else if (bitmis) Yesil else YesilKoyu)
            .clickableIf(!bos) { y.paketAc(dersKod) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (bitmis) "🏅" else "📦", fontSize = 24.sp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(ad, style = MaterialTheme.typography.titleMedium, color = if (bos) MetinSoluk else Krem)
            Text(
                when {
                    bos -> "Hazırlanıyor · yakında"
                    bitmis -> "Paket tamamlandı ✓"
                    tamam == 0 -> "${adimlar.size} adım · Başla"
                    else -> "Adım ${tamam + 1}/${adimlar.size} · ${sonraki!!.tur.ad}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (bos) MetinSoluk else KremSoluk,
            )
            if (!bos) {
                Bosluk(8)
                Ilerleme(tamam / adimlar.size.toFloat())
            }
        }
        if (!bos) Text("▶", color = Altin, fontSize = 20.sp)
    }
}

// ---------------------------------------------------------------- Deneme

@Composable
fun DenemeSekme(banka: SoruBankasi, depo: KullaniciDeposu, y: Yonlendirici, ic: PaddingValues) {
    val dersler = banka.lisansDersleri(depo.kullanici?.lisans)
    val genelAdet = denemeSoruSayisi(banka, dersler)
    val denemeler = depo.denemeler.filter { !it.kapsam.startsWith("konu:") }
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
                    Text("${dersler.size} dersten $genelAdet soru, ${denemeSuresiDk(genelAdet)} dakika. Gerçek sınavda her ders 25 soru ve 45 dakika. Her dersten en az 50, ortalama en az 60.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
            item { Etiket("Ders denemeleri", Modifier.padding(start = 4.dp, top = 8.dp)) }
            items(dersler, key = { "d" + it.kod }) { d ->
                val adet = minOf(SINAV_SORU, banka.dersSorulari(d.kod).size)
                KartKutu(tikla = if (adet > 0) ({ y.dersDenemesi(d.kod) }) else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.kisaAd, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("$adet soru · ${denemeSuresiDk(adet)} dk", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
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
