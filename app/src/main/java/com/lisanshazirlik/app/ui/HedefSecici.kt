package com.lisanshazirlik.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lisanshazirlik.app.data.Ders
import com.lisanshazirlik.app.data.Hedef
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.SoruBankasi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Ders seçimi ve sınav tarihi. Hem ilk kurulumda hem hedef eklerken kullanılır. */
@Composable
fun HedefSecici(
    dersler: List<Ders>,
    secili: String?,
    dersSec: (String) -> Unit,
    tarih: LocalDate?,
    tarihSec: (LocalDate) -> Unit,
    /** Verilirse üstte lisans çipleri çıkar; bütün lisansların dersleri seçilebilir. */
    banka: SoruBankasi? = null,
    baslangicLisans: String? = null,
    /** Zaten hedefi olan dersler: listede görünür ama seçilemez. */
    hedefli: Set<String> = emptySet(),
) {
    var takvim by remember { mutableStateOf(false) }
    var lisansFiltre by remember { mutableStateOf(baslangicLisans) }
    val gosterilen = when {
        banka == null -> dersler
        lisansFiltre == null -> banka.dersler
        else -> banka.lisansDersleri(lisansFiltre)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. adım: ders
        AdimBasligi("1", "Ders Seç")
        if (banka != null) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val secenekler = banka.lisanslar.map { it.kod to it.kisaAd } + (null to "Tüm Dersler")
                items(secenekler.size) { i ->
                    val (kod, ad) = secenekler[i]
                    val s = kod == lisansFiltre
                    Text(
                        ad, style = MaterialTheme.typography.labelLarge, color = if (s) Krem else Yesil, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (s) Yesil else YesilAcik)
                            .clickable { lisansFiltre = kod }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                "${gosterilen.size} Ders · Ortak Dersler Bütün Lisanslarda Geçerli",
                style = MaterialTheme.typography.bodyMedium, color = MetinSoluk,
            )
        }
        gosterilen.forEach { d ->
            val s = d.kod == secili
            val var_ = d.kod in hedefli && d.kod != secili
            val sekil = RoundedCornerShape(16.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(sekil)
                    .background(Kart)
                    .border(if (s) 2.dp else 1.dp, if (s) dersRengi(d.kod) else Cizgi, sekil)
                    .clickable(enabled = !var_) { dersSec(d.kod) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(dersRengi(d.kod)), contentAlignment = Alignment.Center) {
                    Text(d.kod, style = MaterialTheme.typography.labelLarge, color = Krem)
                }
                Text(d.kisaAd, style = MaterialTheme.typography.titleMedium, maxLines = 2, lineHeight = 20.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                if (var_) {
                    Text("Hedefin Var", style = MaterialTheme.typography.labelMedium, color = MetinSoluk)
                } else Box(
                    Modifier.size(26.dp).clip(CircleShape).background(if (s) dersRengi(d.kod) else Zemin).border(1.dp, if (s) dersRengi(d.kod) else Cizgi, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (s) Text("✓", color = Krem, fontSize = 13.sp) }
            }
        }

        // 2. adım: tarih
        Bosluk(6)
        AdimBasligi("2", "Sınav Tarihi", "İsteğe Bağlı")
        val bugun = LocalDate.now()
        val gun = tarih?.let { java.time.temporal.ChronoUnit.DAYS.between(bugun, it) }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (tarih != null) Yesil else Kart)
                .border(1.dp, if (tarih != null) Yesil else Cizgi, RoundedCornerShape(16.dp))
                .clickable { takvim = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.size(width = 56.dp, height = 60.dp).clip(RoundedCornerShape(12.dp)).background(if (tarih != null) Krem else YesilAcik),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (tarih != null) {
                    Text("${tarih.dayOfMonth}", fontFamily = Baslik, fontSize = 22.sp, color = Yesil)
                    Text(tarih.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("tr")).uppercase(java.util.Locale.forLanguageTag("tr")), fontSize = 11.sp, color = Yesil)
                } else Text("📅", fontSize = 24.sp)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    if (tarih == null) "Tarih Seç" else uzunTarih(tarih),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (tarih != null) Krem else Metin,
                )
                Text(
                    if (gun == null) "Kalan Gün ve Günlük Hedef Hesaplanır" else if (gun == 0L) "Sınav Bugün" else "$gun Gün Kaldı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tarih != null) Altin else MetinSoluk,
                )
            }
            Text("›", fontSize = 24.sp, color = if (tarih != null) Krem else MetinSoluk)
        }
        // Hızlı seçim
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1L to "1 Ay Sonra", 2L to "2 Ay Sonra", 3L to "3 Ay Sonra").forEach { (ay, yazi) ->
                val t = bugun.plusMonths(ay)
                val s = tarih == t
                Text(
                    yazi,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (s) Krem else Yesil,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (s) Yesil else YesilAcik)
                        .clickable { tarihSec(t) }.padding(vertical = 10.dp),
                )
            }
        }
    }
    if (takvim) {
        TarihPenceresi(tarih, kapat = { takvim = false }) { tarihSec(it); takvim = false }
    }
}

