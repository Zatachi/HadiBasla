package com.lisanshazirlik.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.AuthCredential
import com.lisanshazirlik.app.R
import com.lisanshazirlik.app.data.Hesap
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.SoruBankasi
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
private fun YesilUst(baslik: String, alt: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Yesil)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(72.dp))
        Text(baslik, fontFamily = Baslik, fontSize = 30.sp, lineHeight = 36.sp, color = Krem)
        Box(Modifier.padding(vertical = 8.dp).width(48.dp).height(2.dp).background(Altin))
        Text(alt, style = MaterialTheme.typography.bodyLarge, color = KremSoluk)
    }
}

/** İlk açılış: tanıtım ve aydınlatma metni. Bu adım geçilmeden hiçbir veri işlenmez. */
@Composable
fun KarsilamaEkrani(devam: () -> Unit) {
    var okudum by remember { mutableStateOf(false) }
    var metinAcik by remember { mutableStateOf(false) }
    if (metinAcik) {
        AydinlatmaEkrani(geri = { metinAcik = false })
        return
    }
    Column(Modifier.fillMaxSize().background(Zemin).navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            YesilUst("Lisansla'ya Hoş Geldin", "Sermaye Piyasası Lisans Sınavlarına Adım Adım Hazırlan.")
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Ozellik("Çalıştığın Dersi ve Sınav Tarihini Seç, Kaç Gün Kaldığını Takip Et.")
                Ozellik("Her Gün Sana Uygun Sayıda Soru Çöz, Serini Bozma.")
                Ozellik("Yanlışlarını ve Zayıf Konularını Tekrar Et, Deneme Sınavlarıyla Kendini Ölç.")
                Bosluk(4)
                KartKutu {
                    Text("Kişisel Verilerin", style = MaterialTheme.typography.titleMedium)
                    Bosluk(6)
                    Text(
                        "Sadece adını ve çalışma ilerlemeni saklarız. E-posta adresin kaydedilmez, reklam yoktur, verilerin satılmaz. Hesabını istediğin an silebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { metinAcik = true }, modifier = Modifier.padding(start = 0.dp)) {
                        Text("Aydınlatma Metnini Oku")
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { okudum = !okudum },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = okudum, onCheckedChange = { okudum = it })
                    Text("Kişisel Verilerin Korunmasına İlişkin Aydınlatma Metnini Okudum.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Button(
            onClick = devam,
            enabled = okudum,
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp),
        ) { Text("Devam Et") }
    }
}

