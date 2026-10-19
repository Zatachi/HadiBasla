package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
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

    Column(Modifier.fillMaxSize()) {
        SekmeBasligi("Profil")
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = ic.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KartKutu {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Yesil), contentAlignment = Alignment.Center) {
                        Text(basHarf(k?.ad.orEmpty()), color = Krem, fontSize = 22.sp)
                    }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(k?.ad.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (hesap.misafir) "Misafir hesap" else (hesap.eposta ?: "Google hesabı"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetinSoluk,
                        )
                    }
                    TextButton(onClick = { adDuzenle = true }) { Text("Adı düzenle") }
                }
                if (hesap.misafir) {
                    Bosluk(10)
                    Text(
                        "Misafir hesabın sadece bu telefonda. Google hesabına bağlarsan ilerlemen kalıcı olur ve başka telefonda da açılır.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetinSoluk,
                    )
                    Bosluk(8)
                    Button(
                        onClick = { calistir("Google hesabına bağlanıyor…") { hesap.googleIleGiris(baglam) } },
                        enabled = bekliyor == null,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("Google hesabına bağla") }
                }
            }

            KartKutu {
                k?.odulPremiumBitis?.takeIf { k.premiumAktif() }?.let {
                    Text("🎁 Ödül Premium: ${uzunTarih(it)} tarihine kadar", style = MaterialTheme.typography.bodyMedium, color = DogruYesil, modifier = Modifier.padding(vertical = 8.dp))
                    HorizontalDivider(color = Cizgi)
                }
                Satir("Sınavım", banka.lisans(k?.lisans)?.ad ?: "Seçilmedi") { y.lisans() }
                HorizontalDivider(color = Cizgi)
                Satir("Hedeflerim", "${depo.aktifHedefler().size} aktif · ${depo.hedefler.count { it.gecti }} geçildi") { y.hedefAc(null) }
                HorizontalDivider(color = Cizgi)
                Satir("Sınav sonuçlarım", "Gerçek sınav puanların ve lisans durumun") { y.sonuclar() }
                HorizontalDivider(color = Cizgi)
                Satir("Günlük hatırlatma", if (kayit.hatirlatmaAcik) "Açık · %02d:%02d".format(kayit.hatirlatmaSaati / 60, kayit.hatirlatmaSaati % 60) else "Kapalı") { y.hatirlatma() }
                HorizontalDivider(color = Cizgi)
                Satir("Verilerim", "Hakkında tutulan bilgiler") { y.verilerim() }
                HorizontalDivider(color = Cizgi)
                Satir("Aydınlatma metni", "Kişisel verilerin nasıl işlenir") { y.aydinlatma() }
                HorizontalDivider(color = Cizgi)
                Satir("Görüşünü yaz", "Uygulamayı birlikte geliştirelim") { y.gorus() }
                if (BuildConfig.PREMIUM_AKTIF) {
                    HorizontalDivider(color = Cizgi)
                    Satir("Premium", if (k?.premium == true) "Aktif" else "Tüm sorular ve denemeler") { y.premium() }
                }
            }

            KartKutu {
                if (hesap.misafir) {
                    Satir("Misafir hesabını sil", "İlerlemen ve tüm kayıtların silinir", YanlisKirmizi) { silSor = true }
                } else {
                    Satir("Çıkış yap", "Verilerin hesabında kalır") { cikisSor = true }
                    HorizontalDivider(color = Cizgi)
                    Satir("Hesabımı sil", "Hesabın ve tüm kayıtların kalıcı olarak silinir", YanlisKirmizi) { silSor = true }
                }
            }

            bekliyor?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Yesil)
                    Text(it, modifier = Modifier.padding(start = 10.dp))
                }
            }
            hata?.let { Text(it, color = YanlisKirmizi, style = MaterialTheme.typography.bodyMedium) }

            Text(
                "Lisansla ${BuildConfig.VERSION_NAME}\nBağımsız bir çalışma aracıdır; SPL veya SPK ile bağlantılı değildir.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = MetinSoluk,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
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
            title = { Text("Çıkış yap") },
            text = { Text("Verilerin Google hesabında saklanmaya devam eder. Tekrar girdiğinde kaldığın yerden devam edersin.") },
            confirmButton = {
                TextButton(onClick = { cikisSor = false; calistir("Çıkış yapılıyor…") { hesap.cikis(baglam) } }) { Text("Çıkış yap") }
            },
            dismissButton = { TextButton(onClick = { cikisSor = false }) { Text("Vazgeç") } },
        )
    }
    if (silSor) {
        AlertDialog(
            onDismissRequest = { silSor = false },
            title = { Text(if (hesap.misafir) "Misafir hesabını sil" else "Hesabını sil") },
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
                }) { Text("Kalıcı olarak sil", color = YanlisKirmizi) }
            },
            dismissButton = { TextButton(onClick = { silSor = false }) { Text("Vazgeç") } },
        )
    }
    cakisma?.let { HesapCakismasiPenceresi(it, hesap) { cakisma = null } }
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
                BilgiSatiri("Giriş yöntemi", if (hesap.misafir) "Misafir" else "Google")
                BilgiSatiri("Kullanıcı kimliği", (hesap.uid ?: "").take(10) + "…")
                Bosluk(6)
                Text(
                    "E-posta adresin Lisansla veritabanına kaydedilmez; yalnızca Google girişinde doğrulama için kullanılır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinSoluk,
                )
            }
            KartKutu {
                Etiket("Çalışma verilerin")
                Bosluk(8)
                BilgiSatiri("Cevapladığın soru", "${depo.cozulenler().size}")
                BilgiSatiri("Bugün çözülen", "${depo.bugunCozulen}")
                BilgiSatiri("Çalışma serisi", "${depo.seri()} gün")
                BilgiSatiri("Deneme sonucu", "${depo.denemeler.size}")
            }
            KartKutu {
                Etiket("Hedeflerin")
                Bosluk(8)
                if (depo.hedefler.isEmpty()) Text("Hedef yok.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                depo.hedefler.forEach { h ->
                    BilgiSatiri(banka.ders(h.ders)?.kisaAd ?: h.ders, if (h.gecti) "Geçildi" else h.sinavTarihi?.let { uzunTarih(it) } ?: "Tarih seçilmedi")
                }
            }
            KartKutu {
                Etiket("Geri bildirimlerin")
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
                Text("Premium ile tüm soru bankası", style = MaterialTheme.typography.headlineSmall)
                listOf(
                    "Tüm derslerde sınırsız soru",
                    "Sınırsız genel ve ders denemesi",
                    "Ayrıntılı konu istatistikleri",
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
