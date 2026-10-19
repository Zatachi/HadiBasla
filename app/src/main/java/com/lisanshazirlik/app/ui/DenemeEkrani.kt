package com.lisanshazirlik.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.GeriBildirim
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.YerelKayit
import kotlinx.coroutines.delay

private const val DERS_BASINA_EN_FAZLA = 10

/** Her dersten eşit sayıda soru: en az sorusu olan dersin sayısı ya da en fazla 10. */
fun denemeSoruSayisi(banka: SoruBankasi): Int {
    val dolu = banka.dersler.map { banka.dersSorulari(it.kod).size }.filter { it > 0 }
    if (dolu.isEmpty()) return 0
    return minOf(DERS_BASINA_EN_FAZLA, dolu.min()) * dolu.size
}

fun denemeSuresiDk(adet: Int): Int = (adet * 3 + 1) / 2 // soru başına 1,5 dakika

fun denemeSorulari(banka: SoruBankasi): List<Soru> {
    val dolu = banka.dersler.filter { banka.dersSorulari(it.kod).isNotEmpty() }
    if (dolu.isEmpty()) return emptyList()
    val n = minOf(DERS_BASINA_EN_FAZLA, dolu.minOf { banka.dersSorulari(it.kod).size })
    return dolu.flatMap { banka.dersSorulari(it.kod).shuffled().take(n) }
}

@Composable
fun DenemeEkrani(
    sorular: List<Soru>,
    banka: SoruBankasi,
    kayit: YerelKayit,
    geri: () -> Unit,
) {
    val cevaplar = remember { mutableStateMapOf<Int, Int>() }
    var sira by remember { mutableIntStateOf(0) }
    var kalan by remember { mutableIntStateOf(denemeSuresiDk(sorular.size) * 60) }
    var bitti by remember { mutableStateOf(false) }
    var bitirSor by remember { mutableStateOf(false) }
    var cikSor by remember { mutableStateOf(false) }

    fun bitir() {
        if (bitti) return
        bitti = true
        sorular.forEachIndexed { i, s ->
            val c = cevaplar[i] ?: return@forEachIndexed
            val dogruMu = c == s.dogru
            kayit.kaydet(s.id, dogruMu)
            GeriBildirim.cozum(s.id, c, dogruMu, "deneme")
        }
    }

    LaunchedEffect(Unit) {
        while (kalan > 0 && !bitti) {
            delay(1000)
            if (!bitti) kalan--
        }
        bitir()
    }

    BackHandler(enabled = !bitti) { cikSor = true }

    if (sorular.isEmpty()) {
        Scaffold(topBar = { UstCubuk("Deneme sınavı", geri) }, containerColor = Zemin) { ic ->
            Text("Deneme için henüz yeterli soru yok.", modifier = Modifier.padding(ic).padding(24.dp))
        }
        return
    }

    if (bitti) {
        DenemeSonucu(sorular, cevaplar, banka, geri)
        return
    }

    val soru = sorular[sira]
    val gezinme = rememberLazyListState()
    LaunchedEffect(sira) { gezinme.animateScrollToItem(maxOf(0, sira - 3)) }

    Scaffold(
        topBar = {
            UstCubuk("Deneme sınavı", { cikSor = true }) {
                Text(
                    "%d:%02d".format(kalan / 60, kalan % 60),
                    color = if (kalan < 120) Altin else Krem,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        },
        bottomBar = {
            Column(Modifier.background(Kart).navigationBarsPadding()) {
                HorizontalDivider(color = Cizgi)
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { sira-- },
                        enabled = sira > 0,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Önceki") }
                    if (sira < sorular.lastIndex) {
                        Button(onClick = { sira++ }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Sonraki") }
                    } else {
                        Button(onClick = { bitirSor = true }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Bitir") }
                    }
                }
            }
        },
        containerColor = Zemin,
    ) { ic ->
        Column(Modifier.fillMaxSize().padding(ic)) {
            LazyRow(
                state = gezinme,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(sorular) { i, _ ->
                    val cevapli = cevaplar.containsKey(i)
                    val aktif = i == sira
                    val sekil = RoundedCornerShape(8.dp)
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(sekil)
                            .background(if (cevapli) Yesil else Kart)
                            .border(if (aktif) 2.dp else 1.dp, if (aktif) Altin else Cizgi, sekil)
                            .clickable { sira = i },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (cevapli) Krem else Metin)
                    }
                }
            }
            HorizontalDivider(color = Cizgi)
            key(sira) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SoruMetni(soru, "Soru ${sira + 1} · ${banka.ders(soru.ders)?.kisaAd ?: soru.ders}")
                    Bosluk(6)
                    soru.secenekler.forEachIndexed { i, metin ->
                        Sik(
                            HARFLER[i],
                            metin,
                            if (cevaplar[sira] == i) SikDurumu.SECILI else SikDurumu.NORMAL,
                        ) {
                            if (cevaplar[sira] == i) cevaplar.remove(sira) else cevaplar[sira] = i
                        }
                    }
                }
            }
        }
    }

    if (bitirSor) {
        val bos = sorular.size - cevaplar.size
        AlertDialog(
            onDismissRequest = { bitirSor = false },
            title = { Text("Sınavı bitir") },
            text = { Text(if (bos > 0) "$bos soruyu boş bıraktın. Yine de bitirmek istiyor musun?" else "Cevaplarını göndermek üzeresin.") },
            confirmButton = { TextButton(onClick = { bitirSor = false; bitir() }) { Text("Bitir") } },
            dismissButton = { TextButton(onClick = { bitirSor = false }) { Text("Devam et") } },
        )
    }
    if (cikSor) {
        AlertDialog(
            onDismissRequest = { cikSor = false },
            title = { Text("Sınavdan çık") },
            text = { Text("Çıkarsan bu denemedeki cevapların kaydedilmez.") },
            confirmButton = { TextButton(onClick = { cikSor = false; bitti = true; geri() }) { Text("Çık") } },
            dismissButton = { TextButton(onClick = { cikSor = false }) { Text("Devam et") } },
        )
    }
}

