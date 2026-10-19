package com.lisanshazirlik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lisanshazirlik.app.data.GeriBildirim
import com.lisanshazirlik.app.data.Soru
import com.lisanshazirlik.app.data.SoruDeposu
import com.lisanshazirlik.app.data.YerelKayit
import com.lisanshazirlik.app.ui.AnaEkran
import com.lisanshazirlik.app.ui.CozEkrani
import com.lisanshazirlik.app.ui.DenemeEkrani
import com.lisanshazirlik.app.ui.DersEkrani
import com.lisanshazirlik.app.ui.GorusEkrani
import com.lisanshazirlik.app.ui.LisanslaTema
import com.lisanshazirlik.app.ui.denemeSorulari

private sealed interface Ekran {
    data object Ana : Ekran
    data class Ders(val kod: String) : Ekran
    data class Coz(val baslik: String, val sorular: List<Soru>, val mod: String) : Ekran
    data class Deneme(val sorular: List<Soru>) : Ekran
    data object Gorus : Ekran
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val depo = SoruDeposu(applicationContext)
        val kayit = YerelKayit(applicationContext)
        setContent {
            LisanslaTema { Uygulama(depo, kayit) }
        }
    }
}

@Composable
private fun Uygulama(depo: SoruDeposu, kayit: YerelKayit) {
    var banka by remember { mutableStateOf(depo.yerel()) }
    var yigin by remember { mutableStateOf(listOf<Ekran>(Ekran.Ana)) }

    LaunchedEffect(Unit) {
        GeriBildirim.hazirla()
        depo.guncelle(banka)?.let { banka = it }
    }

    fun git(e: Ekran) { yigin = yigin + e }
    fun geri() { if (yigin.size > 1) yigin = yigin.dropLast(1) }

    BackHandler(enabled = yigin.size > 1) { geri() }

    key(yigin.size) {
        when (val e = yigin.last()) {
            Ekran.Ana -> AnaEkran(
                banka = banka,
                kayit = kayit,
                dersAc = { git(Ekran.Ders(it)) },
                denemeBaslat = { git(Ekran.Deneme(denemeSorulari(banka))) },
                yanlislariAc = {
                    val yanlislar = kayit.yanlislar().mapNotNull { banka.bul(it) }.shuffled()
                    git(Ekran.Coz("Yanlışlarım", yanlislar, "yanlislar"))
                },
                gorusAc = { git(Ekran.Gorus) },
            )
            is Ekran.Ders -> {
                val ders = banka.ders(e.kod)
                if (ders == null) geri()
                else DersEkrani(ders, banka, kayit, coz = { b, s -> git(Ekran.Coz(b, s, "pratik")) }, geri = ::geri)
            }
            is Ekran.Coz -> CozEkrani(e.baslik, e.sorular, e.mod, banka, kayit, geri = ::geri)
            is Ekran.Deneme -> DenemeEkrani(e.sorular, banka, kayit, geri = ::geri)
            Ekran.Gorus -> GorusEkrani(geri = ::geri)
        }
    }
}
