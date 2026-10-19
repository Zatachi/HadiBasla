package com.lisanshazirlik.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Konu(val id: String, val ad: String)

data class Ders(
    val kod: String,
    val ad: String,
    val kisaAd: String,
    val konular: List<Konu>,
)

data class Soru(
    val id: String,
    val ders: String,
    val konu: String,
    val metin: String,
    val secenekler: List<String>,
    val dogru: Int,
    val aciklama: String,
    val kaynak: String,
)

data class SoruBankasi(
    val surum: Int,
    val guncelleme: String,
    val dersler: List<Ders>,
    val sorular: List<Soru>,
) {
    fun ders(kod: String): Ders? = dersler.firstOrNull { it.kod == kod }
    fun dersSorulari(kod: String): List<Soru> = sorular.filter { it.ders == kod }
    fun konuSorulari(konuId: String): List<Soru> = sorular.filter { it.konu == konuId }
    fun bul(id: String): Soru? = sorular.firstOrNull { it.id == id }

    companion object {
        val BOS = SoruBankasi(0, "", emptyList(), emptyList())

        fun ayristir(metin: String): SoruBankasi {
            val kok = JSONObject(metin)
            val dersler = kok.getJSONArray("dersler").nesneler().map { d ->
                Ders(
                    kod = d.getString("kod"),
                    ad = d.getString("ad"),
                    kisaAd = d.optString("kisaAd", d.getString("ad")),
                    konular = d.getJSONArray("konular").nesneler().map { k ->
                        Konu(k.getString("id"), k.getString("ad"))
                    },
                )
            }
            val sorular = kok.getJSONArray("sorular").nesneler().mapNotNull { s ->
                runCatching {
                    val secenekler = s.getJSONArray("secenekler").let { a ->
                        (0 until a.length()).map { a.getString(it) }
                    }
                    val dogru = s.getInt("dogru")
                    require(secenekler.size in 2..5 && dogru in secenekler.indices)
                    Soru(
                        id = s.getString("id"),
                        ders = s.getString("ders"),
                        konu = s.getString("konu"),
                        metin = s.getString("soru"),
                        secenekler = secenekler,
                        dogru = dogru,
                        aciklama = s.optString("aciklama", ""),
                        kaynak = s.optString("kaynak", ""),
                    )
                }.getOrNull()
            }
            return SoruBankasi(
                surum = kok.optInt("surum", 0),
                guncelleme = kok.optString("guncelleme", ""),
                dersler = dersler,
                sorular = sorular,
            )
        }

        private fun JSONArray.nesneler(): List<JSONObject> =
            (0 until length()).map { getJSONObject(it) }
    }
}

/**
 * Sorular GitHub'daki sorular/sorular.json dosyasından çekilir ve telefonda saklanır.
 * İnternet yoksa en son indirilen (ya da uygulamayla gelen) sorular kullanılır.
 */
class SoruDeposu(private val baglam: Context) {
    private val onbellek = File(baglam.filesDir, "sorular.json")

    fun yerel(): SoruBankasi {
        val gomulu = runCatching {
            SoruBankasi.ayristir(baglam.assets.open("sorular.json").bufferedReader().use { it.readText() })
        }.getOrNull()
        val indirilen = runCatching {
            if (onbellek.exists()) SoruBankasi.ayristir(onbellek.readText()) else null
        }.getOrNull()
        return listOfNotNull(gomulu, indirilen)
            .filter { it.sorular.isNotEmpty() }
            .maxByOrNull { it.surum }
            ?: SoruBankasi.BOS
    }

    suspend fun guncelle(mevcut: SoruBankasi): SoruBankasi? = withContext(Dispatchers.IO) {
        runCatching {
            // GitHub birkaç dakika önbellekte tutar; 5 dakikalık dilimlerle taze sürüm istenir.
            val adres = URL("$ADRES?t=${System.currentTimeMillis() / 300_000}")
            val baglanti = (adres.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 15_000
            }
            try {
                if (baglanti.responseCode != 200) return@runCatching null
                val metin = baglanti.inputStream.bufferedReader().use { it.readText() }
                val yeni = SoruBankasi.ayristir(metin)
                if (yeni.sorular.isEmpty() || yeni.surum < mevcut.surum) return@runCatching null
                onbellek.writeText(metin)
                yeni
            } finally {
                baglanti.disconnect()
            }
        }.getOrNull()
    }

    companion object {
        const val ADRES = "https://raw.githubusercontent.com/Zatachi/HadiBasla/main/sorular/sorular.json"
    }
}
