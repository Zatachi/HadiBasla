package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.Akis
import com.lisanshazirlik.app.data.AkisOlayi
import com.lisanshazirlik.app.data.Cevrimici
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Oyun
import com.lisanshazirlik.app.data.SiralamaSatiri
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.Topluluk
import kotlinx.coroutines.delay
import java.time.LocalDate

private data class Duyuru(val ust: String, val baslik: String, val alt: String, val simge: String, val renk: Color, val tikla: () -> Unit)
private data class Kisayol(val simge: String, val ad: String, val resim: Int? = null, val rozet: Int = 0, val tikla: () -> Unit)

/** Akışta gösterilecek satır: gerçek olay ya da sistemin ürettiği bilgi kartı. */
private data class AkisSatiri(val anahtar: String, val simge: String, val ad: String?, val metin: String, val zaman: String?, val tikla: (() -> Unit)? = null)

/**
 * Ana sayfa platformun buluşma alanıdır: kimler çalışıyor, ligde kim önde, topluluk neler başardı.
 * Kişisel çalışma planı Eğitim sekmesindedir.
 */
@Composable
fun AnaSekme(banka: SoruBankasi, depo: KullaniciDeposu, uid: String?, y: Yonlendirici, ic: PaddingValues) {
    val k = depo.kullanici
    val dersler = banka.lisansDersleri(k?.lisans)
    val videolar = dersler.flatMap { d -> d.konular.filter { it.video.isNotBlank() }.map { d to it } }
    val odak = com.lisanshazirlik.app.data.Odak.ders(banka, depo)
    val bekleyenTekrar = com.lisanshazirlik.app.data.Tekrar.bekleyenler(banka, depo, listOfNotNull(odak?.kod)).size
    val yanlislar = depo.yanlislar().mapNotNull { banka.bul(it) }

    // Canlı veriler: çevrimiçi sayısı her dakika, lig ve akış açılışta yüklenir.
    var cevrimici by remember { mutableStateOf<Cevrimici?>(null) }
    var lig by remember { mutableStateOf<List<SiralamaSatiri>?>(null) }
    var siram by remember { mutableStateOf<Long?>(null) }
    var akis by remember { mutableStateOf<List<AkisOlayi>?>(null) }
    val ligTablo = remember(k?.lisans) { Topluluk.tablolar(banka, k?.lisans).first() }
    LaunchedEffect(Unit) {
        while (true) {
            runCatching { Akis.cevrimici() }.onSuccess { cevrimici = it }
            delay(60_000)
        }
    }
    LaunchedEffect(ligTablo.alan, k?.siralamaKatil) {
        runCatching { Topluluk.liste(ligTablo, 3) }.onSuccess { lig = it }.onFailure { lig = emptyList() }
        if (uid != null && k?.siralamaKatil == true) {
            runCatching { Topluluk.benimDegerim(uid, ligTablo)?.let { Topluluk.sira(ligTablo, it) } }.onSuccess { siram = it }
        }
    }
    LaunchedEffect(Unit) {
        runCatching { Akis.son() }.onSuccess { akis = it }.onFailure { akis = emptyList() }
    }

    val duyurular = buildList {
        videolar.lastOrNull()?.let { (d, kn) ->
            add(Duyuru("Yeni Video", kn.ad, "${d.kisaAd} · Sesli Animasyonlu Anlatım", "🎬", YesilKoyu) { y.konuAc(kn.id) })
        }
        add(Duyuru("Davet Et", "3 Arkadaşına 7 Gün Premium", "Davet Kodunu Paylaş, Birlikte Çalışın", "🎁", Color(0xFF7A2E3A)) { y.basariAc(BasariBolumu.DAVET) })
        add(Duyuru("Deneme", "Gerçek Sınav Gibi Dene", "25 Soru · 45 Dakika · Anında Sonuç", "⏱", Color(0xFF24476B)) { y.sekmeAc(Sekme.DENEME) })
    }
    val duyuru = duyurular[LocalDate.now().dayOfYear % duyurular.size]

    val kisayollar = listOf(
        Kisayol("▶", "Shorts", tikla = y.shorts),
        Kisayol("🔁", "5 Dakika", rozet = bekleyenTekrar, tikla = y.gunun5),
        Kisayol("🎧", "Kulaklık", tikla = y.kulaklik),
        Kisayol("", "Yanlışlarım", com.lisanshazirlik.app.R.drawable.ic_yanlislar, rozet = yanlislar.size) {
            if (yanlislar.isNotEmpty()) y.coz("Yanlışlarım", yanlislar.shuffled(), null, "yanlislar") else y.sekmeAc(Sekme.DERSLER)
        },
        Kisayol("", "Deneme", com.lisanshazirlik.app.R.drawable.ic_deneme) { y.sekmeAc(Sekme.DENEME) },
    )

    LazyColumn(
        Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Üst çubuk
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Yesil)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Lisansla", fontFamily = Baslik, fontSize = 26.sp, color = Krem, modifier = Modifier.weight(1f))
                Rozet("🔥 ${depo.seri()}")
                Box(Modifier.width(8.dp))
                Rozet("⭐ ${k?.xp ?: 0}")
            }
        }

        // Şu an çalışanlar + düello
        item { CanliKart(banka, cevrimici, k?.lisans, y.duello, Modifier.padding(horizontal = 16.dp)) }

        // Sınav tarihi geçmiş ama sonucu girilmemiş ders varsa: sonucu iste
        val sonucBekleyen = depo.hedefler.firstOrNull { h ->
            !h.gecti && h.sinavTarihi != null && h.sinavTarihi.isBefore(LocalDate.now()) && h.ders !in depo.sonuclar
        }
        if (sonucBekleyen != null) {
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFBEFD7)).clickable(onClick = y.sonuclar).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📝", fontSize = 26.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("${banka.ders(sonucBekleyen.ders)?.kisaAd ?: sonucBekleyen.ders} Sınavın Nasıl Geçti?", style = MaterialTheme.typography.titleMedium, color = Color(0xFF7A5A12))
                        Text("Sonucunu Gir · +${Oyun.XP_SONUC} XP", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7A5A12))
                    }
                    Text("›", fontSize = 22.sp, color = Color(0xFF7A5A12))
                }
            }
        }

        // Haftalık lig
        item { LigKarti(lig, siram, k?.siralamaKatil == true, k?.buHaftaXp() ?: 0, Modifier.padding(horizontal = 16.dp)) { y.basariAc(BasariBolumu.SIRALAMA) } }

        // Kısayollar: tek satır, eşit boyut
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                kisayollar.forEach { ky -> KisayolDugmesi(ky, Modifier.weight(1f)) }
            }
        }

        // Topluluk akışı
        item { BolumBasligi("Topluluktan") }
        val satirlar = akisSatirlari(banka, akis, lig, videolar.lastOrNull()?.let { it.first.kisaAd to it.second }, y)
        if (satirlar == null) {
            item { Text("Yükleniyor…", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.padding(horizontal = 20.dp)) }
        } else {
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kart)
                        .border(1.dp, Cizgi, RoundedCornerShape(18.dp)),
                ) {
                    satirlar.forEachIndexed { i, s ->
                        AkisSatirGorunumu(s, son = i == satirlar.lastIndex)
                    }
                    if (k?.akis != true) {
                        Text(
                            if (k?.siralamaKatil == true) "Sen de Akışta Görün › Profil" else "Sen de Akışta Görün › Sıralamaya Katıl",
                            style = MaterialTheme.typography.labelLarge, color = Yesil,
                            modifier = Modifier.fillMaxWidth().clickable { if (k?.siralamaKatil == true) y.sekmeAc(Sekme.PROFIL) else y.basariAc(BasariBolumu.SIRALAMA) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }

        // Tek duyuru
        item { DuyuruKarti(duyuru, Modifier.padding(horizontal = 16.dp)) }
    }
}

// ---------------------------------------------------------------- parçalar

@Composable
private fun CanliKart(banka: SoruBankasi, c: Cevrimici?, lisans: String?, duello: () -> Unit, modifier: Modifier) {
    val toplam = c?.toplam ?: 0
    val ayni = lisans?.let { c?.lisanslar?.get(it) } ?: 0
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(YesilKoyu).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF5BD68A)))
                Text("  Şu An Çevrimiçi", color = Altin, style = MaterialTheme.typography.labelMedium)
            }
            Bosluk(4)
            Text(
                when {
                    c == null -> "Bağlanıyor…"
                    toplam <= 1 -> "Bugün İlk Sen Geldin"
                    else -> "$toplam Kişi Çalışıyor"
                },
                color = Krem, fontFamily = Baslik, fontSize = 22.sp,
            )
            Text(
                when {
                    c == null -> " "
                    ayni > 1 -> "${ayni - 1} Kişi Seninle Aynı Sınava Hazırlanıyor (${banka.lisans(lisans)?.kisaAd.orEmpty()})"
                    else -> "Düelloya Gir, Rakibini Saniyeler İçinde Bul"
                },
                color = KremSoluk, style = MaterialTheme.typography.bodyMedium, maxLines = 2,
            )
        }
        Column(
            Modifier.padding(start = 10.dp).clip(RoundedCornerShape(14.dp)).background(Altin).clickable(onClick = duello)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("⚔️", fontSize = 22.sp)
            Text("Düelloya\nKatıl", color = YesilKoyu, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun LigKarti(lig: List<SiralamaSatiri>?, siram: Long?, katildi: Boolean, xp: Int, modifier: Modifier, tikla: () -> Unit) {
    val bugun = LocalDate.now().dayOfWeek.value
    val kalan = 8 - bugun
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kart).border(1.dp, Cizgi, RoundedCornerShape(20.dp))
            .clickable(onClick = tikla).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🏆 Haftalık Lig", style = MaterialTheme.typography.titleLarge)
                Text(
                    "İlk 3'e ${Oyun.HAFTALIK_ODUL_GUN} Gün Premium · " + if (kalan == 1) "Bugün Son Gün" else "Bitmesine $kalan Gün",
                    style = MaterialTheme.typography.bodyMedium, color = MetinSoluk,
                )
            }
            Text("Tümü ›", style = MaterialTheme.typography.labelLarge, color = Yesil)
        }
        Bosluk(12)
        when {
            lig == null -> Text("Yükleniyor…", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            lig.isEmpty() -> Text("Bu Hafta Lig Yeni Başladı. İlk Sen Ol!", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                listOf(1, 0, 2).forEach { i -> Kursu(lig.getOrNull(i), i) }
            }
        }
        Bosluk(12)
        Text(
            when {
                !katildi -> "Sen de Katıl, Adın Listede Görünsün ›"
                siram != null -> "Sen Bu Hafta $siram. Sıradasın · $xp XP"
                else -> "Bu Hafta Soru Çöz, Listeye Gir"
            },
            style = MaterialTheme.typography.labelLarge, color = Yesil,
        )
    }
}

