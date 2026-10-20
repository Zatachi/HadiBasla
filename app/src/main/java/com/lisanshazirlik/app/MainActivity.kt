package com.lisanshazirlik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.lisanshazirlik.app.ui.BasariSekme
import com.lisanshazirlik.app.ui.gunlukHedef
import com.lisanshazirlik.app.data.Oyun
import com.lisanshazirlik.app.data.Topluluk
import com.lisanshazirlik.app.ui.KartlarEkrani
import com.lisanshazirlik.app.ui.KonuEkrani
import com.lisanshazirlik.app.ui.LisansEkrani
import com.lisanshazirlik.app.ui.OzetEkrani
import com.lisanshazirlik.app.ui.SonuclarEkrani
import com.lisanshazirlik.app.ui.VideoEkrani
import com.lisanshazirlik.app.ui.PaketEkrani
import com.lisanshazirlik.app.ui.PaketGecisEkrani
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
    data class Deneme(val sorular: List<Soru>, val kapsam: String, val no: Int = 0) : Ekran
    data class DenemeListesi(val ders: String) : Ekran
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
    data class Video(val baslik: String, val adres: String) : Ekran
    data class Paket(val ders: String) : Ekran
    data class PaketAdim(val ders: String, val adimId: String) : Ekran
    data class PaketGecis(val ders: String, val bitenId: String, val sonuc: String?) : Ekran
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
    val cevapla: (Soru, Boolean) -> Unit = { s, d -> depo.cevapKaydet(s, d, gunlukHedef(banka, depo)) }

    // ---- puan, rozet, sıralama ve davet işleri ----
    var davetDurumu by remember { mutableStateOf<Topluluk.DavetDurumu?>(null) }
    val k = depo.kullanici
    LaunchedEffect(depo.uid) {
        davetDurumu = runCatching { Topluluk.davetDurumu(depo.uid) }.getOrNull()
        runCatching { Topluluk.haftalikOdulKontrol(depo.uid, depo) }
    }
    LaunchedEffect(davetDurumu?.gecerli, k != null) {
        davetDurumu?.let { Topluluk.davetOdulleri(depo, it.gecerli) }
    }
    LaunchedEffect(depo.ilerleme.size, depo.denemeler.size, depo.sonuclar.size, k?.seri, davetDurumu?.gecerli, k != null) {
        if (k == null) return@LaunchedEffect
        depo.rozetleriGuncelle(Oyun.hakEdilenRozetler(banka, depo, davetDurumu?.gecerli ?: 0))
        Topluluk.gecerlilikKontrol(depo.uid, depo)
    }
    LaunchedEffect(k?.xp, k?.siralamaKatil, k?.takmaAd, k?.duvar, depo.sonuclar.size) {
        kotlinx.coroutines.delay(3000)
        Topluluk.profilYaz(depo.uid, depo, banka)
    }
    depo.kutlama?.let { mesaj ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { depo.kutlama = null },
            title = { androidx.compose.material3.Text("Tebrikler!") },
            text = { androidx.compose.material3.Text(mesaj) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { depo.kutlama = null }) { androidx.compose.material3.Text("Harika") } },
        )
    }

    var basariIlk by remember { mutableStateOf(com.lisanshazirlik.app.ui.BasariBolumu.BEN) }
    val y = Yonlendirici(
        dersAc = { git(Ekran.Ders(it)) },
        coz = { baslik, sorular, konuId, mod ->
            if (konuId != null) depo.sonKonu(konuId)
            git(Ekran.Coz(baslik, sorular, mod))
        },
        genelDeneme = { git(Ekran.Deneme(denemeSorulari(banka, banka.lisansDersleri(depo.kullanici?.lisans)), "genel")) },
        dersDenemesi = { git(Ekran.DenemeListesi(it)) },
        hedefAc = { git(Ekran.Hedef(it)) },
        gorus = { git(Ekran.Gorus) },
        aydinlatma = { git(Ekran.Aydinlatma) },
        verilerim = { git(Ekran.Verilerim) },
        premium = { git(Ekran.Premium) },
        konuAc = { git(Ekran.Konu(it)) },
        sonuclar = { git(Ekran.Sonuclar) },
        lisans = { git(Ekran.Lisans) },
        hatirlatma = { git(Ekran.Hatirlatma) },
        paketAc = { git(Ekran.Paket(it)) },
        paketAdimAc = { d, a -> git(Ekran.PaketAdim(d, a)) },
        sekmeAc = { yigin = emptyList(); basariIlk = com.lisanshazirlik.app.ui.BasariBolumu.BEN; sekme = it },
        basariAc = { yigin = emptyList(); basariIlk = it; sekme = Sekme.BASARI },
    )

    BackHandler(enabled = yigin.isNotEmpty() || sekme != Sekme.ANA) {
        if (yigin.isNotEmpty()) geri() else sekme = Sekme.ANA
    }

    val ust = yigin.lastOrNull()
    // Alt menü her ekranda görünür; sadece deneme/konu testi sırasında gizlenir.
    // Bir sekmeye dokunmak o sekmenin başına döner.
    Kabuk(
        sekme,
        sekmeSec = { yigin = emptyList(); basariIlk = com.lisanshazirlik.app.ui.BasariBolumu.BEN; sekme = it },
        menuGoster = ust !is Ekran.Deneme && !(ust is Ekran.PaketAdim && ust.adimId.endsWith(":deneme")),
    ) { ic ->
        if (ust == null) {
            when (sekme) {
                Sekme.ANA -> AnaSekme(banka, depo, hesap.uid, y, ic)
                Sekme.DERSLER -> DerslerSekme(banka, depo, y, ic)
                Sekme.DENEME -> DenemeSekme(banka, depo, y, ic)
                Sekme.BASARI -> BasariSekme(banka, depo, hesap, davetDurumu, y.konuAc, y.sonuclar, ic, basariIlk)
                Sekme.PROFIL -> ProfilSekme(banka, depo, hesap, kayit, y, ic)
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = ic.calculateBottomPadding())
                    .consumeWindowInsets(ic),
            ) {
                AltEkran(ust, banka, depo, hesap, kayit, y, yigin.size, cevapla, ::git, ::geri)
            }
        }
    }
}

