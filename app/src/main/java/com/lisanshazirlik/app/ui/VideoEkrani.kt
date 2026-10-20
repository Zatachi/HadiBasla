package com.lisanshazirlik.app.ui

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Konu videosunu internetten akışla oynatır. Dokununca oynat/durdur ve ileri-geri sarma çubuğu çıkar. */
@Composable
fun VideoEkrani(
    baslik: String,
    adres: String,
    geri: () -> Unit,
    bitti: (() -> Unit)? = null,
    /** Video içi sorular: video o saniyeye gelince durur ve sorar. */
    sorular: List<Pair<com.lisanshazirlik.app.data.VideoSorusu, com.lisanshazirlik.app.data.Soru>> = emptyList(),
    cevapla: ((com.lisanshazirlik.app.data.Soru, Boolean) -> Unit)? = null,
) {
    val cevaplanan = remember { mutableStateListOf<String>() }
    var soruAni by remember { mutableStateOf<Pair<com.lisanshazirlik.app.data.VideoSorusu, com.lisanshazirlik.app.data.Soru>?>(null) }
    var secim by remember { mutableStateOf<Int?>(null) }
    var izlendi by remember { mutableStateOf(false) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hata by remember { mutableStateOf(false) }
    var oynatici by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose { oynatici?.stopPlayback() }
    }
    LaunchedEffect(oynatici, sorular) {
        if (sorular.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(250)
            val v = oynatici ?: continue
            if (soruAni != null || !v.isPlaying) continue
            val p = v.currentPosition / 1000.0
            val siradaki = sorular.firstOrNull { (vs, s) -> s.id !in cevaplanan && p >= vs.saniye && p < vs.saniye + 3 }
            if (siradaki != null) { v.pause(); secim = null; soruAni = siradaki }
        }
    }

    Scaffold(
        topBar = { UstCubuk(baslik, geri) },
        bottomBar = {
            if (bitti != null) {
                androidx.compose.material3.Button(
                    onClick = bitti,
                    modifier = Modifier.fillMaxWidth().background(Color.Black).padding(16.dp).height(52.dp),
                ) { Text(if (izlendi) "Devam Et: Sorulara Geç" else "İzledim, Devam Et") }
            }
        },
        containerColor = Color.Black,
    ) { ic ->
        Box(Modifier.fillMaxSize().padding(ic).background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { b ->
                    VideoView(b).apply {
                        val kontrol = MediaController(b)
                        kontrol.setAnchorView(this)
                        setMediaController(kontrol)
                        setOnPreparedListener { yukleniyor = false; start() }
                        setOnCompletionListener { izlendi = true }
                        setOnErrorListener { _, _, _ -> yukleniyor = false; hata = true; true }
                        setVideoURI(Uri.parse(adres))
                        oynatici = this
                    }
                },
            )
            if (yukleniyor) CircularProgressIndicator(color = Altin)
            soruAni?.let { (vs, soru) ->
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .background(Zemin, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("⏸ Video Durdu: Bakalım Aklında Kaldı Mı?", color = Yesil, style = MaterialTheme.typography.labelLarge)
                    Text(soru.metin, style = MaterialTheme.typography.titleMedium)
                    soru.secenekler.forEachIndexed { i, m ->
                        val durum = when {
                            secim == null -> SikDurumu.NORMAL
                            i == soru.dogru -> SikDurumu.DOGRU
                            i == secim -> SikDurumu.YANLIS
                            else -> SikDurumu.SOLUK
                        }
                        Sik(HARFLER[i], m, durum, if (secim == null) ({ secim = i; cevapla?.invoke(soru, i == soru.dogru) }) else null)
                    }
                    val sec = secim
                    if (sec != null) {
                        if (sec == soru.dogru) {
                            Text("✓ Doğru! Devam edelim.", color = DogruYesil, style = MaterialTheme.typography.titleMedium)
                            androidx.compose.material3.Button(
                                onClick = { cevaplanan += soru.id; soruAni = null; oynatici?.start() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                            ) { Text("▶ Videoya Devam") }
                        } else {
                            Text("✗ Olmadı. O kısmı bir daha izleyelim.", color = YanlisKirmizi, style = MaterialTheme.typography.titleMedium)
                            androidx.compose.material3.Button(
                                onClick = { soruAni = null; oynatici?.seekTo((vs.geri * 1000).toInt()); oynatici?.start() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                            ) { Text("⟲ O Kısmı Tekrar İzle") }
                            androidx.compose.material3.TextButton(
                                onClick = { cevaplanan += soru.id; soruAni = null; oynatici?.start() },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Yine de Devam Et") }
                        }
                    }
                }
            }
            if (hata) {
                Text(
                    "Video açılamadı. İnternet bağlantını kontrol edip tekrar dene.",
                    color = Krem,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
    }
}