@Composable
private fun DenemeSonucu(
    sorular: List<Soru>,
    cevaplar: Map<Int, Int>,
    banka: SoruBankasi,
    geri: () -> Unit,
) {
    val dersPuanlari = banka.dersler.mapNotNull { ders ->
        val idx = sorular.indices.filter { sorular[it].ders == ders.kod }
        if (idx.isEmpty()) null
        else Triple(ders, idx.count { cevaplar[it] == sorular[it].dogru }, idx.size)
    }
    val puanlar = dersPuanlari.map { (_, d, t) -> yuzde(d, t) }
    val ortalama = if (puanlar.isEmpty()) 0 else Math.round(puanlar.average()).toInt()
    val gecti = puanlar.isNotEmpty() && puanlar.all { it >= 50 } && ortalama >= 60
    val incelenecek = sorular.indices.filter { cevaplar[it] != sorular[it].dogru }

    Scaffold(topBar = { UstCubuk("Deneme sonucu", geri) }, containerColor = Zemin) { ic ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(ic),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                KartKutu {
                    Etiket("Ortalama puan")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$ortalama", fontFamily = Baslik, fontSize = 52.sp, color = if (gecti) DogruYesil else YanlisKirmizi)
                        Text(
                            if (gecti) "Geçer" else "Kalır",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (gecti) DogruYesil else YanlisKirmizi,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    Text(
                        "Gerçek sınavdaki kurala göre: her dersten en az 50, ortalama en az 60.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinSoluk,
                    )
                }
            }
            items(dersPuanlari, key = { it.first.kod }) { (ders, d, t) ->
                val p = yuzde(d, t)
                KartKutu {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ders.kisaAd, style = MaterialTheme.typography.titleMedium)
                            Text("$t sorudan $d doğru", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                        }
                        Text("$p", style = MaterialTheme.typography.titleLarge, color = if (p >= 50) DogruYesil else YanlisKirmizi)
                    }
                    Bosluk(10)
                    Ilerleme(p / 100f)
                }
            }
            if (incelenecek.isNotEmpty()) {
                item { Etiket("Yanlış ve boş sorular", Modifier.padding(start = 4.dp, top = 8.dp)) }
                items(incelenecek, key = { "inc$it" }) { i ->
                    val s = sorular[i]
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KartKutu {
                            SoruMetni(s, "Soru ${i + 1} · ${banka.ders(s.ders)?.kisaAd ?: s.ders}")
                            cevaplar[i]?.let {
                                Bosluk(8)
                                Text("Senin cevabın: ${HARFLER[it]}) ${s.secenekler[it]}", style = MaterialTheme.typography.bodyMedium, color = YanlisKirmizi)
                            }
                        }
                        Aciklama(s, cevaplar[i])
                    }
                }
            }
            item {
                Button(onClick = geri, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Ana sayfaya dön") }
            }
        }
    }
}
