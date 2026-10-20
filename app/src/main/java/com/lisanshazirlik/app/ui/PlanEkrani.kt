package com.lisanshazirlik.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.Hazirlik
import com.lisanshazirlik.app.data.HazirlikSkoru
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Odak
import com.lisanshazirlik.app.data.Plan
import com.lisanshazirlik.app.data.PlanGorevi
import com.lisanshazirlik.app.data.PlanTuru
import com.lisanshazirlik.app.data.SoruBankasi

fun skorRengi(skor: Int): Color = when {
    skor >= 75 -> DogruYesil
    skor >= 60 -> Color(0xFF3E8E5E)
    skor >= 40 -> Color(0xFFC98A1B)
    else -> YanlisKirmizi
}

/** Yarım daire değil tam halka: ortasında skor. */
@Composable
fun SkorHalkasi(skor: Int, boyut: Dp, kalinlik: Dp = 6.dp, zemin: Color = Cizgi, yaziRengi: Color = Metin) {
    Box(Modifier.size(boyut), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(boyut)) {
            val k = kalinlik.toPx()
            val olcu = Size(size.width - k, size.height - k)
            val bas = Offset(k / 2, k / 2)
            drawArc(zemin, -90f, 360f, false, bas, olcu, style = Stroke(k))
            drawArc(skorRengi(skor), -90f, 360f * skor / 100f, false, bas, olcu, style = Stroke(k, cap = StrokeCap.Round))
        }
        Text("$skor", fontFamily = Baslik, fontSize = (boyut.value * 0.32f).sp, color = yaziRengi)
    }
}

/** Ana sayfanın kalbi: bugünkü görevler ve odak dersin hazırlık skoru. */
@Composable
fun BugunkuPlanKarti(banka: SoruBankasi, depo: KullaniciDeposu, y: Yonlendirici, modifier: Modifier = Modifier) {
    val ders = Odak.ders(banka, depo) ?: return
    LaunchedEffect(depo.kullanici?.planGun) { Plan.gunuBaslat(banka, depo) }
    val gorevler = Plan.bugun(banka, depo, gunlukHedef(banka, depo))
    val skor = Hazirlik.ders(banka, depo, ders.kod)
    val tamam = gorevler.count { it.tamam }
    val siradaki = gorevler.firstOrNull { !it.tamam }

    fun ac(g: PlanGorevi) = when (g.tur) {
        PlanTuru.ADIM -> g.adimId?.let { y.paketAdimAc(ders.kod, it) } ?: y.paketAc(ders.kod)
        PlanTuru.TEKRAR -> y.gunun5()
        PlanTuru.SORU -> y.coz("${ders.kisaAd} · Günlük Sorular", calismaSirasi(banka.dersSorulari(ders.kod), depo).take(gunlukHedef(banka, depo)), null, "plan")
        PlanTuru.DENEME -> y.dersDenemesi(ders.kod)
        PlanTuru.SHORT -> y.shorts()
    }

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kart).border(1.dp, Cizgi, RoundedCornerShape(20.dp)),
    ) {
        // Başlık: plan ilerlemesi + hazırlık skoru
        Row(
            Modifier.fillMaxWidth().background(YesilKoyu).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("BUGÜNKÜ PLANIN", color = Altin, style = MaterialTheme.typography.labelMedium)
                Text(ders.kisaAd, color = Krem, fontFamily = Baslik, fontSize = 20.sp, maxLines = 1)
                Text(
                    if (gorevler.isNotEmpty() && tamam == gorevler.size) "🎉 Bugünkü Planı Tamamladın" else "$tamam/${gorevler.size} Görev Tamamlandı",
                    color = KremSoluk, style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (skor != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { y.hazirlik() }) {
                    SkorHalkasi(skor.toplam, 58.dp, zemin = Color(0x33FFFFFF), yaziRengi = Krem)
                    Text("Hazırlık ›", color = KremSoluk, fontSize = 11.sp)
                }
            }
        }
        // Görevler
        gorevler.forEach { g ->
            Row(
                Modifier.fillMaxWidth().clickable { ac(g) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(if (g.tamam) DogruYesil else Zemin)
                        .border(1.dp, if (g.tamam) DogruYesil else Cizgi, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(if (g.tamam) "✓" else g.simge, color = if (g.tamam) Krem else Metin, fontSize = 13.sp) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(g.baslik, style = MaterialTheme.typography.titleMedium, color = if (g.tamam) MetinSoluk else Metin)
                    Text(g.alt, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, maxLines = 1)
                }
                if (!g.tamam) Text("›", fontSize = 22.sp, color = Yesil)
            }
        }
        if (siradaki != null) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Yesil).clickable { ac(siradaki) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) { Text("▶  Sıradakine Başla: ${siradaki.baslik}", color = Krem, style = MaterialTheme.typography.labelLarge) }
        } else {
            Bosluk(8)
        }
    }
}

