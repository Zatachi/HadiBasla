package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.AuthCredential
import com.lisanshazirlik.app.BuildConfig
import com.lisanshazirlik.app.data.Hesap
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.YerelKayit
import kotlinx.coroutines.launch

@Composable
fun ProfilSekme(
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    hesap: Hesap,
    kayit: YerelKayit,
    y: Yonlendirici,
    ic: PaddingValues,
) {
    val baglam = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val k = depo.kullanici
    var adDuzenle by remember { mutableStateOf(false) }
    var silSor by remember { mutableStateOf(false) }
    var cikisSor by remember { mutableStateOf(false) }
    var bekliyor by remember { mutableStateOf<String?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var cakisma by remember { mutableStateOf<AuthCredential?>(null) }

    fun calistir(mesaj: String, is_: suspend () -> Unit) {
        bekliyor = mesaj; hata = null
        kapsam.launch {
            try {
                is_()
            } catch (e: Hesap.Vazgecildi) {
            } catch (e: Hesap.ZatenKayitli) {
                cakisma = e.kimlik
            } catch (e: Hesap.HesapHatasi) {
                hata = e.message
            } catch (e: Exception) {
                hata = "İşlem tamamlanamadı. İnternet bağlantını kontrol edip tekrar dene."
            } finally {
                bekliyor = null
            }
        }
    }

    val seviye = com.lisanshazirlik.app.data.Oyun.seviye(k?.xp ?: 0)
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = ic.calculateBottomPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        // Kimlik kartı: yeşil bant içinde ad, seviye ve üç sayı
        Column(
            Modifier
                .fillMaxWidth()
                .background(Yesil)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Krem), contentAlignment = Alignment.Center) {
                    Text(basHarf(k?.ad.orEmpty()), color = Yesil, fontFamily = Baslik, fontSize = 28.sp)
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(k?.ad.orEmpty(), fontFamily = Baslik, fontSize = 24.sp, color = Krem, maxLines = 1)
                    Bosluk(4)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Rozetcik("${seviye.ad} · ${k?.xp ?: 0} XP", Altin, YesilKoyu)
                        Rozetcik(if (hesap.misafir) "Misafir" else "Google", YesilKoyu, KremSoluk)
                    }
                }
                Text(
                    "✎", color = Krem, fontSize = 20.sp,
                    modifier = Modifier.clip(CircleShape).background(YesilKoyu).clickableIf(true) { adDuzenle = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Bosluk(16)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfilSayi(Modifier.weight(1f), "🔥", "${depo.seri()}", "Gün Seri")
                ProfilSayi(Modifier.weight(1f), "✏️", "${depo.cozulenler().size}", "Soru")
                ProfilSayi(Modifier.weight(1f), "🏅", "${k?.rozetler?.size ?: 0}", "Rozet")
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hesap.misafir) {
                Column(
                    Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(Color(0xFFFBEFD7)).padding(16.dp),
                ) {
                    Text("⚠️ Hesabın Sadece Bu Telefonda", style = MaterialTheme.typography.titleMedium, color = Color(0xFF7A5A12))
                    Bosluk(2)
                    Text("Telefon değişirse ilerlemen kaybolur.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7A5A12))
                    Bosluk(10)
                    Button(
                        onClick = { calistir("Google hesabına bağlanıyor…") { hesap.googleIleGiris(baglam) } },
                        enabled = bekliyor == null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("Google ile Kalıcı Yap") }
                }
            }
            k?.odulPremiumBitis?.takeIf { k.premiumAktif() }?.let {
                Text("🎁 Ödül Premium · ${uzunTarih(it)} tarihine kadar", style = MaterialTheme.typography.labelLarge, color = DogruYesil)
            }

            Etiket("Çalışmam", Modifier.padding(start = 4.dp, top = 4.dp))
            Grup {
                MenuSatiri("🎓", "Sınavım", banka.lisans(k?.lisans)?.kisaAd ?: "Seç") { y.lisans() }
                MenuSatiri("🎯", "Hedeflerim", "${depo.aktifHedefler().size} Aktif") { y.hedefAc(null) }
                MenuSatiri("📜", "Sertifikalarım", null) { y.sonuclar() }
                MenuSatiri(
                    "🔔", "Günlük Hatırlatma",
                    if (kayit.hatirlatmaAcik) "%02d:%02d".format(kayit.hatirlatmaSaati / 60, kayit.hatirlatmaSaati % 60) else "Kapalı",
                    son = !BuildConfig.PREMIUM_AKTIF,
                ) { y.hatirlatma() }
                if (BuildConfig.PREMIUM_AKTIF) {
                    MenuSatiri("⭐", "Premium", if (k?.premium == true) "Aktif" else null, son = true) { y.premium() }
                }
            }

            Etiket("Gizlilik ve Destek", Modifier.padding(start = 4.dp, top = 4.dp))
            Grup {
                MenuSatiri("🗂", "Verilerim", null) { y.verilerim() }
                MenuSatiri("🔒", "Aydınlatma Metni", null) { y.aydinlatma() }
                MenuSatiri("💬", "Görüşünü Yaz", null, son = true) { y.gorus() }
            }

            Etiket("Hesap", Modifier.padding(start = 4.dp, top = 4.dp))
            Grup {
                if (hesap.misafir) {
                    MenuSatiri("🗑", "Misafir Hesabını Sil", null, tehlike = true, son = true) { silSor = true }
                } else {
                    MenuSatiri("↩️", "Çıkış Yap", null) { cikisSor = true }
                    MenuSatiri("🗑", "Hesabımı Sil", null, tehlike = true, son = true) { silSor = true }
                }
            }

            bekliyor?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Yesil)
                    Text(it, modifier = Modifier.padding(start = 10.dp))
                }
            }
            hata?.let { Text(it, color = YanlisKirmizi, style = MaterialTheme.typography.bodyMedium) }

            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lisansla ${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = MetinSoluk)
                Text("Bağımsız bir çalışma aracıdır.", fontSize = 12.sp, color = MetinSoluk)
            }
        }
    }

    if (adDuzenle) {
        var yeni by remember { mutableStateOf(k?.ad.orEmpty()) }
        AlertDialog(
            onDismissRequest = { adDuzenle = false },
            title = { Text("Adın") },
            text = {
                OutlinedTextField(value = yeni, onValueChange = { yeni = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = { depo.adGuncelle(yeni); adDuzenle = false }, enabled = yeni.isNotBlank()) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { adDuzenle = false }) { Text("Vazgeç") } },
        )
    }
    if (cikisSor) {
        AlertDialog(
            onDismissRequest = { cikisSor = false },
            title = { Text("Çıkış Yap") },
            text = { Text("Verilerin Google hesabında saklanmaya devam eder. Tekrar girdiğinde kaldığın yerden devam edersin.") },
            confirmButton = {
                TextButton(onClick = { cikisSor = false; calistir("Çıkış yapılıyor…") { hesap.cikis(baglam) } }) { Text("Çıkış Yap") }
            },
            dismissButton = { TextButton(onClick = { cikisSor = false }) { Text("Vazgeç") } },
        )
    }
    if (silSor) {
        AlertDialog(
            onDismissRequest = { silSor = false },
            title = { Text(if (hesap.misafir) "Misafir Hesabını Sil" else "Hesabını Sil") },
            text = {
                Text(
                    "Adın, çalışma ilerlemen, hedeflerin, deneme sonuçların, puanların, rozetlerin, sıralama ve davet kayıtların, oyların, hata bildirimlerin ve görüşlerin kalıcı olarak silinir. Bu işlem geri alınamaz." +
                        if (hesap.misafir) "" else "\n\nGüvenlik için Google hesabını bir kez daha seçmen istenecek."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    silSor = false
                    calistir("Hesabın siliniyor…") { hesap.hesabiSil(baglam); kayit.temizle() }
                }) { Text("Kalıcı Olarak Sil", color = YanlisKirmizi) }
            },
            dismissButton = { TextButton(onClick = { silSor = false }) { Text("Vazgeç") } },
        )
    }
    cakisma?.let { HesapCakismasiPenceresi(it, hesap) { cakisma = null } }
}

