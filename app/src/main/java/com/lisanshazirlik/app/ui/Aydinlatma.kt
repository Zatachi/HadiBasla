package com.lisanshazirlik.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lisanshazirlik.app.BuildConfig
import com.lisanshazirlik.app.R

/** res/raw/aydinlatma.txt dosyasını, yer tutucuları doldurarak okur. */
fun aydinlatmaMetni(baglam: Context): String =
    runCatching {
        baglam.resources.openRawResource(R.raw.aydinlatma).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrDefault("Aydınlatma metni yüklenemedi.")
        .replace("{{AD_SOYAD}}", BuildConfig.AD_SOYAD)
        .replace("{{EPOSTA}}", BuildConfig.EPOSTA)
        .replace("{{TARIH}}", BuildConfig.METIN_TARIHI)

@Composable
fun AydinlatmaGovdesi(modifier: Modifier = Modifier) {
    val baglam = LocalContext.current
    val metin = remember { aydinlatmaMetni(baglam) }
    BasitMetin(metin, modifier)
}

/** Basit biçimli metin: '# ' başlık, '## ' alt başlık, '- ' madde. Konu özetlerinde de kullanılır. */
@Composable
fun BasitMetin(metin: String, modifier: Modifier = Modifier) {
    val satirlar = remember(metin) { metin.lines() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        satirlar.forEach { ham ->
            val s = ham.trim()
            when {
                s.isEmpty() -> {}
                s.startsWith("## ") -> {
                    Bosluk(8)
                    Text(s.removePrefix("## "), style = MaterialTheme.typography.titleLarge)
                }
                s.startsWith("# ") -> Text(s.removePrefix("# "), style = MaterialTheme.typography.headlineSmall)
                s.startsWith("- ") -> Row {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = Yesil, modifier = Modifier.padding(end = 8.dp))
                    Text(s.removePrefix("- "), style = MaterialTheme.typography.bodyMedium)
                }
                else -> Text(s, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun AydinlatmaEkrani(geri: () -> Unit) {
    Scaffold(topBar = { UstCubuk("Aydınlatma metni", geri) }, containerColor = Zemin) { ic ->
        AydinlatmaGovdesi(
            Modifier
                .fillMaxSize()
                .padding(ic)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        )
    }
}