@Composable
private fun AltEkran(
    ust: Ekran,
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    hesap: Hesap,
    kayit: YerelKayit,
    y: Yonlendirici,
    derinlik: Int,
    cevapla: (Soru, Boolean) -> Unit,
    git: (Ekran) -> Unit,
    geri: () -> Unit,
) {
    key(derinlik) {
        when (ust) {
            is Ekran.Ders -> {
                val ders = banka.ders(ust.kod)
                if (ders == null) Yukleniyor()
                else DersEkrani(
                    ders, banka, depo.cozulenler(),
                    coz = { b, s, konuId -> y.coz(b, s, konuId, "pratik") },
                    deneme = { y.dersDenemesi(ust.kod) },
                    konuAc = y.konuAc,
                    paketAc = { y.paketAc(ust.kod) },
                    geri = geri,
                )
            }
            is Ekran.Coz -> CozEkrani(ust.baslik, ust.sorular, ust.mod, banka, kayit, cevapla, geri = geri)
            is Ekran.Deneme -> DenemeEkrani(
                ust.sorular, ust.kapsam, banka, cevapla,
                sonucKaydet = { kapsam, ort, gecti, puanlar, adet -> depo.denemeKaydet(kapsam, ort, gecti, puanlar, adet, ust.no) },
                geri = geri,
            )
            is Ekran.DenemeListesi -> com.lisanshazirlik.app.ui.DenemeListesiEkrani(
                ust.ders, banka, depo,
                ac = { no ->
                    com.lisanshazirlik.app.ui.numaraliDeneme(banka, ust.ders, no)?.let { git(Ekran.Deneme(it, ust.ders, no)) }
                },
                premiumAc = { git(Ekran.Premium) },
                geri = geri,
            )
            is Ekran.Hedef -> HedefEkrani(banka, depo, ust.ders, geri = geri)
            Ekran.Gorus -> GorusEkrani(geri = geri)
            Ekran.Aydinlatma -> AydinlatmaEkrani(geri = geri)
            Ekran.Verilerim -> VerilerimEkrani(banka, depo, hesap, geri = geri)
            Ekran.Premium -> PremiumEkrani(depo, geri = geri)
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
                        videoAc = { git(Ekran.Video(konu.ad, konu.video)) },
                        coz = { y.coz(konu.ad, calismaSirasi(sorular, depo), konu.id, "pratik") },
                        test = { depo.sonKonu(konu.id); git(Ekran.Deneme(konuTestiSorulari(banka, konu.id), "konu:${konu.id}")) },
                        geri = geri,
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
                        kartlar = if (konu.kartlar.isNotEmpty()) ({ geri(); git(Ekran.Kartlar(konu.id)) }) else null,
                        geri = geri,
                    )
                }
            }
            is Ekran.Kartlar -> {
                val dk = banka.konu(ust.id)
                if (dk == null) Yukleniyor() else KartlarEkrani(dk.second, geri = geri)
            }
            Ekran.Sonuclar -> SonuclarEkrani(banka, depo, depo.kullanici?.lisans, geri = geri)
            Ekran.Lisans -> LisansEkrani(banka.lisanslar, depo.kullanici?.lisans, sec = { depo.lisansSec(it) }, geri = geri)
            Ekran.Hatirlatma -> HatirlatmaEkrani(kayit, geri = geri)
            is Ekran.Video -> VideoEkrani(ust.baslik, ust.adres, geri = geri)
            is Ekran.Paket -> PaketEkrani(ust.ders, banka, depo, adimAc = { git(Ekran.PaketAdim(ust.ders, it.id)) }, geri = geri, denemelerAc = { git(Ekran.DenemeListesi(ust.ders)) })
            is Ekran.PaketGecis -> PaketGecisEkrani(
                ust.ders, ust.bitenId, banka, depo, ust.sonuc,
                adimAc = { geri(); git(Ekran.PaketAdim(ust.ders, it.id)) },
                pakete = geri,
            )
            is Ekran.PaketAdim -> {
                val a = com.lisanshazirlik.app.data.Paket.adim(banka, ust.ders, ust.adimId)
                val dersBilgi = banka.ders(ust.ders)
                if (a == null || dersBilgi == null) {
                    Yukleniyor()
                } else {
                    val tamamla: (String?) -> Unit = { sonuc ->
                        depo.paketAdimTamam(banka, ust.ders, a.id)
                        geri()
                        git(Ekran.PaketGecis(ust.ders, a.id, sonuc))
                    }
                    val konu = a.konu
                    when (a.tur) {
                        com.lisanshazirlik.app.data.Paket.Tur.VIDEO ->
                            VideoEkrani(konu!!.ad, konu.video, geri = geri, bitti = { tamamla(null) })
                        com.lisanshazirlik.app.data.Paket.Tur.OZET ->
                            OzetEkrani(dersBilgi, konu!!, coz = null, kartlar = null, geri = geri, bitti = { tamamla(null) })
                        com.lisanshazirlik.app.data.Paket.Tur.KARTLAR ->
                            KartlarEkrani(konu!!, geri = geri, bitti = { tamamla(null) })
                        com.lisanshazirlik.app.data.Paket.Tur.SORU -> {
                            val sorular = remember(a.id) { calismaSirasi(banka.konuSorulari(konu!!.id), depo) }
                            CozEkrani(konu!!.ad, sorular, "paket", banka, kayit, cevapla, geri = geri,
                                adimBitti = { d, t -> tamamla("$t sorudan $d doğru") })
                        }
                        com.lisanshazirlik.app.data.Paket.Tur.DENEME -> {
                            val sorular = remember(a.id) { dersDenemesiSorulari(banka, ust.ders) }
                            DenemeEkrani(
                                sorular, ust.ders, banka, cevapla,
                                sonucKaydet = { kapsam, ort, gecti, puanlar, adet ->
                                    depo.denemeKaydet(kapsam, ort, gecti, puanlar, adet)
                                    depo.paketAdimTamam(banka, ust.ders, a.id)
                                },
                                geri = geri,
                            )
                        }
                    }
                }
            }
        }
    }
}
