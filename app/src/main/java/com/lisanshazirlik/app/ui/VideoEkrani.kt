package com.lisanshazirlik.app.ui

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
fun VideoEkrani(baslik: String, adres: String, geri: () -> Unit, bitti: (() -> Unit)? = null) {
    var izlendi by remember { mutableStateOf(false) }
    var yukleniyor by remember { mutableStateOf(true) }
    var hata by remember { mutableStateOf(false) }
    var oynatici by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose { oynatici?.stopPlayback() }
    }

    Scaffold(
        topBar = { UstCubuk(baslik, geri) },
        bottomBar = {
            if (bitti != null) {
                androidx.compose.material3.Button(
                    onClick = bitti,
                    modifier = Modifier.fillMaxWidth().background(Color.Black).padding(16.dp).height(52.dp),
                ) { Text(if (izlendi) "Devam et: sorulara geç" else "İzledim, devam et") }
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