@Composable
private fun Rozetcik(yazi: String, zemin: Color, renk: Color) {
    Text(
        yazi, color = renk, style = MaterialTheme.typography.labelMedium, maxLines = 1,
        modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(zemin).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun ProfilSayi(modifier: Modifier, simge: String, deger: String, etiket: String) {
    Column(
        modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(YesilKoyu).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$simge $deger", fontFamily = Baslik, fontSize = 20.sp, color = Krem)
        Text(etiket, fontSize = 12.sp, color = KremSoluk)
    }
}

@Composable
private fun Grup(icerik: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(Kart)
            .border(1.dp, Cizgi, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
    ) { icerik() }
}

/** Menü satırı: simge, başlık, sağda kısa değer ve ok. */
@Composable
private fun MenuSatiri(simge: String, baslik: String, deger: String?, tehlike: Boolean = false, son: Boolean = false, tikla: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickableIf(true, tikla).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(if (tehlike) YanlisZemin else YesilAcik),
                contentAlignment = Alignment.Center,
            ) { Text(simge, fontSize = 17.sp) }
            Text(
                baslik, style = MaterialTheme.typography.titleMedium, color = if (tehlike) YanlisKirmizi else Metin,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            if (deger != null) Text(deger, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, maxLines = 1)
            if (!tehlike) Text("  ›", fontSize = 20.sp, color = MetinSoluk)
        }
        if (!son) HorizontalDivider(color = Cizgi, modifier = Modifier.padding(start = 64.dp))
    }
}

