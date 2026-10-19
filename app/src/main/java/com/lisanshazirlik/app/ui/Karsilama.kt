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
            YesilUst("Lisansla'ya hoş geldin", "Sermaye piyasası lisans sınavlarına adım adım hazırlan.")
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Ozellik("Çalıştığın dersi ve sınav tarihini seç, kaç gün kaldığını takip et.")
                Ozellik("Her gün sana uygun sayıda soru çöz, serini bozma.")
                Ozellik("Yanlışlarını ve zayıf konularını tekrar et, deneme sınavlarıyla kendini ölç.")
                Bosluk(4)
                KartKutu {
                    Text("Kişisel verilerin", style = MaterialTheme.typography.titleMedium)
                    Bosluk(6)
                    Text(
                        "Sadece adını ve çalışma ilerlemeni saklarız. E-posta adresin kaydedilmez, reklam yoktur, verilerin satılmaz. Hesabını istediğin an silebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { metinAcik = true }, modifier = Modifier.padding(start = 0.dp)) {
                        Text("Aydınlatma metnini oku")
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { okudum = !okudum },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = okudum, onCheckedChange = { okudum = it })
                    Text("Kişisel verilerin korunmasına ilişkin aydınlatma metnini okudum.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Button(
            onClick = devam,
            enabled = okudum,
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp),
        ) { Text("Devam et") }
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
            YesilUst("Hesabını oluştur", "İlerlemen hesabında saklanır; telefon değiştirsen de kaybolmaz.")
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { calistir { hesap.googleIleGiris(baglam) } },
                    enabled = !bekliyor,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text("Google ile devam et") }
                OutlinedButton(
                    onClick = { calistir { hesap.misafirGiris(); null } },
                    enabled = !bekliyor,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text("Misafir olarak dene") }
                Text(
                    "Misafir olarak başlarsan ilerlemen bu telefondaki misafir hesabında tutulur. İstediğin zaman Profil'den Google hesabına bağlayıp kalıcı hale getirebilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetinSoluk,
                )
                if (bekliyor) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Yesil)
                        Text("Giriş yapılıyor…", modifier = Modifier.padding(start = 10.dp))
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
    var tarih by remember { mutableStateOf<LocalDate?>(null) }

    Column(Modifier.fillMaxSize().background(Zemin).navigationBarsPadding().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (adim == 0) {
                YesilUst("Seni nasıl çağıralım?", "Ana sayfada bu isimle karşılanacaksın.")
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = ad,
                        onValueChange = { ad = it.take(40) },
                        singleLine = true,
                        label = { Text("Adın") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Bosluk(8)
                    Text("Sadece adın yeterli, soyadına gerek yok.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            } else {
                YesilUst("İlk hedefin", "Bir derse odaklan; diğerlerini sonra eklersin.")
                Column(Modifier.padding(20.dp)) {
                    HedefSecici(banka.dersler, ders, { ders = it }, tarih, { tarih = it })
                }
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (adim == 0) {
                Button(
                    onClick = { adim = 1 },
                    enabled = ad.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Devam et") }
            } else {
                Button(
                    onClick = {
                        depo.profilOlustur(ad, aydinlatmaSurum)
                        val d = ders; val t = tarih
                        if (d != null && t != null) depo.hedefKaydet(d, t)
                    },
                    enabled = ders != null && tarih != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Başla") }
                TextButton(onClick = { depo.profilOlustur(ad, aydinlatmaSurum) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hedefi sonra ekle")
                }
            }
        }
    }
}

/** Misafir hesabı Google'a bağlanırken o Google hesabı zaten kayıtlıysa sorulur. */
@Composable
fun HesapCakismasiPenceresi(kimlik: AuthCredential, hesap: Hesap, kapat: () -> Unit) {
    val kapsam = rememberCoroutineScope()
    var bekliyor by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!bekliyor) kapat() },
        title = { Text("Bu Google hesabı zaten kayıtlı") },
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
            ) { Text("Google hesabına geç") }
        },
        dismissButton = { TextButton(onClick = kapat, enabled = !bekliyor) { Text("Vazgeç") } },
    )
}
