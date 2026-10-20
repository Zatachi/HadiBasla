package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Odak
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.Tekrar

/** Günün 5 dakikası: sadece odak dersten; başlamadan önce ne geleceği görünür. */
@Composable
fun GununBesEkrani(banka: SoruBankasi, depo: KullaniciDeposu, basla: (baslik: String, List<Soru>) -> Unit, geri: () -> Unit) {
    val odak = Odak.ders(banka, depo)
    val kod = odak?.kod
    val bekleyen = if (kod == null) emptyList() else Tekrar.bekleyenler(banka, depo, listOf(kod))
    val oturum = if (kod == null) emptyList() else Tekrar.oturum(banka, depo, listOf(kod))
    val yeni = oturum.count { it.id !in depo.ilerleme }
    val tekrar = oturum.size - yeni

    Scaffold(topBar = { UstCubuk("Günün 5 Dakikası", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Hangi derse çalışıyorsun?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
            OdakSecici(banka, depo, kod)
            Column(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(if (kod != null) dersRengi(kod) else YesilKoyu).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🔁", fontSize = 40.sp)
                Text(odak?.kisaAd ?: "Ders Seç", fontFamily = Baslik, fontSize = 22.sp, color = Krem)
                Bosluk(12)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SayiKutusu(Modifier.weight(1f), "$tekrar", "Tekrar")
                    SayiKutusu(Modifier.weight(1f), "$yeni", "Yeni Soru")
                    SayiKutusu(Modifier.weight(1f), "~${maxOf(1, oturum.size / 2)}", "Dakika")
                }
            }
            Text(
                when {
                    oturum.isEmpty() -> "Bu dersin soruları henüz hazır değil. Başka bir ders seçebilirsin."
                    bekleyen.isNotEmpty() -> "Unutmaya başladığın ${bekleyen.size} soru tam zamanında geri geldi. Doğru bildiklerin daha seyrek gelir."
                    else -> "Bugün tekrar zamanı gelen soru yok; yeni sorularla devam ediyoruz."
                },
                color = MetinSoluk,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Button(
                onClick = { basla("Günün 5 dakikası · ${odak?.kisaAd.orEmpty()}", oturum) },
                enabled = oturum.isNotEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
            ) { Text("▶ Başla") }
        }
    }
}

@Composable
private fun SayiKutusu(modifier: Modifier, sayi: String, etiket: String) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color(0x33000000)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(sayi, fontFamily = Baslik, fontSize = 24.sp, color = Altin)
        Text(etiket, fontSize = 12.sp, color = KremSoluk)
    }
}
