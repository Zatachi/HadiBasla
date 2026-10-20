package com.lisanshazirlik.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lisanshazirlik.app.data.Soru

/**
 * Önce soru, sonra anlatım: konuya girmeden 3 ısınma sorusu. Puan ve istatistiğe yazılmaz;
 * amacı merak uyandırmak ve neyi bilmediğini göstermek.
 */
@Composable
fun IsinmaEkrani(konuAd: String, sorular: List<Soru>, geri: () -> Unit, bitti: () -> Unit) {
    var sira by remember { mutableIntStateOf(0) }
    var secim by remember { mutableStateOf<Int?>(null) }
    var dogru by remember { mutableIntStateOf(0) }
    val son = sira >= sorular.size

    Scaffold(topBar = { UstCubuk("Isınma · $konuAd", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (son) {
                Text("🔥", style = MaterialTheme.typography.displayMedium)
                Text("${sorular.size} sorudan $dogru tanesini bildin", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (dogru == sorular.size) "Konuya hâkimsin gibi görünüyor. Yine de anlatım cevapların nedenini pekiştirecek."
                    else "Bilemediklerinin cevabı birazdan anlatımda. Dikkatle izle!",
                    color = MetinSoluk,
                )
                Bosluk(12)
                Button(onClick = bitti, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("▶ Anlatıma geç") }
                return@Column
            }
            val soru = sorular[sira]
            Etiket("Isınma ${sira + 1}/${sorular.size} · yanlış yapman normal, puana yazılmaz")
            Text(soru.metin, style = MaterialTheme.typography.titleMedium)
            soru.secenekler.forEachIndexed { i, m ->
                val d = when {
                    secim == null -> SikDurumu.NORMAL
                    i == soru.dogru -> SikDurumu.DOGRU
                    i == secim -> SikDurumu.YANLIS
                    else -> SikDurumu.SOLUK
                }
                Sik(HARFLER[i], m, d, if (secim == null) ({ secim = i; if (i == soru.dogru) dogru++ }) else null)
            }
            if (secim != null) {
                Text(
                    if (secim == soru.dogru) "✓ Doğru! Bakalım nedenini de biliyor musun." else "Olmadı. Cevabın nedeni birazdan anlatımda.",
                    color = if (secim == soru.dogru) DogruYesil else Yesil,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = { sira++; secim = null }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text(if (sira == sorular.lastIndex) "Bitir" else "Sonraki")
                }
            }
            TextButton(onClick = bitti) { Text("Isınmayı atla") }
        }
    }
}
