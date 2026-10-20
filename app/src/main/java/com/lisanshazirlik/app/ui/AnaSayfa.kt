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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Oyun
import com.lisanshazirlik.app.data.SiralamaSatiri
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.Topluluk
import kotlinx.coroutines.delay

private data class Banner(val ust: String, val baslik: String, val alt: String, val simge: String, val renk: Color, val tikla: () -> Unit)
private data class Kisayol(val simge: String, val ad: String, val resim: Int? = null, val tikla: () -> Unit)

/** Ana sayfa: uygulamanın vitrini. Öğrenme yolu Eğitim sekmesindedir. */
@Composable
fun AnaSekme(banka: SoruBankasi, depo: KullaniciDeposu, uid: String?, y: Yonlendirici, ic: PaddingValues) {
    val k = depo.kullanici
    val dersler = banka.lisansDersleri(k?.lisans)
    val yanlislar = depo.yanlislar().mapNotNull { banka.bul(it) }
    val videolar = dersler.flatMap { d -> d.konular.filter { it.video.isNotBlank() }.map { d to it } }
    val enYakin = depo.aktifHedefler().filter { (it.kalanGun() ?: -1L) >= 0 }.minByOrNull { it.kalanGun()!! }
    val hedefSayi = gunlukHedef(banka, depo)
    val bugun = depo.bugunCozulen

    val bannerlar = buildList {
        videolar.lastOrNull()?.let { (d, kn) ->
            add(Banner("YENİ VİDEO", kn.ad, "${d.kisaAd} · sesli animasyonlu anlatım", "🎬", YesilKoyu) { y.konuAc(kn.id) })
        }
        add(Banner("HAFTALIK LİG", "İlk 3'e ${Oyun.HAFTALIK_ODUL_GUN} gün Premium", "Soru çöz, XP topla, zirveye çık", "🏆", Color(0xFF7A5A12)) { y.basariAc(BasariBolumu.SIRALAMA) })
        add(Banner("DAVET ET", "3 arkadaşına 7 gün Premium", "Davet kodunu paylaş, birlikte çalışın", "🎁", Color(0xFF7A2E3A)) { y.basariAc(BasariBolumu.DAVET) })
        add(Banner("DENEME", "Gerçek sınav gibi dene", "25 soru · 45 dakika · anında sonuç", "⏱", Color(0xFF24476B)) { y.sekmeAc(Sekme.DENEME) })
    }

    val kisayollar = listOf(
        Kisayol("", "Eğitim", com.lisanshazirlik.app.R.drawable.ic_egitim) { y.sekmeAc(Sekme.DERSLER) },
        Kisayol("", "Deneme", com.lisanshazirlik.app.R.drawable.ic_deneme) { y.sekmeAc(Sekme.DENEME) },
        Kisayol("❌", "Yanlışlarım") {
            if (yanlislar.isNotEmpty()) y.coz("Yanlışlarım", yanlislar.shuffled(), null, "yanlislar") else y.sekmeAc(Sekme.DERSLER)
        },
        Kisayol("🏆", "Sıralama") { y.basariAc(BasariBolumu.SIRALAMA) },
        Kisayol("", "Sertifikalarım", com.lisanshazirlik.app.R.drawable.ic_sertifika, y.sonuclar),
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

        // Bannerlar
        item {
            val pager = rememberPagerState { bannerlar.size }
            LaunchedEffect(bannerlar.size) {
                while (true) {
                    delay(4500)
                    pager.animateScrollToPage((pager.currentPage + 1) % bannerlar.size)
                }
            }
            Column {
                HorizontalPager(
                    pager,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 10.dp,
                ) { i ->
                    val b = bannerlar[i]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(b.renk)
                            .clickable(onClick = b.tikla)
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(b.ust, style = MaterialTheme.typography.labelMedium, color = Altin)
                            Bosluk(4)
                            Text(b.baslik, fontFamily = Baslik, fontSize = 20.sp, color = Krem, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Bosluk(4)
                            Text(b.alt, style = MaterialTheme.typography.bodyMedium, color = KremSoluk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(b.simge, fontSize = 44.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Bosluk(8)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(bannerlar.size) { i ->
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (i == pager.currentPage) 18.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(if (i == pager.currentPage) Yesil else Cizgi)
                        )
                    }
                }
            }
        }

        // Hızlı erişim
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(kisayollar, key = { it.ad }) { ky ->
                    Column(
                        Modifier.width(76.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = ky.tikla).padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(YesilAcik), contentAlignment = Alignment.Center) {
                            if (ky.resim != null) {
                                androidx.compose.foundation.Image(
                                    androidx.compose.ui.res.painterResource(ky.resim),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                )
                            } else Text(ky.simge, fontSize = 24.sp)
                        }
                        Bosluk(6)
                        Text(ky.ad, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }

        // Sınav / günlük hedef şeridi
        item {
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Kart)
                    .clickable { y.hedefAc(enYakin?.ders) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        enYakin == null -> "📅  Sınav tarihini ekle"
                        enYakin.kalanGun() == 0L -> "📅  ${banka.ders(enYakin.ders)?.kisaAd} sınavı bugün!"
                        else -> "📅  ${banka.ders(enYakin.ders)?.kisaAd} sınavına ${enYakin.kalanGun()} gün"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Bugün $bugun/$hedefSayi", style = MaterialTheme.typography.labelLarge, color = if (bugun >= hedefSayi) DogruYesil else Yesil)
            }
        }

        // Haftanın liderleri
        item { Liderler(banka, depo, uid, y) }

        // Videolar
        if (videolar.isNotEmpty()) {
            item { BolumBasligi("Video anlatımlar", "Tümü") { y.sekmeAc(Sekme.DERSLER) } }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(videolar.reversed(), key = { "v" + it.second.id }) { (d, kn) ->
                        Column(
                            Modifier.width(200.dp).clip(RoundedCornerShape(14.dp)).background(Kart).clickable { y.konuAc(kn.id) },
                        ) {
                            Box(Modifier.fillMaxWidth().height(100.dp).background(YesilKoyu), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(Altin), contentAlignment = Alignment.Center) {
                                    Text("▶", color = YesilKoyu, fontSize = 20.sp)
                                }
                            }
                            Column(Modifier.padding(12.dp)) {
                                Text(kn.ad, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(d.kisaAd, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

    }
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

@Composable
private fun BolumBasligi(baslik: String, eylem: String? = null, tikla: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(baslik, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (eylem != null) Text(eylem, style = MaterialTheme.typography.labelLarge, color = Yesil, modifier = Modifier.clickable(onClick = tikla))
    }
}

@Composable
private fun Liderler(banka: SoruBankasi, depo: KullaniciDeposu, uid: String?, y: Yonlendirici) {
    val k = depo.kullanici
    val tablo = remember(k?.lisans) { Topluluk.tablolar(banka, k?.lisans).first() }
    var ilk by remember { mutableStateOf<List<SiralamaSatiri>?>(null) }
    var siram by remember { mutableStateOf<Long?>(null) }
    var hata by remember { mutableStateOf(false) }
    LaunchedEffect(tablo.id, k?.siralamaKatil) {
        try {
            ilk = Topluluk.liste(tablo, 3)
            if (uid != null && k?.siralamaKatil == true) {
                Topluluk.benimDegerim(uid, tablo)?.let { siram = Topluluk.sira(tablo, it) }
            }
        } catch (e: Exception) {
            hata = true
        }
    }

    Column {
        BolumBasligi("🏆 Haftanın liderleri", "Tümü") { y.basariAc(BasariBolumu.SIRALAMA) }
        Bosluk(8)
        KartKutu(Modifier.padding(horizontal = 16.dp), tikla = { y.basariAc(BasariBolumu.SIRALAMA) }) {
            val liste = ilk
            when {
                hata -> Text("Sıralama şu an yüklenemedi.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                liste == null -> Text("Yükleniyor…", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                liste.isEmpty() -> Text("Bu hafta lig yeni başladı. İlk sen ol!", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    // Kürsü sırası: 2 · 1 · 3
                    listOf(1, 0, 2).forEach { i ->
                        val s = liste.getOrNull(i)
                        Kursu(s, i)
                    }
                }
            }
            Bosluk(12)
            Text(
                when {
                    k?.siralamaKatil != true -> "Sen de katıl, adın listede görünsün →"
                    siram != null -> "Sen bu hafta $siram. sıradasın · ${k?.buHaftaXp() ?: 0} XP"
                    else -> "Bu hafta soru çöz, listeye gir."
                },
                style = MaterialTheme.typography.labelLarge,
                color = Yesil,
            )
        }
    }
}

@Composable
private fun Kursu(s: SiralamaSatiri?, i: Int) {
    val madalya = listOf("🥇", "🥈", "🥉")[i]
    val yukseklik = listOf(56, 40, 30)[i]
    Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(madalya, fontSize = 26.sp)
        Text(s?.takmaAd ?: "–", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (s != null) "${s.deger} XP" else "", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = MetinSoluk)
        Bosluk(4)
        Box(
            Modifier
                .fillMaxWidth()
                .height(yukseklik.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(if (i == 0) Altin else YesilAcik),
            contentAlignment = Alignment.Center,
        ) {
            Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = if (i == 0) YesilKoyu else Yesil)
        }
    }
}
