package com.lisanshazirlik.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.SoruBankasi
import java.time.LocalDate

private val Sari = Color(0xFFC98A1B)
private val SariZemin = Color(0xFFFBEFD7)

// ---------------------------------------------------------------- İstatistik sekmesi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IstatistikSekme(
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    lisans: String?,
    konuAc: (String) -> Unit,
    sonuclarAc: () -> Unit,
    ic: PaddingValues,
    baslikGoster: Boolean = true,
    ust: @Composable () -> Unit = {},
) {
    val il = depo.ilerleme
    val dersler = banka.lisansDersleri(lisans)
    val dersSorulari = dersler.flatMap { banka.dersSorulari(it.kod) }
    val cozulen = dersSorulari.count { it.id in il }
    val dogru = dersSorulari.count { il[it.id] == 1 }
    val denemeler = depo.denemeler.filter { !it.kapsam.startsWith("konu:") }

    Column(Modifier.fillMaxSize()) {
        if (baslikGoster) SekmeBasligi("İstatistik")
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = ic.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ust()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Sayi(Modifier.weight(1f), "Çözülen", "$cozulen")
                Sayi(Modifier.weight(1f), "Doğru oranı", if (cozulen == 0) "–" else "%${yuzde(dogru, cozulen)}")
                Sayi(Modifier.weight(1f), "Deneme", "${denemeler.size}")
            }

            KartKutu(tikla = sonuclarAc) {
                Text("Sertifikalarım", style = MaterialTheme.typography.titleMedium)
                Bosluk(4)
                val girilen = dersler.count { it.kod in depo.sonuclar }
                Text(
                    if (girilen == 0) "Girdiğin sınavların puanlarını kaydet, lisans durumunu takip et."
                    else "${dersler.size} dersten $girilen sonuç girildi · ${dersler.count { (depo.sonuclar[it.kod]?.puan ?: 0) >= 50 }} ders geçildi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinSoluk,
                )
            }

            KartKutu {
                Text("Son 14 gün", style = MaterialTheme.typography.titleMedium)
                Text("Günlük çözülen soru; koyu kısım doğrular.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                Bosluk(12)
                GunlukGrafik(depo)
            }

            KartKutu {
                Text("Deneme puanların", style = MaterialTheme.typography.titleMedium)
                Bosluk(4)
                if (denemeler.isEmpty()) {
                    Text("Deneme çözdükçe puanlarının gelişimi burada görünür.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                } else {
                    Text("Kesikli çizgiler geçme notu 50 ve ortalama 60.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    Bosluk(12)
                    PuanGrafigi(denemeler.take(12).reversed().map { it.ortalama })
                }
            }

            KartKutu {
                Text("Konu haritası", style = MaterialTheme.typography.titleMedium)
                Bosluk(4)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Lejant(DogruZemin, "%75+")
                    Lejant(SariZemin, "%50–74")
                    Lejant(YanlisZemin, "%50 altı")
                    Lejant(Zemin, "Başlanmadı")
                }
                dersler.forEach { d ->
                    Bosluk(14)
                    Text(d.kisaAd, style = MaterialTheme.typography.labelLarge)
                    Bosluk(6)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        d.konular.forEach { k ->
                            val s = banka.konuSorulari(k.id).filter { it.id in il }
                            val oran = if (s.isEmpty()) null else yuzde(s.count { il[it.id] == 1 }, s.size)
                            val (zemin, yazi) = when {
                                oran == null -> Zemin to MetinSoluk
                                oran >= 75 -> DogruZemin to DogruYesil
                                oran >= 50 -> SariZemin to Sari
                                else -> YanlisZemin to YanlisKirmizi
                            }
                            val sekil = RoundedCornerShape(8.dp)
                            Text(
                                k.ad + (oran?.let { " %$it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                color = yazi,
                                modifier = Modifier
                                    .clip(sekil)
                                    .background(zemin)
                                    .border(1.dp, Cizgi, sekil)
                                    .clickableIf(true) { konuAc(k.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sayi(modifier: Modifier, etiket: String, deger: String) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Kart).border(1.dp, Cizgi, RoundedCornerShape(12.dp)).padding(12.dp),
    ) {
        Text(etiket, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = MetinSoluk)
        Text(deger, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Lejant(renk: Color, yazi: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(renk).border(1.dp, Cizgi, RoundedCornerShape(3.dp)))
        Text(yazi, style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, color = MetinSoluk, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun GunlukGrafik(depo: KullaniciDeposu) {
    val bugun = LocalDate.now()
    val gunler = (13 downTo 0).map { bugun.minusDays(it.toLong()) }
    val kayit = depo.gunler.associateBy { it.tarih }
    val enFazla = maxOf(10, gunler.maxOf { kayit[it]?.sayi ?: 0 })
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val aralik = size.width / gunler.size
        val genislik = aralik * 0.62f
        gunler.forEachIndexed { i, g ->
            val k = kayit[g]
            val x = i * aralik + (aralik - genislik) / 2
            val toplamH = (k?.sayi ?: 0) / enFazla.toFloat() * size.height
            val dogruH = (k?.dogru ?: 0) / enFazla.toFloat() * size.height
            drawRoundRect(Cizgi, Offset(x, size.height - 3f), Size(genislik, 3f), CornerRadius(2f))
            if (toplamH > 0f) {
                drawRoundRect(YesilAcik, Offset(x, size.height - toplamH), Size(genislik, toplamH), CornerRadius(6f))
                drawRoundRect(Yesil, Offset(x, size.height - dogruH), Size(genislik, dogruH), CornerRadius(6f))
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        gunler.forEachIndexed { i, g ->
            Text(
                if (i % 2 == 1 || i == gunler.lastIndex) "${g.dayOfMonth}" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = if (g == bugun) Yesil else MetinSoluk,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PuanGrafigi(puanlar: List<Int>) {
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val h = size.height
        fun y(p: Int) = h - p / 100f * h
        val kesik = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        drawLine(Cizgi, Offset(0f, y(50)), Offset(size.width, y(50)), 2f, pathEffect = kesik)
        drawLine(Altin, Offset(0f, y(60)), Offset(size.width, y(60)), 2f, pathEffect = kesik)
        val adim = if (puanlar.size > 1) size.width / (puanlar.size - 1) else 0f
        val noktalar = puanlar.mapIndexed { i, p -> Offset(if (puanlar.size > 1) i * adim else size.width / 2, y(p)) }
        noktalar.zipWithNext { a, b -> drawLine(Yesil, a, b, 5f) }
        noktalar.forEachIndexed { i, n ->
            drawCircle(if (puanlar[i] >= 60) DogruYesil else YanlisKirmizi, 9f, n)
            drawCircle(Kart, 4f, n)
        }
        drawRect(Cizgi, Offset.Zero, size, style = Stroke(1f))
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("İlk", style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, color = MetinSoluk)
        Text("Son: ${puanlar.last()}", style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, color = MetinSoluk)
    }
}

// ---------------------------------------------------------------- Sınav sonuçları

/** Geniş kapsamlı mevzuatı (1002) geçen, dar kapsamlıyı (1001) da geçmiş sayılır. */
fun etkinSonuc(sonuclar: Map<String, com.lisanshazirlik.app.data.SinavSonucu>, kod: String): com.lisanshazirlik.app.data.SinavSonucu? {
    val kendi = sonuclar[kod]
    if (kod != "1001") return kendi
    val genis = sonuclar["1002"]?.takeIf { it.puan >= 50 } ?: return kendi
    return if (kendi == null || kendi.puan < genis.puan) genis.copy(ders = "1001") else kendi
}

enum class LisansDurumu { ALINDI, DEVAM, BASLANMADI }

fun lisansDurumu(banka: SoruBankasi, sonuclar: Map<String, com.lisanshazirlik.app.data.SinavSonucu>, lisans: String): Triple<LisansDurumu, Int, Int> {
    val dersler = banka.lisans(lisans)?.dersler.orEmpty()
    val puanlar = dersler.mapNotNull { etkinSonuc(sonuclar, it)?.puan }
    val gecilen = puanlar.count { it >= 50 }
    val tamam = dersler.isNotEmpty() && puanlar.size == dersler.size && gecilen == dersler.size && puanlar.average() >= 60
    val durum = when {
        tamam -> LisansDurumu.ALINDI
        puanlar.isNotEmpty() -> LisansDurumu.DEVAM
        else -> LisansDurumu.BASLANMADI
    }
    return Triple(durum, gecilen, dersler.size)
}

/** Sertifika cüzdanı: bütün lisanslar ve durumları; dokununca o lisansın puanları. */
@Composable
fun SonuclarEkrani(banka: SoruBankasi, depo: KullaniciDeposu, lisans: String?, geri: () -> Unit) {
    var acik by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    acik?.let { kod ->
        androidx.activity.compose.BackHandler { acik = null }
        LisansSonucDetay(banka, depo, kod) { acik = null }
        return
    }
    val sonuclar = depo.sonuclar
    val sirali = banka.lisanslar.sortedByDescending { it.kod == lisans }
    val alinan = banka.lisanslar.count { lisansDurumu(banka, sonuclar, it.kod).first == LisansDurumu.ALINDI }
    Scaffold(topBar = { UstCubuk("Sertifikalarım", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Yesil).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(com.lisanshazirlik.app.R.drawable.ic_sertifika),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                )
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Sertifika cüzdanın", fontFamily = Baslik, fontSize = 20.sp, color = Krem)
                    Text("${banka.lisanslar.size} lisanstan $alinan tanesi alındı", style = MaterialTheme.typography.bodyMedium, color = KremSoluk)
                }
                Text("$alinan", fontFamily = Baslik, fontSize = 34.sp, color = Altin)
            }
            Text(
                "Lisansına dokun, sınav puanlarını gir. Ortak dersler bütün lisanslarda otomatik sayılır.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            sirali.forEach { l ->
                val (durum, gecilen, toplam) = lisansDurumu(banka, sonuclar, l.kod)
                val i = banka.lisanslar.indexOf(l)
                val renk = SEVIYE_RENKLERI[i % SEVIYE_RENKLERI.size]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Kart)
                        .border(if (durum == LisansDurumu.ALINDI) 2.dp else 1.dp, if (durum == LisansDurumu.ALINDI) Altin else Cizgi, RoundedCornerShape(16.dp))
                        .clickable { acik = l.kod }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(renk), contentAlignment = Alignment.Center) {
                        Text(if (durum == LisansDurumu.ALINDI) "🏅" else "📜", fontSize = 22.sp)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(l.kisaAd, style = MaterialTheme.typography.titleMedium)
                        Bosluk(4)
                        Ilerleme(if (toplam == 0) 0f else gecilen / toplam.toFloat())
                        Bosluk(2)
                        Text("$gecilen/$toplam ders geçildi", fontSize = 12.sp, color = MetinSoluk)
                    }
                    Text(
                        when (durum) {
                            LisansDurumu.ALINDI -> "Alındı"
                            LisansDurumu.DEVAM -> "Devam"
                            LisansDurumu.BASLANMADI -> "+ Puan gir"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when (durum) {
                            LisansDurumu.ALINDI -> YesilKoyu
                            LisansDurumu.DEVAM -> Krem
                            LisansDurumu.BASLANMADI -> Yesil
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                when (durum) {
                                    LisansDurumu.ALINDI -> Altin
                                    LisansDurumu.DEVAM -> Sari
                                    LisansDurumu.BASLANMADI -> YesilAcik
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Text("🔒 Puanların sadece senin hesabında durur.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun LisansSonucDetay(banka: SoruBankasi, depo: KullaniciDeposu, lisans: String?, geri: () -> Unit) {
    val dersler = banka.lisansDersleri(lisans)
    val l = banka.lisans(lisans)
    var duzenlenen by remember { mutableStateOf<String?>(null) }
    val sonuclar = depo.sonuclar
    val girilen = dersler.mapNotNull { etkinSonuc(sonuclar, it.kod) }
    val hepsiGirildi = girilen.size == dersler.size && dersler.isNotEmpty()
    val ortalama = if (girilen.isEmpty()) 0 else Math.round(girilen.map { it.puan }.average()).toInt()
    val hepsiGecti = hepsiGirildi && girilen.all { it.puan >= 50 }
    val lisansTamam = hepsiGecti && ortalama >= 60

    val gecilen = girilen.count { it.puan >= 50 }
    Scaffold(topBar = { UstCubuk(banka.lisans(lisans)?.kisaAd ?: "Sertifikalarım", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Sertifika kartı
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Yesil).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(com.lisanshazirlik.app.R.drawable.ic_sertifika),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
                Bosluk(6)
                Text(l?.kisaAd ?: "Lisans", fontFamily = Baslik, fontSize = 22.sp, color = Krem)
                Text(
                    when {
                        lisansTamam -> "Lisans koşulunu sağladın 🎉"
                        hepsiGecti -> "Ortalamanı 60'a çıkarman gerekiyor"
                        else -> "Lisans yolundaki durumun"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lisansTamam) Altin else KremSoluk,
                )
                Bosluk(14)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SertifikaKutusu(Modifier.weight(1f), "$gecilen/${dersler.size}", "geçilen ders")
                    SertifikaKutusu(Modifier.weight(1f), if (girilen.isEmpty()) "–" else "$ortalama", "ortalama")
                }
            }

            // Koşullar
            KartKutu {
                Text("Lisans için iki koşul", style = MaterialTheme.typography.titleMedium)
                Bosluk(8)
                KosulSatiri(hepsiGecti, "Her dersten en az 50")
                Bosluk(6)
                KosulSatiri(hepsiGirildi && ortalama >= 60, "Ortalama en az 60")
            }

            Text("Sınav puanların", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            dersler.forEachIndexed { i, d ->
                val s2 = etkinSonuc(sonuclar, d.kod)
                val renk = dersRengi(d.kod)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Kart)
                        .border(1.dp, Cizgi, RoundedCornerShape(16.dp))
                        .clickable { duzenlenen = d.kod }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(renk), contentAlignment = Alignment.Center) {
                        Text(d.kod, style = MaterialTheme.typography.labelLarge, color = Krem)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(d.kisaAd, style = MaterialTheme.typography.titleMedium, maxLines = 2, lineHeight = 20.sp)
                        if (s2 != null) {
                            Text(
                                if (s2.puan >= 50) "✓ Geçtin" else "✗ Kaldın",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (s2.puan >= 50) DogruYesil else YanlisKirmizi,
                            )
                        }
                    }
                    if (s2 == null) {
                        Text(
                            "+ Puan gir",
                            style = MaterialTheme.typography.labelLarge,
                            color = Krem,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Yesil).padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(if (s2.puan >= 50) DogruYesil else YanlisKirmizi),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${s2.puan}", style = MaterialTheme.typography.titleMedium, color = Krem)
                            }
                            Text("✎ düzenle", fontSize = 11.sp, color = MetinSoluk)
                        }
                    }
                }
            }
            Text(
                "🔒 Puanların sadece senin hesabında durur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    duzenlenen?.let { kod ->
        SonucPenceresi(
            dersAdi = banka.ders(kod)?.kisaAd ?: kod,
            mevcutPuan = sonuclar[kod]?.puan,
            mevcutTarih = sonuclar[kod]?.tarih,
            kapat = { duzenlenen = null },
            kaydet = { p, t -> depo.sonucKaydet(kod, p, t); duzenlenen = null },
            sil = if (sonuclar[kod] != null) ({ depo.sonucSil(kod); duzenlenen = null }) else null,
        )
    }
}

@Composable
private fun SonucPenceresi(
    dersAdi: String,
    mevcutPuan: Int?,
    mevcutTarih: LocalDate?,
    kapat: () -> Unit,
    kaydet: (Int, LocalDate?) -> Unit,
    sil: (() -> Unit)?,
) {
    var puan by remember { mutableStateOf(mevcutPuan?.toString().orEmpty()) }
    var tarih by remember { mutableStateOf(mevcutTarih) }
    var takvim by remember { mutableStateOf(false) }
    val sayi = puan.toIntOrNull()
    val gecerli = sayi != null && sayi in 0..100

    AlertDialog(
        onDismissRequest = kapat,
        title = { Text(dersAdi) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = puan,
                    onValueChange = { yeni -> puan = yeni.filter { it.isDigit() }.take(3) },
                    label = { Text("Aldığın puan (0–100)") },
                    singleLine = true,
                    isError = puan.isNotEmpty() && !gecerli,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (puan.isNotEmpty() && !gecerli) Text("Puan 0 ile 100 arasında olmalı.", color = YanlisKirmizi, fontSize = 13.sp)
                OutlinedButton(onClick = { takvim = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(tarih?.let { "Sınav tarihi: ${uzunTarih(it)}" } ?: "Sınav tarihini seç (isteğe bağlı)")
                }
                if (sil != null) {
                    TextButton(onClick = sil) { Text("Sonucu kaldır", color = YanlisKirmizi) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { sayi?.let { kaydet(it, tarih) } }, enabled = gecerli) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç") } },
    )
    if (takvim) {
        TarihPenceresi(tarih, kapat = { takvim = false }, gecmis = true, baslik = "Sınava girdiğin tarih") {
            tarih = it; takvim = false
        }
    }
}

@Composable
private fun SertifikaKutusu(modifier: Modifier, deger: String, etiket: String) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(YesilKoyu).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(deger, fontFamily = Baslik, fontSize = 24.sp, color = Altin)
        Text(etiket, fontSize = 12.sp, color = KremSoluk)
    }
}

@Composable
private fun KosulSatiri(tamam: Boolean, metin: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(if (tamam) DogruYesil else Zemin)
                .border(1.dp, if (tamam) DogruYesil else Cizgi, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (tamam) Text("✓", color = Krem, fontSize = 13.sp)
        }
        Text(metin, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 10.dp), color = if (tamam) DogruYesil else Metin)
    }
}
