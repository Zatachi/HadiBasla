package com.lisanshazirlik.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lisanshazirlik.app.data.Hatirlatma
import com.lisanshazirlik.app.data.Lisans
import com.lisanshazirlik.app.data.YerelKayit

/** Lisans listesi; aktif olmayanlar "Yakında" olarak gösterilir. */
@Composable
fun LisansSecici(lisanslar: List<Lisans>, secili: String?, icerikVar: (Lisans) -> Boolean = { true }, sec: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lisanslar.forEach { l ->
            val s = l.kod == secili
            val sekil = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(sekil)
                    .background(if (s) YesilAcik else Kart)
                    .border(if (s) 2.dp else 1.dp, if (s) Yesil else Cizgi, sekil)
                    .clickableIf(l.aktif) { sec(l.kod) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = s, onClick = if (l.aktif) ({ sec(l.kod) }) else null, enabled = l.aktif)
                Column(Modifier.weight(1f)) {
                    Text(l.ad, style = MaterialTheme.typography.titleMedium, color = if (l.aktif) Metin else MetinSoluk)
                    Text(
                        when {
                            !l.aktif -> "Yakında"
                            icerikVar(l) -> "${l.dersler.size} ders"
                            else -> "${l.dersler.size} ders · içerik yakında"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (l.aktif && !icerikVar(l)) Sari2 else MetinSoluk,
                    )
                }
            }
        }
    }
}

@Composable
fun LisansEkrani(lisanslar: List<Lisans>, secili: String?, sec: (String) -> Unit, geri: () -> Unit, icerikVar: (Lisans) -> Boolean = { true }) {
    Scaffold(topBar = { UstCubuk("Sınavım", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Hangi lisans sınavına hazırlanıyorsun?", style = MaterialTheme.typography.titleLarge)
            Text("Dersler, denemeler ve istatistikler seçtiğin lisansa göre gösterilir.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            LisansSecici(lisanslar, secili, icerikVar) { sec(it); geri() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatirlatmaEkrani(kayit: YerelKayit, geri: () -> Unit) {
    val baglam = LocalContext.current
    kayit.degisim
    var saatSec by remember { mutableStateOf(false) }
    var izinReddedildi by remember { mutableStateOf(false) }
    val dk = kayit.hatirlatmaSaati
    val saatYazi = "%02d:%02d".format(dk / 60, dk % 60)

    fun ac() {
        kayit.hatirlatmaAcik = true
        Hatirlatma.kur(baglam, dk / 60, dk % 60)
    }

    val izinIste = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { verildi ->
        if (verildi) ac() else izinReddedildi = true
    }

    Scaffold(topBar = { UstCubuk("Günlük hatırlatma", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KartKutu {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Her gün hatırlat", style = MaterialTheme.typography.titleMedium)
                        Text("Seçtiğin saatte çalışma bildirimi gelir.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    }
                    Switch(
                        checked = kayit.hatirlatmaAcik,
                        onCheckedChange = { acik ->
                            if (acik) {
                                if (Build.VERSION.SDK_INT >= 33 && !Hatirlatma.izinVar(baglam)) izinIste.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else ac()
                            } else {
                                kayit.hatirlatmaAcik = false
                                Hatirlatma.kapat(baglam)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Yesil),
                    )
                }
                Bosluk(12)
                OutlinedButton(onClick = { saatSec = true }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Saat: $saatYazi")
                }
            }
            if (izinReddedildi) {
                Text(
                    "Bildirim izni verilmedi. Hatırlatma almak için telefonun Ayarlar → Uygulamalar → Lisansla → Bildirimler bölümünden izin verebilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = YanlisKirmizi,
                )
            }
        }
    }

    if (saatSec) {
        val durum = rememberTimePickerState(initialHour = dk / 60, initialMinute = dk % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { saatSec = false },
            title = { Text("Hatırlatma saati") },
            text = { TimePicker(state = durum) },
            confirmButton = {
                TextButton(onClick = {
                    kayit.hatirlatmaSaati = durum.hour * 60 + durum.minute
                    if (kayit.hatirlatmaAcik) Hatirlatma.kur(baglam, durum.hour, durum.minute)
                    saatSec = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { saatSec = false }) { Text("Vazgeç") } },
        )
    }
}

private val Sari2 = androidx.compose.ui.graphics.Color(0xFFB07A10)

/** Lisans seçimi için açılır pencere (Deneme sekmesi başlığından). */
@Composable
fun LisansPenceresi(banka: com.lisanshazirlik.app.data.SoruBankasi, secili: String?, kapat: () -> Unit, sec: (String) -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = kapat,
        title = { Text("Hangi lisans?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LisansSecici(banka.lisanslar, secili, { l -> banka.icerikVar(l) }) { sec(it); kapat() }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = kapat) { Text("Kapat") } },
    )
}
