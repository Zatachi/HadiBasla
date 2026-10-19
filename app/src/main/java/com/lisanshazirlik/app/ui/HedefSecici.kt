package com.lisanshazirlik.app.ui

import androidx.compose.foundation.BorderStroke
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
) {
    var takvim by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Etiket("Hangi derse çalışıyorsun?")
        dersler.forEach { d ->
            val s = d.kod == secili
            val sekil = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(sekil)
                    .background(if (s) YesilAcik else Kart)
                    .border(if (s) 2.dp else 1.dp, if (s) Yesil else Cizgi, sekil)
                    .clickable { dersSec(d.kod) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = s, onClick = { dersSec(d.kod) })
                Column {
                    Text(d.kisaAd, style = MaterialTheme.typography.titleMedium)
                    Text("Ders kodu ${d.kod}", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
        }
        Bosluk(8)
        Etiket("Sınav tarihin")
        OutlinedButton(
            onClick = { takvim = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            border = BorderStroke(1.dp, if (tarih == null) Cizgi else Yesil),
        ) {
            Text(
                if (tarih == null) "Tarih seç" else "${uzunTarih(tarih)} · ${kalanGunYazisi(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), tarih))}",
                color = if (tarih == null) MetinSoluk else Yesil,
            )
        }
        Text(
            "İsteğe bağlı. Tarih seçersen kalan günleri ve günlük hedefini sınavına göre hesaplarız; sonra da ekleyebilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinSoluk,
        )
    }
    if (takvim) {
        TarihPenceresi(tarih, kapat = { takvim = false }) { tarihSec(it); takvim = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarihPenceresi(mevcut: LocalDate?, kapat: () -> Unit, sec: (LocalDate) -> Unit) {
    val bugunMs = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val durum = rememberDatePickerState(
        initialSelectedDateMillis = (mevcut ?: LocalDate.now().plusDays(30)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= bugunMs
            override fun isSelectableYear(year: Int) = year >= LocalDate.now().year
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
        DatePicker(state = durum, title = { Text("Sınav tarihi", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
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
    var ders by remember { mutableStateOf(duzenlenen ?: bosDersler.firstOrNull()?.kod) }
    var tarih by remember { mutableStateOf(mevcut?.sinavTarihi) }
    var silSor by remember { mutableStateOf(false) }
    var tarihsizSor by remember { mutableStateOf(false) }

    Scaffold(topBar = { UstCubuk(if (mevcut == null) "Hedef ekle" else "Hedefi düzenle", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(ic)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bosDersler.isEmpty()) {
                Text("Dört ders için de hedefin var. Bir hedefi düzenlemek için Ana sayfadaki kartına dokun.")
                return@Column
            }
            HedefSecici(
                dersler = if (mevcut != null) bosDersler.filter { it.kod == duzenlenen } else bosDersler,
                secili = ders,
                dersSec = { ders = it },
                tarih = tarih,
                tarihSec = { tarih = it },
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
            ) { Text("Kaydet") }
            if (mevcut != null) {
                OutlinedButton(
                    onClick = { depo.hedefKaydet(mevcut.ders, mevcut.sinavTarihi, gecti = true); geri() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Bu dersten geçtim") }
                TextButton(onClick = { silSor = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hedefi kaldır", color = YanlisKirmizi)
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
            title = { Text("Hedefi kaldır") },
            text = { Text("Bu dersin sınav tarihi ve geri sayımı kaldırılır. Çözdüğün sorular silinmez.") },
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
        title = { Text("Sınav tarihi seçmedin") },
        text = {
            Text("Tarih seçersen kaç gün kaldığını görür, günlük hedefini sınavına göre ayarlarız. Tarihi daha sonra da ekleyebilirsin. Yine de devam etmek istiyor musun?")
        },
        confirmButton = { TextButton(onClick = devam) { Text("Devam et") } },
        dismissButton = { TextButton(onClick = kapat) { Text("Tarih seç") } },
    )
}
