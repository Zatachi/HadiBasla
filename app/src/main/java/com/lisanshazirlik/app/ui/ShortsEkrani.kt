package com.lisanshazirlik.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lisanshazirlik.app.R
import com.lisanshazirlik.app.data.Kisa
import com.lisanshazirlik.app.data.KisaSosyal
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Odak
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.Yorum
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KALP_KIRMIZI = Color(0xFFFF3040)
private val SAYFA_KOYU = Color(0xFF1B1B1B)
private val SAYFA_CIZGI = Color(0xFF363636)
private val SAYFA_SOLUK = Color(0xFFA8A8A8)

/**
 * Akış sırası, seçim gerektirmez: önce odak dersin dizisi (bölüm sırasıyla), sonra lisansın diğer
 * dersleri, en sonda diğer bütün kısa videolar. Her grupta izlenmemişler öne alınır.
 */
fun shortsAkisi(banka: SoruBankasi, depo: KullaniciDeposu): List<Kisa> {
    val odak = Odak.ders(banka, depo)?.kod
    val lisans = banka.lisansDersleri(depo.kullanici?.lisans).map { it.kod }.toSet()
    val izlenen = depo.kullanici?.izlenenShort.orEmpty().toSet()
    return banka.kisalar.sortedWith(
        compareBy<Kisa>(
            { if (it.ders == odak) 0 else if (it.ders in lisans) 1 else 2 },
            { if (it.ders == odak) 0 else if (it.id in izlenen) 1 else 0 },
            { it.ders },
            { it.dizi.isEmpty() },
            { it.dizi },
            { it.bolum },
        )
    )
}

/** Shorts: açılır açılmaz oynar. Dikey kaydır, çift dokunuşla beğen, yorumlar alttan açılır. */
@Composable
fun ShortsEkrani(
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    konuAc: (String) -> Unit,
    takmaAdSec: () -> Unit,
    geri: () -> Unit,
) {
    var kayitliMod by remember { mutableStateOf(false) }
    val k = depo.kullanici
    val tum = remember(banka, k?.lisans) { shortsAkisi(banka, depo) }
    val liste = if (kayitliMod) tum.filter { it.id in k?.kaydedilenler.orEmpty() } else tum
    // Odak dizide izlenmemiş ilk bölümden başla.
    val baslangic = remember(kayitliMod) { liste.indexOfFirst { it.id !in k?.izlenenShort.orEmpty() }.coerceAtLeast(0) }
    val pager = rememberPagerState(initialPage = baslangic) { liste.size }
    var yorumlarAcik by remember { mutableStateOf<Kisa?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (liste.isEmpty()) {
            Text(
                if (kayitliMod) "Henüz Kaydettiğin Video Yok. Beğendiğin Videoda Kaydet Simgesine Dokun."
                else "Kısa Videolar Hazırlanıyor.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            VerticalPager(pager, modifier = Modifier.fillMaxSize(), key = { liste[it].id }, beyondViewportPageCount = 1) { i ->
                val kisa = liste[i]
                KisaVideo(
                    kisa = kisa,
                    depo = depo,
                    dersAd = banka.ders(kisa.ders)?.kisaAd.orEmpty(),
                    konuAd = banka.ders(kisa.ders)?.konular?.firstOrNull { it.id == kisa.konu }?.ad,
                    aktif = pager.settledPage == i && yorumlarAcik == null,
                    yorumAc = { yorumlarAcik = kisa },
                    konuyaGit = { konuAc(kisa.konu) },
                )
            }
        }

        // Üst: başlık solda, kaydedilenler ve kapat sağda
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0x80000000), Color.Transparent)))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (kayitliMod) "Kaydedilenler" else "Shorts",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            UstSimge(if (kayitliMod) R.drawable.ic_r_kaydet_dolu else R.drawable.ic_r_kaydet) { kayitliMod = !kayitliMod }
            Text("✕", color = Color.White, fontSize = 22.sp, modifier = Modifier.clip(CircleShape).clickable(onClick = geri).padding(10.dp))
        }
    }

    yorumlarAcik?.let { kisa ->
        YorumSayfasi(kisa, depo, takmaAdSec = { yorumlarAcik = null; takmaAdSec() }, kapat = { yorumlarAcik = null })
    }
}

