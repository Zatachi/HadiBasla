package com.lisanshazirlik.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Sadece bu telefona ait küçük tercihler. İlerleme bulutta tutulur (KullaniciDeposu). */
class YerelKayit(baglam: Context) {
    private val tercih = baglam.getSharedPreferences("lisansla", Context.MODE_PRIVATE)

    var degisim by mutableIntStateOf(0)
        private set

    /** Girişten önce okunan aydınlatma metninin sürümü. */
    var okunanAydinlatma: Int
        get() = tercih.getInt("aydinlatma", 0)
        set(v) { tercih.edit().putInt("aydinlatma", v).apply(); degisim++ }

    fun oy(soruId: String): Int = tercih.getInt("oy_$soruId", 0)

    fun oyVer(soruId: String, deger: Int) {
        tercih.edit().putInt("oy_$soruId", deger).apply()
        degisim++
    }

    fun temizle() {
        tercih.edit().clear().apply()
        degisim++
    }
}
