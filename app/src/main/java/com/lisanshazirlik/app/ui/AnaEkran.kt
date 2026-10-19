package com.lisanshazirlik.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.R
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.YerelKayit

@Composable
fun AnaEkran(
    banka: SoruBankasi,
    kayit: YerelKayit,
    dersAc: (String) -> Unit,
    denemeBaslat: () -> Unit,
    yanlislariAc: () -> Unit,
    gorusAc: () -> Unit,
) {
    kayit.degisim // kayıt değişince yenilensin
    val cozulen = kayit.cozulenler()
    val dogru = kayit.dogrular()
    val yanlisSayisi = kayit.yanlislar().count { banka.bul(it) != null }
    val toplam = banka.sorular.size
    val cozulenSayi = banka.sorular.count { it.id in cozulen }
    val dogruSayi = banka.sorular.count { it.id in dogru }
    var denemeSor by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Zemin)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Ust() }

        item {
            KartKutu(Modifier.padding(horizontal = 16.dp)) {
                Etiket("Genel ilerleme")
                Bosluk(8)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$cozulenSayi", style = MaterialTheme.typography.headlineSmall)
                    Text(" / $toplam soru çözüldü", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.padding(bottom = 3.dp))
                }
                Bosluk(10)
                Ilerleme(if (toplam == 0) 0f else cozulenSayi / toplam.toFloat())
                Bosluk(10)
                BilgiSatiri("Doğru oranın", if (cozulenSayi == 0) "–" else "%${yuzde(dogruSayi, cozulenSayi)}")
            }
        }

        item { Etiket("Dersler", Modifier.padding(start = 20.dp, top = 8.dp)) }

        items(banka.dersler, key = { it.kod }) { ders ->
            val sorular = banka.dersSorulari(ders.kod)
            val c = sorular.count { it.id in cozulen }
            KartKutu(Modifier.padding(horizontal = 16.dp), tikla = { dersAc(ders.kod) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(YesilAcik),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ders.kod, style = MaterialTheme.typography.labelLarge, color = YesilKoyu, fontSize = 13.sp)
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(ders.kisaAd, style = MaterialTheme.typography.titleMedium)
                        Text("${sorular.size} soru · ${ders.konular.size} konu", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                    }
                    Text("%${yuzde(c, sorular.size)}", style = MaterialTheme.typography.labelLarge, color = Yesil)
                }
                Bosluk(12)
                Ilerleme(if (sorular.isEmpty()) 0f else c / sorular.size.toFloat())
            }
        }

        item { Etiket("Çalışma", Modifier.padding(start = 20.dp, top = 8.dp)) }

        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KartKutu(Modifier.weight(1f), tikla = { denemeSor = true }) {
                    Text("Deneme sınavı", style = MaterialTheme.typography.titleMedium)
                    Bosluk(4)
                    Text("Dört dersten karışık, süreli", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
                KartKutu(Modifier.weight(1f), tikla = if (yanlisSayisi > 0) yanlislariAc else null) {
                    Text("Yanlışlarım", style = MaterialTheme.typography.titleMedium)
                    Bosluk(4)
                    Text(
                        if (yanlisSayisi == 0) "Henüz yanlışın yok" else "$yanlisSayisi soruyu tekrar çöz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (yanlisSayisi == 0) MetinSoluk else YanlisKirmizi,
                    )
                }
            }
        }

        item {
            KartKutu(Modifier.padding(horizontal = 16.dp), tikla = gorusAc) {
                Text("Görüşünü yaz", style = MaterialTheme.typography.titleMedium)
                Bosluk(4)
                Text("Uygulamayı ve soruları senin yorumlarınla geliştiriyoruz.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            }
        }

        item {
            Text(
                "Lisansla bağımsız bir çalışma aracıdır; SPL veya SPK ile bağlantılı değildir. " +
                    "Sorular güncel mevzuata göre hazırlanır, sınava girmeden önce resmi kaynakları da kontrol et." +
                    (if (banka.guncelleme.isNotBlank()) "\nSoru bankası: ${banka.guncelleme}" else ""),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MetinSoluk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
            )
        }
    }

    if (denemeSor) {
        val adet = denemeSoruSayisi(banka)
        AlertDialog(
            onDismissRequest = { denemeSor = false },
            title = { Text("Deneme sınavı") },
            text = {
                Text(
                    "$adet soru, ${denemeSuresiDk(adet)} dakika. Her dersten eşit sayıda soru gelir. " +
                        "Cevaplar sınav bitince gösterilir.\n\nGeçme kuralı: her dersten en az 50, ortalama en az 60."
                )
            },
            confirmButton = { TextButton(onClick = { denemeSor = false; denemeBaslat() }) { Text("Başla") } },
            dismissButton = { TextButton(onClick = { denemeSor = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun Ust() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Yesil)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Column(Modifier.padding(start = 4.dp)) {
                Text("Lisansla", fontFamily = Baslik, fontSize = 30.sp, color = Krem)
                Box(Modifier.padding(vertical = 4.dp).width(48.dp).height(2.dp).background(Altin))
                Text("DÜZEY 1 SINAV HAZIRLIK", style = MaterialTheme.typography.labelMedium, color = KremSoluk)
            }
        }
    }
}