@Composable
private fun UstSimge(resim: Int, tikla: () -> Unit) {
    Icon(
        painterResource(resim), contentDescription = null, tint = Color.White,
        modifier = Modifier.clip(CircleShape).clickable(onClick = tikla).padding(10.dp).size(24.dp),
    )
}

@Composable
private fun KisaVideo(
    kisa: Kisa,
    depo: KullaniciDeposu,
    dersAd: String,
    konuAd: String?,
    aktif: Boolean,
    yorumAc: () -> Unit,
    konuyaGit: () -> Unit,
) {
    val baglam = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val k = depo.kullanici
    val begenildi = kisa.id in k?.begeniler.orEmpty()
    val kaydedildi = kisa.id in k?.kaydedilenler.orEmpty()

    var oynatici by remember { mutableStateOf<VideoView?>(null) }
    var hazir by remember { mutableStateOf(false) }
    var durdu by remember { mutableStateOf(false) }
    var sayildi by remember { mutableStateOf(false) }
    var ilerleme by remember { mutableStateOf(0f) }
    var acik by remember { mutableStateOf(false) }
    var begeni by remember { mutableLongStateOf(-1L) }
    var yorum by remember { mutableLongStateOf(-1L) }
    val buyukKalp = remember { Animatable(0f) }
    DisposableEffect(Unit) { onDispose { oynatici?.stopPlayback() } }

    LaunchedEffect(kisa.id, aktif) {
        if (!aktif) return@LaunchedEffect
        runCatching { KisaSosyal.begeniSayisi(kisa.id) }.onSuccess { begeni = it }
        runCatching { KisaSosyal.yorumSayisi(kisa.id) }.onSuccess { yorum = it }
    }

    fun begen(ekle: Boolean) {
        if (ekle == begenildi) return
        depo.begen(kisa.id, ekle)
        KisaSosyal.begen(depo.uid, kisa.id, ekle)
        if (begeni >= 0) begeni += if (ekle) 1 else -1
    }

    // Oynatma: aktifken baştan oynar, bitince başa döner (döngü). %70 izlenince sayılır.
    LaunchedEffect(aktif, hazir) {
        val v = oynatici ?: return@LaunchedEffect
        if (!hazir) return@LaunchedEffect
        if (!aktif) { v.pause(); return@LaunchedEffect }
        v.seekTo((kisa.bas * 1000).toInt()); v.start(); durdu = false
        while (true) {
            delay(200)
            val p = v.currentPosition
            ilerleme = ((p - kisa.bas * 1000) / ((kisa.son - kisa.bas) * 1000)).toFloat().coerceIn(0f, 1f)
            if (!sayildi && ilerleme >= 0.7f) { sayildi = true; depo.shortIzlendi(kisa.id) }
            if (p >= kisa.son * 1000) { v.seekTo((kisa.bas * 1000).toInt()); if (!durdu) v.start() }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                VideoView(c).apply {
                    setOnPreparedListener { hazir = true }
                    setVideoURI(Uri.parse(kisa.video))
                    oynatici = this
                }
            },
        )
        // Dokunma katmanı: tek dokunuş durdur/oynat, çift dokunuş beğen.
        Box(
            Modifier.fillMaxSize().pointerInput(kisa.id, begenildi) {
                detectTapGestures(
                    onTap = {
                        oynatici?.let { v -> if (v.isPlaying) { v.pause(); durdu = true } else { v.start(); durdu = false } }
                    },
                    onDoubleTap = {
                        begen(true)
                        kapsam.launch {
                            buyukKalp.snapTo(0f)
                            buyukKalp.animateTo(1.2f, tween(180))
                            buyukKalp.animateTo(1f, tween(120))
                            delay(500)
                            buyukKalp.animateTo(0f, tween(200))
                        }
                    },
                )
            },
        )
        if (!hazir) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.align(Alignment.Center).size(36.dp))
        if (durdu) {
            Box(Modifier.align(Alignment.Center).size(72.dp).clip(CircleShape).background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                Text("▶", color = Color.White, fontSize = 32.sp)
            }
        }
        if (buyukKalp.value > 0f) {
            Icon(
                painterResource(R.drawable.ic_r_kalp_dolu), contentDescription = null, tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(110.dp).scale(buyukKalp.value),
            )
        }

        // Alt karartma: yazılar her videoda okunsun
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.4f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000)))),
        )

        // Sağ sütun: beğen, yorum, paylaş, kaydet, daha fazla
        Column(
            Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBars).padding(end = 8.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            YanDugme(
                if (begenildi) R.drawable.ic_r_kalp_dolu else R.drawable.ic_r_kalp,
                if (begenildi) KALP_KIRMIZI else Color.White,
                sayiYazisi(begeni),
            ) { begen(!begenildi) }
            YanDugme(R.drawable.ic_r_yorum, Color.White, sayiYazisi(yorum), tikla = yorumAc)
            YanDugme(R.drawable.ic_r_gonder, Color.White, null) {
                val niyet = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Lisansla'da bu kısa videoyu izle: \"${kisa.baslik}\"\nhttps://github.com/Zatachi/HadiBasla/releases/latest/download/Lisansla.apk")
                baglam.startActivity(Intent.createChooser(niyet, null))
                depo.paylasildi()
            }
            YanDugme(if (kaydedildi) R.drawable.ic_r_kaydet_dolu else R.drawable.ic_r_kaydet, Color.White, null) { depo.kaydet(kisa.id, !kaydedildi) }
            YanDugme(R.drawable.ic_r_daha, Color.White, null, tikla = konuyaGit)
            // Köşede dönen kare yerine ders kodu: dokununca konuya gider
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Yesil).border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .clickable(onClick = konuyaGit),
                contentAlignment = Alignment.Center,
            ) { Text(kisa.ders.takeLast(2), color = Krem, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }

        // Sol alt: hesap, açıklama, ses satırı
        Column(
            Modifier.align(Alignment.BottomStart).windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 14.dp, end = 72.dp, bottom = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Yesil).border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(34.dp))
                }
                Text("Lisansla", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                Text(" ✓", color = Color(0xFF4CB5F9), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (kisa.dizi.isNotBlank()) {
                    Text(
                        "${kisa.bolum}/${kisa.toplam}",
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 10.dp).border(1.dp, Color(0xB3FFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Bosluk(10)
            Text(
                buildAnnotatedString {
                    if (kisa.dizi.isNotBlank()) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(kisa.dizi + " · ") }
                    append(kisa.baslik)
                    if (acik) {
                        if (kisa.merak.isNotBlank()) append("\n\nSıradaki Bölümde: " + kisa.merak)
                        append("\n\n#" + dersAd.replace(" ", ""))
                        if (konuAd != null) append(" #" + konuAd.replace(" ", ""))
                    }
                },
                color = Color.White, fontSize = 14.sp, lineHeight = 19.sp,
                maxLines = if (acik) 12 else 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { acik = !acik },
            )
            if (!acik) Text("Devamı", color = SAYFA_SOLUK, fontSize = 13.sp, modifier = Modifier.clickable { acik = true })
            Bosluk(10)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("♫", color = Color.White, fontSize = 12.sp)
                Text(
                    "Lisansla · Özgün Anlatım",
                    color = Color.White, fontSize = 12.sp, maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        // En altta ince ilerleme çizgisi
        Box(Modifier.align(Alignment.BottomStart).windowInsetsPadding(WindowInsets.navigationBars).fillMaxWidth().height(2.dp).background(Color(0x33FFFFFF))) {
            Box(Modifier.fillMaxWidth(ilerleme).height(2.dp).background(Color.White))
        }
    }
}

private fun sayiYazisi(n: Long): String? = when {
    n < 0 -> ""
    n == 0L -> null
    n < 1000 -> "$n"
    n < 1_000_000 -> "%.1f B".format(n / 1000f).replace(".0 ", " ")
    else -> "%.1f Mn".format(n / 1_000_000f).replace(".0 ", " ")
}

@Composable
private fun YanDugme(resim: Int, renk: Color, sayi: String?, tikla: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = tikla).padding(4.dp)) {
        Icon(painterResource(resim), contentDescription = null, tint = renk, modifier = Modifier.size(30.dp))
        if (sayi != null) {
            Bosluk(3)
            Text(sayi, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------- yorumlar

private val EMOJILER = listOf("❤️", "🙌", "🔥", "👏", "😢", "😍", "😮", "😂")

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun YorumSayfasi(kisa: Kisa, depo: KullaniciDeposu, takmaAdSec: () -> Unit, kapat: () -> Unit) {
    val k = depo.kullanici
    val kapsam = rememberCoroutineScope()
    val durum = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var yorumlar by remember { mutableStateOf<List<Yorum>?>(null) }
    var yazi by remember { mutableStateOf("") }
    var gonderiliyor by remember { mutableStateOf(false) }
    var hata by remember { mutableStateOf<String?>(null) }
    var secili by remember { mutableStateOf<Yorum?>(null) }
    val adVar = k?.siralamaKatil == true && k.takmaAd.isNotBlank()

    suspend fun yukle() {
        yorumlar = runCatching { KisaSosyal.yorumlar(kisa.id) }.getOrElse { emptyList() }
    }
    LaunchedEffect(kisa.id) { yukle() }

    ModalBottomSheet(
        onDismissRequest = kapat,
        sheetState = durum,
        containerColor = SAYFA_KOYU,
        contentColor = Color.White,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF5C5C5C)))
        },
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f).imePadding()) {
            Text("Yorumlar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(SAYFA_CIZGI))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val liste = yorumlar
                when {
                    liste == null -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.align(Alignment.Center).size(28.dp))
                    liste.isEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Henüz Yorum Yok", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Bosluk(6)
                        Text("Sohbeti Sen Başlat.", color = SAYFA_SOLUK, fontSize = 14.sp)
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(liste, key = { it.id }) { y ->
                            val begendim = depo.uid in y.begenenler
                            YorumSatiri(
                                y, begendim,
                                begen = {
                                    KisaSosyal.yorumBegen(y.id, depo.uid, !begendim)
                                    yorumlar = yorumlar?.map {
                                        if (it.id != y.id) it
                                        else it.copy(begenenler = if (begendim) it.begenenler - depo.uid else it.begenenler + depo.uid)
                                    }
                                },
                                uzunBas = { secili = y },
                            )
                        }
                    }
                }
            }

            // Hızlı emojiler ve yazma satırı
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(SAYFA_CIZGI))
            if (adVar) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    EMOJILER.forEach { e -> Text(e, fontSize = 24.sp, modifier = Modifier.clip(CircleShape).clickable { yazi += e }.padding(2.dp)) }
                }
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(start = 14.dp, end = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasHarf(k!!.takmaAd, 36.dp)
                    Box(
                        Modifier.weight(1f).padding(start = 10.dp).clip(RoundedCornerShape(22.dp)).border(1.dp, SAYFA_CIZGI, RoundedCornerShape(22.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        if (yazi.isEmpty()) Text("${k.takmaAd} Olarak Yorum Ekle…", color = SAYFA_SOLUK, fontSize = 14.sp, maxLines = 1)
                        BasicTextField(
                            value = yazi,
                            onValueChange = { yazi = it.take(KisaSosyal.YORUM_SINIR); hata = null },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color.White),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        "Paylaş",
                        color = if (yazi.isNotBlank() && !gonderiliyor) Color(0xFF0095F6) else Color(0x660095F6),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = yazi.isNotBlank() && !gonderiliyor) {
                            gonderiliyor = true
                            kapsam.launch {
                                runCatching { KisaSosyal.yorumYaz(depo.uid, k.takmaAd, kisa.id, yazi) }
                                    .onSuccess { yazi = ""; yukle() }
                                    .onFailure { hata = "Yorum Gönderilemedi. İnternet Bağlantını Kontrol Et." }
                                gonderiliyor = false
                            }
                        }.padding(10.dp),
                    )
                }
                hata?.let { Text(it, color = KALP_KIRMIZI, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) }
            } else {
                Column(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Yorum Yazmak İçin Bir Takma Ad Seç. Gerçek Adın Görünmez.", color = SAYFA_SOLUK, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Bosluk(10)
                    Text(
                        "Takma Ad Seç",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0095F6)).clickable(onClick = takmaAdSec)
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }

    secili?.let { y ->
        val benim = y.uid == depo.uid
        AlertDialog(
            onDismissRequest = { secili = null },
            title = { Text(if (benim) "Yorumu Sil" else "Yorumu Bildir") },
            text = {
                Text(
                    if (benim) "Bu Yorum Kalıcı Olarak Silinecek."
                    else "Uygunsuz, Yanıltıcı ya da Saldırgan Bir Yorumsa Bildir. ${KisaSosyal.GIZLEME_SIKAYET} Bildirim Alan Yorum Gizlenir."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (benim) KisaSosyal.yorumSil(y.id) else KisaSosyal.bildir(y.id, depo.uid)
                    yorumlar = yorumlar?.filterNot { it.id == y.id }
                    secili = null
                }) { Text(if (benim) "Sil" else "Bildir", color = YanlisKirmizi) }
            },
            dismissButton = { TextButton(onClick = { secili = null }) { Text("Vazgeç") } },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun YorumSatiri(y: Yorum, begendim: Boolean, begen: () -> Unit, uzunBas: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = uzunBas).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BasHarf(y.takmaAd, 34.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(y.takmaAd, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("  ${kisaZaman(y.zaman)}", color = SAYFA_SOLUK, fontSize = 12.sp)
            }
            Bosluk(2)
            Text(y.metin, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp)
            Bosluk(6)
            Text("Silmek ya da Bildirmek İçin Basılı Tut", color = Color(0xFF6E6E6E), fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(CircleShape).clickable(onClick = begen).padding(4.dp)) {
            Icon(
                painterResource(if (begendim) R.drawable.ic_r_kalp_dolu else R.drawable.ic_r_kalp),
                contentDescription = null, tint = if (begendim) KALP_KIRMIZI else SAYFA_SOLUK,
                modifier = Modifier.size(16.dp),
            )
            if (y.begenenler.isNotEmpty()) Text("${y.begenenler.size}", color = SAYFA_SOLUK, fontSize = 11.sp)
        }
    }
}

