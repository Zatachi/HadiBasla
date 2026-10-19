package com.lisanshazirlik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lisanshazirlik.app.data.Hesap
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.SoruDeposu
import com.lisanshazirlik.app.data.YerelKayit
import com.lisanshazirlik.app.ui.AnaSekme
import com.lisanshazirlik.app.ui.AydinlatmaEkrani
import com.lisanshazirlik.app.ui.DerslerSekme
import com.lisanshazirlik.app.ui.HatirlatmaEkrani
import com.lisanshazirlik.app.ui.IstatistikSekme
import com.lisanshazirlik.app.ui.KartlarEkrani
import com.lisanshazirlik.app.ui.KonuEkrani
import com.lisanshazirlik.app.ui.LisansEkrani
import com.lisanshazirlik.app.ui.OzetEkrani
import com.lisanshazirlik.app.ui.SonuclarEkrani
import com.lisanshazirlik.app.ui.calismaSirasi
import com.lisanshazirlik.app.ui.konuTestiSorulari
import com.lisanshazirlik.app.ui.CozEkrani
import com.lisanshazirlik.app.ui.DenemeEkrani
import com.lisanshazirlik.app.ui.DenemeSekme
import com.lisanshazirlik.app.ui.DersEkrani
import com.lisanshazirlik.app.ui.GirisEkrani
import com.lisanshazirlik.app.ui.GorusEkrani
import com.lisanshazirlik.app.ui.HedefEkrani
import com.lisanshazirlik.app.ui.Kabuk
import com.lisanshazirlik.app.ui.KarsilamaEkrani
import com.lisanshazirlik.app.ui.KurulumEkrani
import com.lisanshazirlik.app.ui.LisanslaTema
import com.lisanshazirlik.app.ui.PremiumEkrani
import com.lisanshazirlik.app.ui.ProfilSekme
import com.lisanshazirlik.app.ui.Sekme
import com.lisanshazirlik.app.ui.VerilerimEkrani
import com.lisanshazirlik.app.ui.Yesil
import com.lisanshazirlik.app.ui.Yonlendirici
import com.lisanshazirlik.app.ui.Zemin
import com.lisanshazirlik.app.ui.denemeSorulari
import com.lisanshazirlik.app.ui.dersDenemesiSorulari

private sealed interface Ekran {
    data class Ders(val kod: String) : Ekran
    data class Coz(val baslik: String, val sorular: List<Soru>, val mod: String) : Ekran
    data class Deneme(val sorular: List<Soru>, val kapsam: String) : Ekran
    data class Hedef(val ders: String?) : Ekran
    data object Gorus : Ekran
    data object Aydinlatma : Ekran
    data object Verilerim : Ekran
    data object Premium : Ekran
    data class Konu(val id: String) : Ekran
    data class Ozet(val id: String) : Ekran
    data class Kartlar(val id: String) : Ekran
    data object Sonuclar : Ekran
    data object Lisans : Ekran
    data object Hatirlatma : Ekran
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val depo = SoruDeposu(applicationContext)
        val kayit = YerelKayit(applicationContext)
        val hesap = Hesap()
        setContent {
            LisanslaTema { Uygulama(depo, kayit, hesap) }
        }
    }
}

@Composable
private fun Uygulama(soruDeposu: SoruDeposu, kayit: YerelKayit, hesap: Hesap) {
    var banka by remember { mutableStateOf(soruDeposu.yerel()) }
    var googleAdi by remember { mutableStateOf<String?>(null) }
    val surum = BuildConfig.AYDINLATMA_SURUM

    LaunchedEffect(Unit) {
        soruDeposu.guncelle(banka)?.let { banka = it }
    }

    kayit.degisim
    val uid = hesap.uid
    if (uid == null) {
        if (kayit.okunanAydinlatma < surum) {
            KarsilamaEkrani(devam = { kayit.okunanAydinlatma = surum })
        } else {
            GirisEkrani(hesap) { googleAdi = it }
        }
        return
    }

    key(uid) {
        val depo = remember { KullaniciDeposu(uid) }
        DisposableEffect(Unit) {
            depo.baslat()
            onDispose { depo.durdur() }
        }
        val k = depo.kullanici
        when {
            !depo.yuklendi -> Yukleniyor()
            k == null || k.ad.isBlank() -> KurulumEkrani(banka, depo, googleAdi, surum)
            k.aydinlatmaSurum < surum -> KarsilamaEkrani(devam = {
                depo.aydinlatmaOnayla(surum)
                kayit.okunanAydinlatma = surum
            })
            else -> AnaUygulama(banka, depo, hesap, kayit)
        }
    }
}

@Composable
private fun Yukleniyor() {
    Box(Modifier.fillMaxSize().background(Zemin), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Yesil)
    }
}

