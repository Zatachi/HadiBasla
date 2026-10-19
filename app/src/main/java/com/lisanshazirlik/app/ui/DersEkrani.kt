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
import com.lisanshazirlik.app.data.Ders
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.YerelKayit

@Composable
fun DersEkrani(
    ders: Ders,
    banka: SoruBankasi,
    kayit: YerelKayit,
    coz: (baslik: String, sorular: List<Soru>) -> Unit,
    geri: () -> Unit,
) {
    kayit.degisim
    val cozulen = kayit.cozulenler()
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
                Text("Ders kodu ${ders.kod}", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            }
            item {
                KartKutu(
                    tikla = if (tumu.isNotEmpty()) ({ coz(ders.kisaAd, tumu.shuffled()) }) else null,
                ) {
                    Text("Tüm konulardan karışık", style = MaterialTheme.typography.titleMedium, color = Yesil)
                    Bosluk(4)
                    Text("${tumu.size} soru", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
            item { Etiket("Konular", Modifier.padding(start = 4.dp, top = 8.dp)) }
            items(ders.konular, key = { it.id }) { konu ->
                val sorular = banka.konuSorulari(konu.id)
                val c = sorular.count { it.id in cozulen }
                KartKutu(tikla = if (sorular.isNotEmpty()) ({ coz(konu.ad, sorular.shuffled()) }) else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(konu.ad, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            if (sorular.isEmpty()) "Yakında" else "${sorular.size} soru",
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
