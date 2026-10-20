package com.lisanshazirlik.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class BilgiKarti(val on: String, val arka: String)

data class Konu(
    val id: String,
    val ad: String,
    val ozet: String = "",
    val kartlar: List<BilgiKarti> = emptyList(),
    /** Animasyonlu anlatım videosunun adresi (yoksa boş). */
    val video: String = "",
)

data class Ders(
    val kod: String,
    val ad: String,
    val kisaAd: String,
    val aciklama: String,
    val konular: List<Konu>,
)

/** Bir lisans sınavı ve içerdiği dersler. Aktif olmayanlar "Yakında" görünür. */
data class Lisans(
    val kod: String,
    val ad: String,
    val kisaAd: String,
    val dersler: List<String>,
    val aktif: Boolean,
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
    val lisanslar: List<Lisans>,
    val dersler: List<Ders>,
    val sorular: List<Soru>,
) {
    fun ders(kod: String): Ders? = dersler.firstOrNull { it.kod == kod }
    fun lisans(kod: String?): Lisans? = lisanslar.firstOrNull { it.kod == kod }
    /** Seçili lisansın dersleri; lisans bilinmiyorsa tüm dersler. */
    fun lisansDersleri(kod: String?): List<Ders> =
        lisans(kod)?.dersler?.mapNotNull { ders(it) } ?: dersler
    fun konu(id: String): Pair<Ders, Konu>? =
        dersler.firstNotNullOfOrNull { d -> d.konular.firstOrNull { it.id == id }?.let { d to it } }
    fun dersSorulari(kod: String): List<Soru> = sorular.filter { it.ders == kod }
    fun konuSorulari(konuId: String): List<Soru> = sorular.filter { it.konu == konuId }
    fun bul(id: String): Soru? = sorular.firstOrNull { it.id == id }
    /** Lisansın derslerinden en az birinde soru varsa içerik hazır sayılır. */
    fun icerikVar(l: Lisans): Boolean = l.dersler.any { k -> sorular.any { it.ders == k } }

    companion object {
        val BOS = SoruBankasi(0, "", emptyList(), emptyList(), emptyList())

        fun ayristir(metin: String): SoruBankasi {
            val kok = JSONObject(metin)
            val dersler = kok.getJSONArray("dersler").nesneler().map { d ->
                Ders(
                    kod = d.getString("kod"),
                    ad = d.getString("ad"),
                    kisaAd = d.optString("kisaAd", d.getString("ad")),
                    aciklama = d.optString("aciklama", ""),
                    konular = d.getJSONArray("konular").nesneler().map { k ->
                        Konu(
                            id = k.getString("id"),
                            ad = k.getString("ad"),
                            ozet = k.optString("ozet", ""),
                            video = k.optString("video", ""),
                            kartlar = k.optJSONArray("kartlar")?.nesneler()?.map {
                                BilgiKarti(it.getString("on"), it.getString("arka"))
                            } ?: emptyList(),
                        )
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
            val lisanslar = kok.optJSONArray("lisanslar")?.nesneler()?.map { l ->
                Lisans(
                    kod = l.getString("kod"),
                    ad = l.getString("ad"),
                    kisaAd = l.optString("kisaAd", l.getString("ad")),
                    dersler = l.optJSONArray("dersler")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                    aktif = l.optBoolean("aktif", true),
                )
            } ?: emptyList()
            return SoruBankasi(
                surum = kok.optInt("surum", 0),
                guncelleme = kok.optString("guncelleme", ""),
                lisanslar = lisanslar,
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
