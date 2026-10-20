package com.lisanshazirlik.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class DuelloKaydi(
    val kod: String,
    val kurucu: String,
    val kurucuAd: String,
    val sorular: List<String>,
    val kurucuDogru: Int,
    val kurucuSure: Int,
    val rakip: String?,
    val rakipAd: String,
    val rakipDogru: Int?,
    val rakipSure: Int?,
    val zaman: Long,
) {
    val bitti get() = rakip != null && rakipDogru != null

    /** uid açısından sonuç: 1 kazandı, 0 berabere, -1 kaybetti, null henüz bitmedi. */
    fun sonuc(uid: String): Int? {
        if (!bitti) return null
        val ben = if (uid == kurucu) kurucuDogru to kurucuSure else rakipDogru!! to rakipSure!!
        val o = if (uid == kurucu) rakipDogru!! to rakipSure!! else kurucuDogru to kurucuSure
        return when {
            ben.first != o.first -> if (ben.first > o.first) 1 else -1
            ben.second != o.second -> if (ben.second < o.second) 1 else -1
            else -> 0
        }
    }
}

/**
 * Düello: 5 soruluk, sırayla oynanan yarış. Kuran oynar ve kodu paylaşır; rakip aynı soruları
 * kodla açıp oynar. En çok doğru yapan, eşitlikte daha hızlı olan kazanır.
 *
 * duellolar/{kod}  kurucu, kurucuAd, sorular, kurucuDogru, kurucuSure, rakip (ilk başta null), rakip* alanları
 */
object Duello {
    private val db by lazy { FirebaseFirestore.getInstance() }
    const val SORU_SAYISI = 5
    private const val HARFLER = "ABCDEFGHJKLMNPRSTUVYZ23456789"

    class Hata(mesaj: String) : Exception(mesaj)

    fun kodUret(): String = (1..6).map { HARFLER.random() }.joinToString("")

    fun sorulariSec(banka: SoruBankasi, dersler: Collection<String>): List<Soru> {
        val havuz = banka.sorular.filter { it.ders in dersler }
        return havuz.shuffled().take(SORU_SAYISI)
    }

    /** Görünen ad: takma ad varsa o, yoksa adın ilk kelimesi. */
    fun gorunenAd(k: Kullanici?): String =
        k?.takmaAd?.takeIf { it.isNotBlank() } ?: k?.ad?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Rakip"

    suspend fun olustur(uid: String, ad: String, lisans: String?, sorular: List<String>, dogru: Int, sure: Int): String {
        repeat(5) {
            val kod = kodUret()
            try {
                db.collection("duellolar").document(kod).set(
                    mapOf(
                        "kurucu" to uid, "kurucuAd" to ad, "sorular" to sorular,
                        "kurucuDogru" to dogru, "kurucuSure" to sure, "rakip" to null,
                        "zaman" to FieldValue.serverTimestamp(), "lisans" to (lisans ?: ""),
                    )
                ).await()
                return kod
            } catch (e: Exception) {
                // Kod çakıştıysa kural yazmayı reddeder; yeni kodla dene.
            }
        }
        throw Hata("Düello Oluşturulamadı. İnternet Bağlantını Kontrol Et.")
    }

    suspend fun getir(kod: String): DuelloKaydi? {
        val d = db.collection("duellolar").document(kod.trim().uppercase()).get().await()
        if (!d.exists()) return null
        @Suppress("UNCHECKED_CAST")
        return DuelloKaydi(
            kod = d.id,
            kurucu = d.getString("kurucu").orEmpty(),
            kurucuAd = d.getString("kurucuAd").orEmpty(),
            sorular = (d.get("sorular") as? List<String>).orEmpty(),
            kurucuDogru = d.getLong("kurucuDogru")?.toInt() ?: 0,
            kurucuSure = d.getLong("kurucuSure")?.toInt() ?: 0,
            rakip = d.getString("rakip"),
            rakipAd = d.getString("rakipAd").orEmpty(),
            rakipDogru = d.getLong("rakipDogru")?.toInt(),
            rakipSure = d.getLong("rakipSure")?.toInt(),
            zaman = d.getTimestamp("zaman")?.toDate()?.time ?: 0L,
        )
    }

    /** Kodu kontrol eder; oynanabilirse kaydı döner. */
    suspend fun katilKontrol(kod: String, uid: String): DuelloKaydi {
        val k = getir(kod) ?: throw Hata("Bu Kodla Bir Düello Bulunamadı.")
        if (k.kurucu == uid) throw Hata("Bu Senin Düellon. Kodu Bir Arkadaşına Gönder.")
        if (k.rakip != null) throw Hata("Bu Düello Zaten Oynanmış.")
        return k
    }

    suspend fun sonucYaz(kod: String, uid: String, ad: String, dogru: Int, sure: Int) {
        db.collection("duellolar").document(kod).update(
            mapOf("rakip" to uid, "rakipAd" to ad, "rakipDogru" to dogru, "rakipSure" to sure, "rakipZaman" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun benimkiler(uid: String): List<DuelloKaydi> {
        val a = db.collection("duellolar").whereEqualTo("kurucu", uid).limit(20).get().await().documents.map { it.id }
        val b = db.collection("duellolar").whereEqualTo("rakip", uid).limit(20).get().await().documents.map { it.id }
        return (a + b).distinct().mapNotNull { runCatching { getir(it) }.getOrNull() }.sortedByDescending { it.zaman }
    }
}