@Composable
private fun Kursu(s: SiralamaSatiri?, i: Int) {
    val madalya = listOf("🥇", "🥈", "🥉")[i]
    val yukseklik = listOf(52, 38, 28)[i]
    Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(madalya, fontSize = 24.sp)
        Text(s?.takmaAd ?: "–", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (s != null) "${s.deger} XP" else "", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = MetinSoluk)
        Bosluk(4)
        Box(
            Modifier.fillMaxWidth().height(yukseklik.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(if (i == 0) Altin else YesilAcik),
            contentAlignment = Alignment.Center,
        ) { Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = if (i == 0) YesilKoyu else Yesil) }
    }
}

@Composable
private fun KisayolDugmesi(ky: Kisayol, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = ky.tikla).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(YesilAcik), contentAlignment = Alignment.Center) {
                if (ky.resim != null) {
                    androidx.compose.foundation.Image(
                        androidx.compose.ui.res.painterResource(ky.resim),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                } else Text(ky.simge, fontSize = 22.sp, color = Yesil)
            }
            if (ky.rozet > 0) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape).background(YanlisKirmizi).border(2.dp, Zemin, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(if (ky.rozet > 9) "9+" else "${ky.rozet}", color = Krem, fontSize = 10.sp) }
            }
        }
        Bosluk(6)
        Text(ky.ad, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Gerçek olaylar azsa (3'ten az) sistem bilgi kartlarıyla tamamlanır; akış hiç boş görünmez. */
private fun akisSatirlari(
    banka: SoruBankasi,
    olaylar: List<AkisOlayi>?,
    lig: List<SiralamaSatiri>?,
    sonVideo: Pair<String, com.lisanshazirlik.app.data.Konu>?,
    y: Yonlendirici,
): List<AkisSatiri>? {
    if (olaylar == null) return null
    val liste = olaylar.mapNotNull { o ->
        val metin = when (o.tur) {
            "gecti" -> "${banka.ders(o.ders.orEmpty())?.kisaAd ?: "Bir"} Sınavını Geçti"
            "deneme" -> "${kapsamAdi(banka, o.ders)} Denemesinden ${o.deger} Aldı"
            "duello" -> "Düelloda Rakibini Yendi"
            "rozet" -> Oyun.ROZETLER.firstOrNull { it.id == o.ders }?.let { "${it.ad} Rozetini Kazandı" } ?: return@mapNotNull null
            else -> return@mapNotNull null
        }
        val simge = when (o.tur) {
            "gecti" -> "🎓"; "deneme" -> "⏱"; "duello" -> "⚔️"
            else -> Oyun.ROZETLER.firstOrNull { it.id == o.ders }?.simge ?: "🏅"
        }
        AkisSatiri(o.id, simge, o.takmaAd, metin, onceYazisi(o.zaman))
    }.toMutableList()
    if (liste.size < 3) {
        lig?.firstOrNull()?.let { l ->
            liste += AkisSatiri("sis_lider", "🏆", l.takmaAd, "Bu Hafta ${l.deger} XP ile Ligin Lideri", null) { y.basariAc(BasariBolumu.SIRALAMA) }
        }
        sonVideo?.let { (ders, kn) ->
            liste += AkisSatiri("sis_video", "🎬", null, "Yeni Video Yayında: ${kn.ad} ($ders)", null) { y.konuAc(kn.id) }
        }
        liste += AkisSatiri("sis_duello", "⚔️", null, "Düelloda Rakip Bulunamazsa Bot Seninle Yarışır, Beklemek Yok", null, y.duello)
    }
    return liste.take(8)
}

private fun kapsamAdi(banka: SoruBankasi, kapsam: String?): String =
    kapsam?.let { banka.ders(it)?.kisaAd ?: banka.lisans(it)?.kisaAd } ?: "Genel"

private fun onceYazisi(zaman: Long): String {
    val dk = (System.currentTimeMillis() - zaman) / 60_000
    return when {
        dk < 1 -> "Az Önce"
        dk < 60 -> "$dk Dk Önce"
        dk < 60 * 24 -> "${dk / 60} Sa Önce"
        else -> "${dk / (60 * 24)} Gün Önce"
    }
}

@Composable
private fun AkisSatirGorunumu(s: AkisSatiri, son: Boolean) {
    Column {
        Row(
            Modifier.fillMaxWidth().then(if (s.tikla != null) Modifier.clickable(onClick = s.tikla) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(YesilAcik), contentAlignment = Alignment.Center) {
                Text(s.simge, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    buildAnnotatedString {
                        if (s.ad != null) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.ad) }
                            append(" ")
                        }
                        append(s.metin)
                    },
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (s.zaman != null) Text(s.zaman, style = MaterialTheme.typography.labelMedium, color = MetinSoluk)
            }
            if (s.tikla != null) Text("›", fontSize = 20.sp, color = Yesil)
        }
        if (!son) Box(Modifier.padding(start = 66.dp).fillMaxWidth().height(1.dp).background(Cizgi))
    }
}

@Composable
private fun DuyuruKarti(d: Duyuru, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(d.renk).clickable(onClick = d.tikla).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(d.ust.uppercase(java.util.Locale("tr")), style = MaterialTheme.typography.labelMedium, color = Altin)
            Bosluk(4)
            Text(d.baslik, fontFamily = Baslik, fontSize = 19.sp, color = Krem, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Bosluk(2)
            Text(d.alt, style = MaterialTheme.typography.bodyMedium, color = KremSoluk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(d.simge, fontSize = 40.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun BolumBasligi(baslik: String) {
    Text(baslik, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun Rozet(metin: String) {
    Text(
        metin,
        style = MaterialTheme.typography.labelLarge,
        color = Krem,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(YesilKoyu).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
