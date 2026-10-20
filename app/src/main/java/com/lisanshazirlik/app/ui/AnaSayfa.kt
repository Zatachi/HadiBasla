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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Odak
import com.lisanshazirlik.app.data.Oyun
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.Tekrar
import java.time.LocalDate

private data class Duyuru(val ust: String, val baslik: String, val alt: String, val simge: String, val renk: Color, val tikla: () -> Unit)
private data class Kisayol(val simge: String, val ad: String, val resim: Int? = null, val rozet: Int = 0, val tikla: () -> Unit)

/**
 * Ana sayfa tek ekrana sığar: bugünkü plan, kısayollar ve tek bir duyuru.
 * Öğrenme yolu Eğitim sekmesinde, sıralama Başarı sekmesinde, videolar Shorts'tadır.
 */
@Composable
fun AnaSekme(banka: SoruBankasi, depo: KullaniciDeposu, uid: String?, y: Yonlendirici, ic: PaddingValues) {
    val k = depo.kullanici
    val odak = Odak.ders(banka, depo)
    val dersler = banka.lisansDersleri(k?.lisans)
    val videolar = dersler.flatMap { d -> d.konular.filter { it.video.isNotBlank() }.map { d to it } }
    val bekleyenTekrar = Tekrar.bekleyenler(banka, depo, listOfNotNull(odak?.kod)).size

    // Tek duyuru: her gün sırayla biri gösterilir, kaydırmalı banner yok.
    val duyurular = buildList {
        videolar.lastOrNull()?.let { (d, kn) ->
            add(Duyuru("Yeni Video", kn.ad, "${d.kisaAd} · Sesli Animasyonlu Anlatım", "🎬", YesilKoyu) { y.konuAc(kn.id) })
        }
        add(Duyuru("Haftalık Lig", "İlk 3'e ${Oyun.HAFTALIK_ODUL_GUN} Gün Premium", "Soru Çöz, XP Topla, Zirveye Çık", "🏆", Color(0xFF7A5A12)) { y.basariAc(BasariBolumu.SIRALAMA) })
        add(Duyuru("Davet Et", "3 Arkadaşına 7 Gün Premium", "Davet Kodunu Paylaş, Birlikte Çalışın", "🎁", Color(0xFF7A2E3A)) { y.basariAc(BasariBolumu.DAVET) })
    }
    val duyuru = duyurular[LocalDate.now().dayOfYear % duyurular.size]

    val kisayollar = listOf(
        Kisayol("▶", "Shorts", tikla = y.shorts),
        Kisayol("🔁", "5 Dakika", rozet = bekleyenTekrar, tikla = y.gunun5),
        Kisayol("⚔️", "Düello", tikla = y.duello),
        Kisayol("🎧", "Kulaklık", tikla = y.kulaklik),
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

        // Kahraman: bugünkü plan. Odak ders yoksa önce hedef seçtirilir.
        item {
            if (odak != null) {
                BugunkuPlanKarti(banka, depo, y, Modifier.padding(horizontal = 16.dp))
            } else {
                Column(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(YesilKoyu)
                        .clickable { y.hedefAc(null) }.padding(20.dp),
                ) {
                    Text("Bugünkü Planın", color = Altin, style = MaterialTheme.typography.labelMedium)
                    Bosluk(4)
                    Text("Hangi Sınava Hazırlanıyorsun?", color = Krem, fontFamily = Baslik, fontSize = 20.sp)
                    Text("Dersini ve Sınav Tarihini Seç, Sana Günlük Plan Hazırlayalım", color = KremSoluk, style = MaterialTheme.typography.bodyMedium)
                    Bosluk(14)
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Altin),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) { Text("Hedefimi Seç", color = YesilKoyu, style = MaterialTheme.typography.labelLarge) }
                }
            }
        }

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

        // Kısayollar: tek satır, eşit boyut
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                kisayollar.forEach { ky ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = ky.tikla).padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Box(
                                Modifier.size(54.dp).clip(CircleShape).background(YesilAcik),
                                contentAlignment = Alignment.Center,
                            ) {
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
            }
        }

        // Tek duyuru
        item {
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(duyuru.renk)
                    .clickable(onClick = duyuru.tikla)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(duyuru.ust.uppercase(java.util.Locale("tr")), style = MaterialTheme.typography.labelMedium, color = Altin)
                    Bosluk(4)
                    Text(duyuru.baslik, fontFamily = Baslik, fontSize = 19.sp, color = Krem, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Bosluk(2)
                    Text(duyuru.alt, style = MaterialTheme.typography.bodyMedium, color = KremSoluk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(duyuru.simge, fontSize = 40.sp, modifier = Modifier.padding(start = 8.dp))
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