/** Takma adın baş harfi, addan türetilen sabit renkte yuvarlak içinde. */
@Composable
private fun BasHarf(ad: String, boyut: androidx.compose.ui.unit.Dp) {
    val renkler = listOf(Color(0xFF833AB4), Color(0xFFC13584), Color(0xFFE1306C), Color(0xFFF77737), Color(0xFF2C7A5A), Color(0xFF3B5998), Color(0xFF8A5A1A))
    val renk = renkler[(ad.hashCode() and 0x7fffffff) % renkler.size]
    Box(Modifier.size(boyut).clip(CircleShape).background(renk), contentAlignment = Alignment.Center) {
        Text(ad.take(1).uppercase(java.util.Locale.forLanguageTag("tr")), color = Color.White, fontSize = (boyut.value * 0.42f).sp, fontWeight = FontWeight.Bold)
    }
}

private fun kisaZaman(zaman: Long): String {
    val dk = (System.currentTimeMillis() - zaman) / 60_000
    return when {
        dk < 1 -> "Şimdi"
        dk < 60 -> "$dk dk"
        dk < 60 * 24 -> "${dk / 60} sa"
        dk < 60 * 24 * 7 -> "${dk / (60 * 24)} g"
        else -> "${dk / (60 * 24 * 7)} hf"
    }
}
