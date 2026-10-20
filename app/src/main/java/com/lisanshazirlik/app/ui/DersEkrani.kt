package com.lisanshazirlik.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import com.lisanshazirlik.app.data.Ders
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi

@Composable
fun DersEkrani(
    ders: Ders,
    banka: SoruBankasi,
    cozulen: Set<String>,
    coz: (baslik: String, sorular: List<Soru>, konuId: String?) -> Unit,
    deneme: () -> Unit,
    konuAc: (String) -> Unit,
    paketAc: () -> Unit,
    geri: () -> Unit,
) {
    val tumu = banka.dersSorulari(ders.kod)

    Scaffold(topBar = { UstCubuk(ders.kisaAd, geri) }, containerColor = Zemin) { ic ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(ic),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(ders.ad, style = MaterialTheme.typography.titleLarge)
                Bosluk(4)
                Text("Ders Kodu ${ders.kod}" + if (ders.aciklama.isNotBlank()) " · ${ders.aciklama}" else "", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background(Yesil)
                        .clickableIf(true, paketAc)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📦", fontSize = 28.sp)
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Eğitim Paketi", style = MaterialTheme.typography.titleMedium, color = Krem)
                        Text("Konu Konu Öğren, Tekrar Et, Soru Çöz; Sonunda Deneme.", style = MaterialTheme.typography.bodyMedium, color = KremSoluk)
                    }
                    Text("›", fontSize = 26.sp, color = Altin)
                }
            }
            item {
                KartKutu(
                    tikla = if (tumu.isNotEmpty()) ({ coz(ders.kisaAd, tumu.shuffled(), null) }) else null,
                ) {
                    Text("Tüm Konulardan Karışık", style = MaterialTheme.typography.titleMedium, color = Yesil)
                    Bosluk(4)
                    Text("${tumu.size} Soru", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
            item {
                KartKutu(tikla = if (tumu.isNotEmpty()) deneme else null) {
                    Text("Bu Dersten Deneme Sınavı", style = MaterialTheme.typography.titleMedium)
                    Bosluk(4)
                    Text(if (tumu.size >= SINAV_SORU) "Gerçek sınav gibi: $SINAV_SORU soru, $SINAV_DAKIKA dakika. Geçme notu 50." else "${tumu.size} soru, ${denemeSuresiDk(tumu.size)} dakika. Soru bankası büyüdükçe 25 soruya tamamlanacak.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
            item { Etiket("Konular", Modifier.padding(start = 4.dp, top = 8.dp)) }
            items(ders.konular, key = { it.id }) { konu ->
                val sorular = banka.konuSorulari(konu.id)
                val c = sorular.count { it.id in cozulen }
                val icerikVar = sorular.isNotEmpty() || konu.ozet.isNotBlank() || konu.kartlar.isNotEmpty()
                KartKutu(tikla = if (icerikVar) ({ konuAc(konu.id) }) else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(konu.ad, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            when {
                                !icerikVar -> "Yakında"
                                else -> listOfNotNull(
                                    "Özet".takeIf { konu.ozet.isNotBlank() },
                                    "${konu.kartlar.size} Kart".takeIf { konu.kartlar.isNotEmpty() },
                                    "${sorular.size} Soru".takeIf { sorular.isNotEmpty() },
                                ).joinToString(" · ")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetinSoluk,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (sorular.isNotEmpty()) {
                        Bosluk(10)
                        Ilerleme(c / sorular.size.toFloat())
                    }
                }
            }
        }
    }
}