@Composable
private fun Ozellik(metin: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 8.dp, end = 12.dp).size(8.dp).background(Altin))
        Text(metin, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Google ile giriş ya da misafir olarak deneme. */
@Composable
fun GirisEkrani(hesap: Hesap, girildi: (googleAdi: String?) -> Unit) {
    val baglam = LocalContext.current
    val kapsam = rememberCoroutineScope()
    var bekliyor by remember { mutableStateOf(false) }
    var hata by remember { mutableStateOf<String?>(null) }

    fun calistir(is_: suspend () -> String?) {
        bekliyor = true; hata = null
        kapsam.launch {
            try {
                girildi(is_())
            } catch (e: Hesap.Vazgecildi) {
            } catch (e: Hesap.HesapHatasi) {
                hata = e.message
            } catch (e: Exception) {
                hata = "Giriş yapılamadı. İnternet bağlantını kontrol edip tekrar dene."
            } finally {
                bekliyor = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Zemin).navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            YesilUst("Hesabını Oluştur", "İlerlemen Hesabında Saklanır; Telefon Değiştirsen de Kaybolmaz.")
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { calistir { hesap.googleIleGiris(baglam) } },
                    enabled = !bekliyor,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text("Google ile Devam Et") }
                OutlinedButton(
                    onClick = { calistir { hesap.misafirGiris(); null } },
                    enabled = !bekliyor,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text("Misafir Olarak Dene") }
                Text(
                    "Misafir olarak başlarsan ilerlemen bu telefondaki misafir hesabında tutulur. İstediğin zaman Profil'den Google hesabına bağlayıp kalıcı hale getirebilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinSoluk,
                )
                if (bekliyor) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Yesil)
                        Text("Giriş Yapılıyor…", modifier = Modifier.padding(start = 10.dp))
                    }
                }
                hata?.let { Text(it, color = YanlisKirmizi, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

/** Giriş sonrası: ad ve ilk hedef. */
@Composable
fun KurulumEkrani(
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    onerilenAd: String?,
    aydinlatmaSurum: Int,
) {
    var adim by remember { mutableStateOf(0) }
    var ad by remember { mutableStateOf(onerilenAd.orEmpty()) }
    var ders by remember { mutableStateOf<String?>(null) }
    var lisans by remember { mutableStateOf(banka.lisanslar.firstOrNull { it.aktif }?.kod) }
    var tarih by remember { mutableStateOf<LocalDate?>(null) }
    var tarihsizSor by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Zemin).navigationBarsPadding().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (adim == 0) {
                YesilUst("Seni Nasıl Çağıralım?", "Ana Sayfada Bu İsimle Karşılanacaksın.")
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = ad,
                        onValueChange = { ad = it.take(40) },
                        singleLine = true,
                        label = { Text("Adın") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Bosluk(8)
                    Text("Sadece Adın Yeterli, Soyadına Gerek Yok.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            } else if (adim == 1) {
                YesilUst("Hangi Sınava Hazırlanıyorsun?", "Dersler ve Denemeler Buna Göre Hazırlanır.")
                Column(Modifier.padding(20.dp)) {
                    LisansSecici(banka.lisanslar, lisans, { l -> banka.icerikVar(l) }) { lisans = it }
                }
            } else {
                YesilUst("İlk Hedefin", "Bir Derse Odaklan; Diğerlerini Sonra Eklersin.")
                Column(Modifier.padding(20.dp)) {
                    HedefSecici(banka.lisansDersleri(lisans), ders, { ders = it }, tarih, { tarih = it }, banka = banka, baslangicLisans = lisans)
                }
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (adim == 0) {
                Button(
                    onClick = { adim = 1 },
                    enabled = ad.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Devam Et") }
            } else if (adim == 1) {
                Button(
                    onClick = { adim = 2 },
                    enabled = lisans != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Devam Et") }
                TextButton(onClick = { adim = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Geri") }
            } else {
                Button(
                    onClick = {
                        val d = ders
                        if (d != null) {
                            if (tarih == null) tarihsizSor = true
                            else { depo.profilOlustur(ad, aydinlatmaSurum); lisans?.let { depo.lisansSec(it) }; depo.hedefKaydet(d, tarih) }
                        }
                    },
                    enabled = ders != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Başla") }
                TextButton(onClick = { depo.profilOlustur(ad, aydinlatmaSurum); lisans?.let { depo.lisansSec(it) } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hedefi Sonra Ekle")
                }
            }
        }
    }
    if (tarihsizSor) {
        TarihsizPenceresi(
            kapat = { tarihsizSor = false },
            devam = {
                tarihsizSor = false
                depo.profilOlustur(ad, aydinlatmaSurum); lisans?.let { depo.lisansSec(it) }
                ders?.let { depo.hedefKaydet(it, null) }
            },
        )
    }
}

/** Misafir hesabı Google'a bağlanırken o Google hesabı zaten kayıtlıysa sorulur. */
@Composable
fun HesapCakismasiPenceresi(kimlik: AuthCredential, hesap: Hesap, kapat: () -> Unit) {
    val kapsam = rememberCoroutineScope()
    var bekliyor by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!bekliyor) kapat() },
        title = { Text("Bu Google Hesabı Zaten Kayıtlı") },
        text = {
            Text("Bu Google hesabıyla daha önce Lisansla'ya giriş yapılmış. O hesaba geçersen bu misafir hesaptaki ilerleme silinir ve Google hesabındaki ilerlemen yüklenir.")
        },
        confirmButton = {
            TextButton(
                enabled = !bekliyor,
                onClick = {
                    bekliyor = true
                    kapsam.launch {
                        runCatching { hesap.misafiriBirakGoogleaGec(kimlik) }
                        bekliyor = false
                        kapat()
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = YanlisKirmizi),
            ) { Text("Google Hesabına Geç") }
        },
        dismissButton = { TextButton(onClick = kapat, enabled = !bekliyor) { Text("Vazgeç") } },
    )
}