/** Hazırlık ayrıntısı: lisansın her dersi için skor, bileşenleri ve öneri. */
@Composable
fun HazirlikEkrani(banka: SoruBankasi, depo: KullaniciDeposu, dersAc: (String) -> Unit, geri: () -> Unit) {
    val lisans = banka.lisans(depo.kullanici?.lisans)
    val (ort, skorlar) = Hazirlik.lisans(banka, depo, depo.kullanici?.lisans)
    val hazir = skorlar.isNotEmpty() && skorlar.all { it.toplam >= 60 } && ort >= 70
    Scaffold(topBar = { UstCubuk("Hazırlık Skorun", geri) }, containerColor = Zemin) { ic ->
        Column(Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().background(Yesil).padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SkorHalkasi(ort, 120.dp, 10.dp, Color(0x33FFFFFF), Krem)
                Bosluk(8)
                Text(lisans?.kisaAd ?: "Lisans", fontFamily = Baslik, fontSize = 22.sp, color = Krem)
                Text(
                    if (hazir) "Lisans Sınavına Hazırsın" else "Her Ders En Az 60, Ortalama En Az 70 Olunca Hazırsın",
                    color = if (hazir) Altin else KremSoluk, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (skorlar.isEmpty()) Text("Bu Lisansın Soruları Hazırlanıyor", color = MetinSoluk)
                skorlar.forEach { s -> DersSkorKarti(banka, s) { dersAc(s.ders) } }
                Text(
                    "Skor; Kapsam (%30), Doğruluk (%30), Deneme (%30) ve Hafıza (%10) Üzerinden Hesaplanır. Deneme Çözmeden Skor 70'i Geçmez.",
                    style = MaterialTheme.typography.bodyMedium, color = MetinSoluk,
                )
            }
        }
    }
}

@Composable
private fun DersSkorKarti(banka: SoruBankasi, s: HazirlikSkoru, tikla: () -> Unit) {
    val d = banka.ders(s.ders) ?: return
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kart).border(1.dp, Cizgi, RoundedCornerShape(16.dp))
            .clickable(onClick = tikla).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(dersRengi(d.kod)), contentAlignment = Alignment.Center) {
                Text(d.kod, color = Krem, style = MaterialTheme.typography.labelLarge)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(d.kisaAd, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(s.etiket, color = skorRengi(s.toplam), style = MaterialTheme.typography.labelLarge)
            }
            SkorHalkasi(s.toplam, 52.dp)
        }
        Bilesen("Kapsam", s.kapsam)
        Bilesen("Doğruluk", s.dogruluk)
        Bilesen("Deneme", s.deneme)
        Bilesen("Hafıza", s.hafiza)
        Text("💡 ${s.oneri}", style = MaterialTheme.typography.bodyMedium, color = Yesil)
    }
}

@Composable
private fun Bilesen(ad: String, deger: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(ad, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.size(width = 80.dp, height = 20.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Zemin)) {
            if (deger != null) Box(Modifier.fillMaxWidth(deger / 100f).height(8.dp).background(skorRengi(deger)))
        }
        Text(deger?.let { "%$it" } ?: "–", style = MaterialTheme.typography.labelMedium, color = MetinSoluk, modifier = Modifier.padding(start = 8.dp).size(width = 40.dp, height = 18.dp), textAlign = TextAlign.End)
    }
}