@Composable
private fun AdimBasligi(no: String, baslik: String, not: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(Yesil), contentAlignment = Alignment.Center) {
            Text(no, color = Krem, style = MaterialTheme.typography.labelLarge)
        }
        Text(baslik, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
        if (not != null) Text("  · $not", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
    }
}

/** gecmis=true: geçmiş tarihler seçilir (sınav sonucu); false: bugünden sonrası (sınav hedefi). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarihPenceresi(
    mevcut: LocalDate?,
    kapat: () -> Unit,
    gecmis: Boolean = false,
    baslik: String = "Sınav Tarihi",
    sec: (LocalDate) -> Unit,
) {
    val bugunMs = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val durum = rememberDatePickerState(
        initialSelectedDateMillis = (mevcut ?: if (gecmis) LocalDate.now() else LocalDate.now().plusDays(30))
            .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = if (gecmis) utcTimeMillis <= bugunMs else utcTimeMillis >= bugunMs
            override fun isSelectableYear(year: Int) = if (gecmis) year <= LocalDate.now().year else year >= LocalDate.now().year
        },
    )
    DatePickerDialog(
        onDismissRequest = kapat,
        confirmButton = {
            TextButton(
                onClick = {
                    durum.selectedDateMillis?.let { sec(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
                enabled = durum.selectedDateMillis != null,
            ) { Text("Tamam") }
        },
        dismissButton = { TextButton(onClick = kapat) { Text("Vazgeç") } },
    ) {
        DatePicker(state = durum, title = { Text(baslik, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

/** Hedef ekleme ya da düzenleme ekranı. */
@Composable
fun HedefEkrani(
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    duzenlenen: String?,
    geri: () -> Unit,
) {
    val mevcut: Hedef? = duzenlenen?.let { depo.hedef(it) }
    val bosDersler = banka.dersler.filter { d -> d.kod == duzenlenen || depo.hedef(d.kod) == null }
    var ders by remember {
        mutableStateOf(duzenlenen ?: banka.lisansDersleri(depo.kullanici?.lisans).firstOrNull { depo.hedef(it.kod) == null }?.kod ?: bosDersler.firstOrNull()?.kod)
    }
    var tarih by remember { mutableStateOf(mevcut?.sinavTarihi) }
    var silSor by remember { mutableStateOf(false) }
    var tarihsizSor by remember { mutableStateOf(false) }

    Scaffold(topBar = { UstCubuk(if (mevcut == null) "Hedef Ekle" else "Hedefi Düzenle", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(ic)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bosDersler.isEmpty()) {
                Text("Bütün Dersler İçin Hedefin Var")
                return@Column
            }
            HedefSecici(
                dersler = if (mevcut != null) bosDersler.filter { it.kod == duzenlenen } else bosDersler,
                secili = ders,
                dersSec = { ders = it },
                tarih = tarih,
                tarihSec = { tarih = it },
                banka = if (mevcut == null) banka else null,
                baslangicLisans = depo.kullanici?.lisans,
                hedefli = depo.hedefler.map { it.ders }.toSet(),
            )
            Bosluk(8)
            Button(
                onClick = {
                    val d = ders
                    if (d != null) {
                        if (tarih == null) tarihsizSor = true
                        else { depo.hedefKaydet(d, tarih, gecti = false); geri() }
                    }
                },
                enabled = ders != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("🎯 Hedefi Kaydet") }
            if (mevcut != null) {
                OutlinedButton(
                    onClick = { depo.hedefKaydet(mevcut.ders, mevcut.sinavTarihi, gecti = true); geri() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Bu Dersten Geçtim") }
                TextButton(onClick = { silSor = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hedefi Kaldır", color = YanlisKirmizi)
                }
            }
        }
    }
    if (tarihsizSor) {
        TarihsizPenceresi(
            kapat = { tarihsizSor = false },
            devam = {
                tarihsizSor = false
                ders?.let { depo.hedefKaydet(it, null, gecti = false); geri() }
            },
        )
    }
    if (silSor && mevcut != null) {
        AlertDialog(
            onDismissRequest = { silSor = false },
            title = { Text("Hedefi Kaldır") },
            text = { Text("Bu Dersin Sınav Tarihi ve Geri Sayımı Kaldırılır. Çözdüğün Sorular Silinmez.") },
            confirmButton = { TextButton(onClick = { depo.hedefSil(mevcut.ders); silSor = false; geri() }) { Text("Kaldır", color = YanlisKirmizi) } },
            dismissButton = { TextButton(onClick = { silSor = false }) { Text("Vazgeç") } },
        )
    }
}

/** Sınav tarihi seçilmeden kaydedilirken hatırlatır; zorunlu tutmaz. */
@Composable
fun TarihsizPenceresi(kapat: () -> Unit, devam: () -> Unit) {
    AlertDialog(
        onDismissRequest = kapat,
        title = { Text("Sınav Tarihi Seçmedin") },
        text = {
            Text("Tarih Seçersen Kaç Gün Kaldığını Görür, Günlük Hedefini Sınavına Göre Ayarlarız. Tarihi Daha Sonra da Ekleyebilirsin. Yine de Devam Etmek İstiyor musun?")
        },
        confirmButton = { TextButton(onClick = devam) { Text("Devam Et") } },
        dismissButton = { TextButton(onClick = kapat) { Text("Tarih Seç") } },
    )
}
