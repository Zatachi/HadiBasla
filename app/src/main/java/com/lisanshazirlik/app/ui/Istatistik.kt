package com.lisanshazirlik.app.ui

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
                Text("Sınav sonuçlarım", style = MaterialTheme.typography.titleMedium)
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

@Composable
fun SonuclarEkrani(banka: SoruBankasi, depo: KullaniciDeposu, lisans: String?, geri: () -> Unit) {
    val dersler = banka.lisansDersleri(lisans)
    val l = banka.lisans(lisans)
    var duzenlenen by remember { mutableStateOf<String?>(null) }
    val sonuclar = depo.sonuclar
    val girilen = dersler.mapNotNull { sonuclar[it.kod] }
    val hepsiGirildi = girilen.size == dersler.size && dersler.isNotEmpty()
    val ortalama = if (girilen.isEmpty()) 0 else Math.round(girilen.map { it.puan }.average()).toInt()
    val hepsiGecti = hepsiGirildi && girilen.all { it.puan >= 50 }
    val lisansTamam = hepsiGecti && ortalama >= 60

    Scaffold(topBar = { UstCubuk("Sınav sonuçlarım", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(l?.ad ?: "Lisans sınavı", style = MaterialTheme.typography.titleLarge)
            KartKutu {
                when {
                    lisansTamam -> {
                        Text("Lisans koşulunu sağladın 🎉", style = MaterialTheme.typography.titleMedium, color = DogruYesil)
                        Bosluk(4)
                        Text("Bütün derslerden en az 50 ve ortalaman $ortalama. Tebrikler! Lisans başvurunu yapmayı unutma.", style = MaterialTheme.typography.bodyMedium)
                    }
                    hepsiGecti -> {
                        Text("Ortalaman $ortalama", style = MaterialTheme.typography.titleMedium, color = Sari)
                        Bosluk(4)
                        Text("Bütün derslerden geçtin ama lisans için ortalamanın en az 60 olması gerekiyor. Düşük puanlı bir derse yeniden girerek ortalamanı yükseltebilirsin.", style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {
                        Text("${dersler.size} dersten ${girilen.count { it.puan >= 50 }} ders geçildi", style = MaterialTheme.typography.titleMedium)
                        Bosluk(4)
                        Text("Lisans için her dersten en az 50 almalı, ortalaman da en az 60 olmalı.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    }
                }
                if (girilen.isNotEmpty()) {
                    Bosluk(10)
                    Ilerleme(girilen.count { it.puan >= 50 } / dersler.size.toFloat())
                }
            }
            dersler.forEach { d ->
                val s = sonuclar[d.kod]
                KartKutu(tikla = { duzenlenen = d.kod }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.ad, style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    s == null -> "Sonuç girilmedi · dokun ve ekle"
                                    s.tarih != null -> uzunTarih(s.tarih)
                                    else -> "Tarih girilmedi"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetinSoluk,
                            )
                        }
                        if (s != null) {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                                Text("${s.puan}", style = MaterialTheme.typography.headlineSmall, color = if (s.puan >= 50) DogruYesil else YanlisKirmizi)
                                Text(if (s.puan >= 50) "Geçti" else "Kaldı", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = if (s.puan >= 50) DogruYesil else YanlisKirmizi)
                            }
                        }
                    }
                }
            }
            Text(
                "Sonuçların sadece senin hesabında tutulur. Bir dersten 50 ve üzeri girdiğinde o dersin hedefi \"geçildi\" olarak işaretlenir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
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
