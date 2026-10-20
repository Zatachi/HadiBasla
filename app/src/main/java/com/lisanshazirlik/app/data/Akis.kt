package com.lisanshazirlik.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Topluluk akışında bir olay. Metin cihazda üretilir; kayıtta sadece tür ve değerler durur. */
data class AkisOlayi(
    val id: String,
    val takmaAd: String,
    val tur: String,
    val ders: String?,
    val deger: Int,
    val zaman: Long,
)

/** Şu an çalışanlar: toplam ve lisanslara göre dağılım. */
data class Cevrimici(val toplam: Int, val lisanslar: Map<String, Int>)

/**
 * Ana sayfanın buluşma alanı.
 *
 * akis/{otomatik}     { uid, takmaAd, tur, ders, deger, zaman }  Sadece akışa katılmayı seçenler yazar.
 * cevrimici/{uid}     { zaman, lisans }                          Uygulama açıkken dakikada bir yenilenir.
 *
 * Türler: gecti (sınavı geçti), deneme (deneme puanı), duello (düello galibiyeti), rozet (yeni rozet).
 */
object Akis {
    private val db by lazy { FirebaseFirestore.getInstance() }
    const val CEVRIMICI_DK = 5L
    val TURLER = setOf("gecti", "deneme", "duello", "rozet")

    /** Kullanıcı hem sıralamaya hem akışa katıldıysa olayı paylaşır. */
    fun paylas(depo: KullaniciDeposu, tur: String, ders: String? = null, deger: Int = 0) {
        val k = depo.kullanici ?: return
        if (!k.siralamaKatil || !k.akis || k.takmaAd.isBlank() || tur !in TURLER) return
        db.collection("akis").add(
            mapOf(
                "uid" to depo.uid,
                "takmaAd" to k.takmaAd,
                "tur" to tur,
                "ders" to ders,
                "deger" to deger,
                "zaman" to FieldValue.serverTimestamp(),
            )
        )
    }

    suspend fun son(sinir: Long = 12): List<AkisOlayi> {
        val q = db.collection("akis").orderBy("zaman", Query.Direction.DESCENDING).limit(sinir).get().await()
        return q.documents.mapNotNull { d ->
            val tur = d.getString("tur") ?: return@mapNotNull null
            AkisOlayi(
                id = d.id,
                takmaAd = d.getString("takmaAd") ?: "Anonim",
                tur = tur,
                ders = d.getString("ders"),
                deger = d.getLong("deger")?.toInt() ?: 0,
                zaman = d.getTimestamp("zaman")?.toDate()?.time ?: System.currentTimeMillis(),
            )
        }
    }

    /** Akıştan çıkınca kişinin eski olayları da silinir. */
    fun benimkileriSil(uid: String) {
        db.collection("akis").whereEqualTo("uid", uid).get().addOnSuccessListener { q ->
            q.documents.forEach { it.reference.delete() }
        }
    }

    /** "Buradayım" sinyali. Kimlik bilgisi yazılmaz; sadece zaman ve seçili lisans. */
    fun nabiz(uid: String, lisans: String?) {
        db.collection("cevrimici").document(uid).set(mapOf("zaman" to FieldValue.serverTimestamp(), "lisans" to lisans))
    }

    suspend fun cevrimici(): Cevrimici {
        val esik = Timestamp(Date(System.currentTimeMillis() - CEVRIMICI_DK * 60_000))
        val q = db.collection("cevrimici").whereGreaterThan("zaman", esik).limit(500).get().await()
        val lisanslar = q.documents.mapNotNull { it.getString("lisans") }.groupingBy { it }.eachCount()
        return Cevrimici(q.size(), lisanslar)
    }
}