@Composable
private fun Satir(baslik: String, alt: String, renk: Color = Metin, tikla: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickableIf(true, tikla)
            .padding(vertical = 12.dp),
    ) {
        Text(baslik, style = MaterialTheme.typography.titleMedium, color = renk)
        Text(alt, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
    }
}

/** KVKK m.11: hakkında tutulan bilgileri gösterir. */
@Composable
fun VerilerimEkrani(banka: SoruBankasi, depo: KullaniciDeposu, hesap: Hesap, geri: () -> Unit) {
    val k = depo.kullanici
    Scaffold(topBar = { UstCubuk("Verilerim", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(ic)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Lisansla'da senin hakkında tutulan bilgilerin tamamı aşağıda. Bunların dışında bir bilgi saklanmaz.",
                style = MaterialTheme.typography.bodyLarge,
            )
            KartKutu {
                Etiket("Hesap")
                Bosluk(8)
                BilgiSatiri("Ad", k?.ad.orEmpty())
                BilgiSatiri("Giriş Yöntemi", if (hesap.misafir) "Misafir" else "Google")
                BilgiSatiri("Kullanıcı Kimliği", (hesap.uid ?: "").take(10) + "…")
                Bosluk(6)
                Text(
                    "E-posta adresin Lisansla veritabanına kaydedilmez; yalnızca Google girişinde doğrulama için kullanılır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinSoluk,
                )
            }
            KartKutu {
                Etiket("Çalışma Verilerin")
                Bosluk(8)
                BilgiSatiri("Cevapladığın Soru", "${depo.cozulenler().size}")
                BilgiSatiri("Bugün Çözülen", "${depo.bugunCozulen}")
                BilgiSatiri("Çalışma Serisi", "${depo.seri()} Gün")
                BilgiSatiri("Deneme Sonucu", "${depo.denemeler.size}")
            }
            KartKutu {
                Etiket("Hedeflerin")
                Bosluk(8)
                if (depo.hedefler.isEmpty()) Text("Hedef yok.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                depo.hedefler.forEach { h ->
                    BilgiSatiri(banka.ders(h.ders)?.kisaAd ?: h.ders, if (h.gecti) "Geçildi" else h.sinavTarihi?.let { uzunTarih(it) } ?: "Tarih Seçilmedi")
                }
            }
            KartKutu {
                Etiket("Geri Bildirimlerin")
                Bosluk(8)
                Text(
                    "Sorulara verdiğin oylar, hata bildirimlerin, görüşlerin ve hangi soruda hangi şıkkı seçtiğin, soruları iyileştirmek için kullanıcı kimliğinle birlikte saklanır. Hesabını silersen bunlar da silinir.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "Düzeltme, silme ya da başka bir talebin için: ${BuildConfig.EPOSTA}",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
            )
        }
    }
}

/** Premium altyapısı: PREMIUM_AKTIF=false iken bu ekrana ulaşılamaz. Ödeme entegrasyonu ileride eklenecek. */
@Composable
fun PremiumEkrani(depo: KullaniciDeposu, geri: () -> Unit) {
    Scaffold(topBar = { UstCubuk("Premium", geri) }, containerColor = Zemin) { ic ->
        Column(
            Modifier.fillMaxSize().padding(ic).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (depo.kullanici?.premium == true) {
                Text("Premium üyeliğin aktif.", style = MaterialTheme.typography.headlineSmall)
            } else {
                Text("Premium ile Tüm Soru Bankası", style = MaterialTheme.typography.headlineSmall)
                listOf(
                    "Tüm Derslerde Sınırsız Soru",
                    "Sınırsız Genel ve Ders Denemesi",
                    "Ayrıntılı Konu İstatistikleri",
                ).forEach { Text("•  $it", style = MaterialTheme.typography.bodyLarge) }
                Bosluk(8)
                Text("Satın alma yakında açılacak.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
            }
        }
    }
}

/** İleride ücretli özellikleri kısıtlamak için tek kontrol noktası. Şu an her şey herkese açık. */
object Premium {
    fun erisimVar(depo: KullaniciDeposu): Boolean = !BuildConfig.PREMIUM_AKTIF || depo.kullanici?.premiumAktif() == true
}
