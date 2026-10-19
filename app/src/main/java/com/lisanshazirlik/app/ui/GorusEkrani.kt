package com.lisanshazirlik.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.GeriBildirim

@Composable
fun GorusEkrani(geri: () -> Unit) {
    var puan by remember { mutableIntStateOf(0) }
    var metin by remember { mutableStateOf("") }
    var gonderildi by remember { mutableStateOf(false) }

    Scaffold(topBar = { UstCubuk("Görüşünü yaz", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(ic)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (gonderildi) {
                Text("Teşekkürler!", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Görüşün bize ulaştı. Her yorumu okuyup uygulamayı ve soruları buna göre geliştiriyoruz.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Bosluk(12)
                Button(onClick = geri, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Ana sayfaya dön") }
                return@Column
            }
            Text(
                "Uygulamayı nasıl buldun? Eksik gördüğün bir konu, beğenmediğin bir soru tarzı ya da istediğin bir özellik varsa yaz.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Etiket("Puanın")
            Row {
                (1..5).forEach { y ->
                    TextButton(onClick = { puan = y }) {
                        Text(if (y <= puan) "★" else "☆", fontSize = 30.sp, color = if (y <= puan) Altin else MetinSoluk)
                    }
                }
            }
            OutlinedTextField(
                value = metin,
                onValueChange = { metin = it.take(2000) },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Görüşün") },
            )
            Text(
                "İsmin ya da iletişim bilgin alınmaz; görüşün anonim olarak iletilir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
            )
            Button(
                onClick = {
                    GeriBildirim.gorus(metin.trim(), puan)
                    gonderildi = true
                },
                enabled = metin.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Gönder") }
        }
    }
}
