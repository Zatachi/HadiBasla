package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi

const val DENEME_ADEDI = 10
const val UCRETSIZ_DENEME = 1

val DENEME_ADLARI = listOf(
    "Isınma Turu", "İlk Adım", "Temel Prova", "Pekiştirme", "Orta Seviye",
    "Zorlu Tur", "Hız Testi", "Tekrar Turu", "Genel Prova", "Final Provası",
)

fun denemeAdi(no: Int): String = "Deneme $no · ${DENEME_ADLARI.getOrElse(no - 1) { "" }}"

/**
 * Numaralı deneme: dersin soruları sabit bir sırayla karıştırılır, her deneme farklı 25 soruyu alır.
 * Böylece "Deneme 3" her açılışta aynı sorulardan oluşur. Yeterli soru yoksa null (hazırlanıyor).
 * Deneme 1, soru az olsa da eldeki sorularla açılır.
 */
fun numaraliDeneme(banka: SoruBankasi, ders: String, no: Int): List<Soru>? {
    val havuz = banka.dersSorulari(ders).sortedBy { it.id }.shuffled(java.util.Random(ders.hashCode().toLong() * 7919))
    if (havuz.isEmpty()) return null
    if (no == 1) return havuz.take(SINAV_SORU)
    if (havuz.size < no * SINAV_SORU) return null
    return havuz.subList((no - 1) * SINAV_SORU, no * SINAV_SORU)
}

@Composable
fun DenemeListesiEkrani(
    dersKod: String,
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    ac: (no: Int) -> Unit,
    premiumAc: () -> Unit,
    geri: () -> Unit,
) {
    val ders = banka.ders(dersKod) ?: return
    val premium = depo.kullanici?.premiumAktif() == true
    val kayitlar = depo.denemeler.filter { it.kapsam == dersKod }

    Scaffold(topBar = { UstCubuk("Denemeler", geri) }, containerColor = Zemin) { ic ->
        LazyColumn(
            Modifier.fillMaxSize().padding(ic),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(dersRengi(ders.kod)), contentAlignment = Alignment.Center) {
                        Text(ders.kod, style = MaterialTheme.typography.labelLarge, color = Krem)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(ders.kisaAd, style = MaterialTheme.typography.titleMedium)
                        Text("$DENEME_ADEDI deneme · her biri 25 soru, 45 dk", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    }
                }
            }
            if (!premium) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(YesilAcik)
                            .clickableIf(true, premiumAc)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔓", fontSize = 22.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("Deneme 1 Ücretsiz", style = MaterialTheme.typography.titleMedium)
                            Text("Diğerleri Premium ile açılır. Premium'u davetle ya da haftalık ligde kazanabilirsin.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                        }
                    }
                }
            }
            items((1..DENEME_ADEDI).toList(), key = { "n$it" }) { no ->
                val hazir = numaraliDeneme(banka, dersKod, no) != null
                val kilitli = !premium && no > UCRETSIZ_DENEME
                val son = kayitlar.filter { it.no == no }.maxByOrNull { it.zaman }
                val renk = when {
                    son?.gecti == true -> DogruYesil
                    son != null -> YanlisKirmizi
                    else -> Yesil
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Kart)
                        .border(1.dp, Cizgi, RoundedCornerShape(16.dp))
                        .clickableIf(hazir) { if (kilitli) premiumAc() else ac(no) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(if (!hazir || kilitli) Zemin else YesilAcik)
                            .border(1.dp, Cizgi, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$no", style = MaterialTheme.typography.titleMedium, color = if (!hazir || kilitli) MetinSoluk else Yesil)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            "Deneme $no",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (!hazir || kilitli) MetinSoluk else Metin,
                        )
                        Text(
                            when {
                                !hazir -> "${DENEME_ADLARI[no - 1]} · hazırlanıyor"
                                son != null -> "${DENEME_ADLARI[no - 1]} · son puan ${son.ortalama} ${if (son.gecti) "✓" else ""}"
                                else -> DENEME_ADLARI[no - 1]
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (son != null && hazir) renk else MetinSoluk,
                        )
                    }
                    when {
                        !hazir -> Text("Yakında", style = MaterialTheme.typography.labelMedium, color = MetinSoluk)
                        kilitli -> Text("🔒", fontSize = 20.sp)
                        else -> Box(Modifier.size(38.dp).clip(CircleShape).background(Yesil), contentAlignment = Alignment.Center) {
                            Text("▶", color = Krem, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
