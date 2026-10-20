package com.lisanshazirlik.app.ui

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
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

/** Lisans kartları: iki sütunlu, her lisansın kendi rengi ve simgesi; seçili olan altın çerçeveli. */
@Composable
fun LisansSecici(
    lisanslar: List<Lisans>,
    secili: String?,
    icerikVar: (Lisans) -> Boolean = { true },
    ilerleme: ((Lisans) -> Pair<Int, Int>)? = null,
    sec: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        lisanslar.chunked(2).forEach { satir ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                satir.forEach { l ->
                    val i = lisanslar.indexOf(l)
                    val renk = SEVIYE_RENKLERI[i % SEVIYE_RENKLERI.size]
                    val s = l.kod == secili
                    val hazir = icerikVar(l)
                    val sekil = RoundedCornerShape(18.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(150.dp)
                            .clip(sekil)
                            .background(renk.copy(alpha = if (hazir) 1f else 0.7f))
                            .border(if (s) 3.dp else 0.dp, if (s) Altin else androidx.compose.ui.graphics.Color.Transparent, sekil)
                            .clickableIf(l.aktif) { sec(l.kod) }
                            .padding(14.dp),
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                                Text(seviyeSimgesi(l.kod), fontSize = 20.sp)
                            }
                            Box(Modifier.weight(1f))
                            Text(l.kisaAd, style = MaterialTheme.typography.titleMedium, color = Krem, maxLines = 2, lineHeight = 20.sp, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Bosluk(2)
                            val ilr = ilerleme?.invoke(l)
                            if (ilr != null && ilr.first > 0) {
                                Text("${ilr.first}/${ilr.second} Ders Geçildi", fontSize = 12.sp, color = Altin)
                                Bosluk(4)
                                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(androidx.compose.ui.graphics.Color(0x33FFFFFF))) {
                                    Box(Modifier.fillMaxWidth(ilr.first / ilr.second.toFloat()).height(4.dp).background(Altin))
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (l.aktif) "${l.dersler.size} Ders" else "", fontSize = 12.sp, color = KremSoluk)
                                    if (!l.aktif || !hazir) {
                                        Text(
                                            "Yakında",
                                            fontSize = 11.sp, color = YesilKoyu,
                                            modifier = Modifier.padding(start = if (l.aktif) 6.dp else 0.dp).clip(RoundedCornerShape(50)).background(Altin)
                                                .padding(horizontal = 7.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                            }
                        }
                        if (s) {
                            Box(
                                Modifier.align(Alignment.TopEnd).size(26.dp).clip(CircleShape).background(Altin),
                                contentAlignment = Alignment.Center,
                            ) { Text("✓", color = YesilKoyu, fontSize = 14.sp) }
                        }
                    }
                }
                if (satir.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun LisansEkrani(
    banka: com.lisanshazirlik.app.data.SoruBankasi,
    depo: com.lisanshazirlik.app.data.KullaniciDeposu,
    sec: (String) -> Unit,
    geri: () -> Unit,
) {
    Scaffold(topBar = { UstCubuk("Sınavım", geri) }, containerColor = Zemin) { ic ->
        Column(Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().background(Yesil).padding(start = 20.dp, end = 20.dp, bottom = 18.dp)) {
                Text("Hangi Lisansa Hazırlanıyorsun?", fontFamily = Baslik, fontSize = 22.sp, color = Krem)
                Bosluk(4)
                Text("Dersler, Denemeler ve İstatistikler Bu Seçime Göre Gelir.", style = MaterialTheme.typography.bodyMedium, color = KremSoluk)
            }
            Column(Modifier.padding(16.dp)) {
                LisansSecici(
                    banka.lisanslar,
                    depo.kullanici?.lisans,
                    icerikVar = { banka.icerikVar(it) },
                    ilerleme = { l -> lisansDurumu(banka, depo.sonuclar, l.kod).let { it.second to it.third } },
                ) { sec(it); geri() }
                Bosluk(12)
                Text("Ortak Dersler Bütün Lisanslarda Geçerli Sayılır.", fontSize = 12.sp, color = MetinSoluk)
            }
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

    Scaffold(topBar = { UstCubuk("Günlük Hatırlatma", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KartKutu {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Her Gün Hatırlat", style = MaterialTheme.typography.titleMedium)
                        Text("Seçtiğin Saatte Çalışma Bildirimi Gelir.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
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
                    "Bildirim İzni Verilmedi. Hatırlatma Almak İçin Telefonun Ayarlar → Uygulamalar → Lisansla → Bildirimler Bölümünden İzin Verebilirsin.",
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
            title = { Text("Hatırlatma Saati") },
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