@Composable
private fun AnaUygulama(banka: SoruBankasi, depo: KullaniciDeposu, hesap: Hesap, kayit: YerelKayit) {
    var sekme by remember { mutableStateOf(Sekme.ANA) }
    var yigin by remember { mutableStateOf(listOf<Ekran>()) }

    fun git(e: Ekran) { yigin = yigin + e }
    fun geri() { if (yigin.isNotEmpty()) yigin = yigin.dropLast(1) }
    val cevapla: (Soru, Boolean) -> Unit = { s, d -> depo.cevapKaydet(s, d) }

    val y = Yonlendirici(
        dersAc = { git(Ekran.Ders(it)) },
        coz = { baslik, sorular, konuId, mod ->
            if (konuId != null) depo.sonKonu(konuId)
            git(Ekran.Coz(baslik, sorular, mod))
        },
        genelDeneme = { git(Ekran.Deneme(denemeSorulari(banka, banka.lisansDersleri(depo.kullanici?.lisans)), "genel")) },
        dersDenemesi = { git(Ekran.Deneme(dersDenemesiSorulari(banka, it), it)) },
        hedefAc = { git(Ekran.Hedef(it)) },
        gorus = { git(Ekran.Gorus) },
        aydinlatma = { git(Ekran.Aydinlatma) },
        verilerim = { git(Ekran.Verilerim) },
        premium = { git(Ekran.Premium) },
        konuAc = { git(Ekran.Konu(it)) },
        sonuclar = { git(Ekran.Sonuclar) },
        lisans = { git(Ekran.Lisans) },
        hatirlatma = { git(Ekran.Hatirlatma) },
    )

    BackHandler(enabled = yigin.isNotEmpty() || sekme != Sekme.ANA) {
        if (yigin.isNotEmpty()) geri() else sekme = Sekme.ANA
    }

    val ust = yigin.lastOrNull()
    if (ust == null) {
        Kabuk(sekme, { sekme = it }) { ic ->
            when (sekme) {
                Sekme.ANA -> AnaSekme(banka, depo, y, ic)
                Sekme.DERSLER -> DerslerSekme(banka, depo, y, ic)
                Sekme.DENEME -> DenemeSekme(banka, depo, y, ic)
                Sekme.ISTATISTIK -> IstatistikSekme(banka, depo, depo.kullanici?.lisans, y.konuAc, y.sonuclar, ic)
                Sekme.PROFIL -> ProfilSekme(banka, depo, hesap, kayit, y, ic)
            }
        }
        return
    }

    key(yigin.size) {
        when (ust) {
            is Ekran.Ders -> {
                val ders = banka.ders(ust.kod)
                if (ders == null) Yukleniyor()
                else DersEkrani(
                    ders, banka, depo.cozulenler(),
                    coz = { b, s, konuId -> y.coz(b, s, konuId, "pratik") },
                    deneme = { y.dersDenemesi(ust.kod) },
                    konuAc = y.konuAc,
                    geri = ::geri,
                )
            }
            is Ekran.Coz -> CozEkrani(ust.baslik, ust.sorular, ust.mod, banka, kayit, cevapla, geri = ::geri)
            is Ekran.Deneme -> DenemeEkrani(
                ust.sorular, ust.kapsam, banka, cevapla,
                sonucKaydet = { kapsam, ort, gecti, puanlar, adet -> depo.denemeKaydet(kapsam, ort, gecti, puanlar, adet) },
                geri = ::geri,
            )
            is Ekran.Hedef -> HedefEkrani(banka, depo, ust.ders, geri = ::geri)
            Ekran.Gorus -> GorusEkrani(geri = ::geri)
            Ekran.Aydinlatma -> AydinlatmaEkrani(geri = ::geri)
            Ekran.Verilerim -> VerilerimEkrani(banka, depo, hesap, geri = ::geri)
            Ekran.Premium -> PremiumEkrani(depo, geri = ::geri)
            is Ekran.Konu -> {
                val dk = banka.konu(ust.id)
                if (dk == null) Yukleniyor()
                else {
                    val (ders, konu) = dk
                    val sorular = banka.konuSorulari(konu.id)
                    KonuEkrani(
                        ders, konu, banka, depo,
                        ozetAc = { git(Ekran.Ozet(konu.id)) },
                        kartlarAc = { git(Ekran.Kartlar(konu.id)) },
                        coz = { y.coz(konu.ad, calismaSirasi(sorular, depo), konu.id, "pratik") },
                        test = { depo.sonKonu(konu.id); git(Ekran.Deneme(konuTestiSorulari(banka, konu.id), "konu:${konu.id}")) },
                        geri = ::geri,
                    )
                }
            }
            is Ekran.Ozet -> {
                val dk = banka.konu(ust.id)
                if (dk == null) Yukleniyor()
                else {
                    val (ders, konu) = dk
                    val sorular = banka.konuSorulari(konu.id)
                    OzetEkrani(
                        ders, konu,
                        coz = if (sorular.isNotEmpty()) ({ geri(); y.coz(konu.ad, calismaSirasi(sorular, depo), konu.id, "pratik") }) else null,
                        geri = ::geri,
                    )
                }
            }
            is Ekran.Kartlar -> {
                val dk = banka.konu(ust.id)
                if (dk == null) Yukleniyor() else KartlarEkrani(dk.second, geri = ::geri)
            }
            Ekran.Sonuclar -> SonuclarEkrani(banka, depo, depo.kullanici?.lisans, geri = ::geri)
            Ekran.Lisans -> LisansEkrani(banka.lisanslar, depo.kullanici?.lisans, sec = { depo.lisansSec(it) }, geri = ::geri)
            Ekran.Hatirlatma -> HatirlatmaEkrani(kayit, geri = ::geri)
        }
    }
}
