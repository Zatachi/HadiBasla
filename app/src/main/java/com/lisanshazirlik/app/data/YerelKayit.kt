package com.lisanshazirlik.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Kullanıcının kendi ilerlemesi: telefonda tutulur, kimseyle paylaşılmaz. */
class YerelKayit(baglam: Context) {
    private val tercih = baglam.getSharedPreferences("lisansla", Context.MODE_PRIVATE)

    /** Ekranlar bunu okuyarak kayıt değiştikçe kendini yeniler. */
    var degisim by mutableIntStateOf(0)
        private set

    private fun kume(anahtar: String): Set<String> =
        tercih.getStringSet(anahtar, emptySet())?.toSet() ?: emptySet()

    fun cozulenler(): Set<String> = kume("cozulen")
    fun dogrular(): Set<String> = kume("dogru")
    fun yanlislar(): Set<String> = kume("yanlis")

    fun kaydet(soruId: String, dogruMu: Boolean) {
        val cozulen = cozulenler() + soruId
        val dogru = if (dogruMu) dogrular() + soruId else dogrular() - soruId
        val yanlis = if (dogruMu) yanlislar() - soruId else yanlislar() + soruId
        tercih.edit()
            .putStringSet("cozulen", cozulen)
            .putStringSet("dogru", dogru)
            .putStringSet("yanlis", yanlis)
            .apply()
        degisim++
    }

    fun oy(soruId: String): Int = tercih.getInt("oy_$soruId", 0)

    fun oyVer(soruId: String, deger: Int) {
        tercih.edit().putInt("oy_$soruId", deger).apply()
        degisim++
    }
}
