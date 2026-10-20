package com.lisanshazirlik.app.data

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Yorum(
    val id: String,
    val uid: String,
    val takmaAd: String,
    val metin: String,
    val zaman: Long,
    val begenenler: List<String>,
    val sikayetEdenler: List<String>,
)

/**
 * Kısa videoların beğeni ve yorumları.
 *
 * kisaBegeni/{kisa}_{uid}  { kisa, uid }                     Beğeni sayısı belge sayımıyla bulunur; sayaç tutulmaz, şişirilemez.
 * kisaYorum/{otomatik}     { uid, takmaAd, kisa, metin, zaman, begenenler[], sikayetEdenler[] }
 *
 * 3 kişinin bildirdiği yorum gizlenir. Yorum yazmak için takma ad gerekir (takmaAdlar ile doğrulanır).
 */
object KisaSosyal {
    private val db by lazy { FirebaseFirestore.getInstance() }
    const val YORUM_SINIR = 300
    const val GIZLEME_SIKAYET = 3

    suspend fun begeniSayisi(kisa: String): Long =
        db.collection("kisaBegeni").whereEqualTo("kisa", kisa).count().get(AggregateSource.SERVER).await().count

    fun begen(uid: String, kisa: String, ekle: Boolean) {
        val ref = db.collection("kisaBegeni").document("${kisa}_$uid")
        if (ekle) ref.set(mapOf("kisa" to kisa, "uid" to uid)) else ref.delete()
    }

    suspend fun yorumSayisi(kisa: String): Long =
        db.collection("kisaYorum").whereEqualTo("kisa", kisa).count().get(AggregateSource.SERVER).await().count

    /** Sıralama sorgusu bileşik dizin istemesin diye cihazda sıralanır. */
    suspend fun yorumlar(kisa: String): List<Yorum> {
        val q = db.collection("kisaYorum").whereEqualTo("kisa", kisa).limit(300).get().await()
        return q.documents.mapNotNull { d ->
            Yorum(
                id = d.id,
                uid = d.getString("uid") ?: return@mapNotNull null,
                takmaAd = d.getString("takmaAd") ?: "Anonim",
                metin = d.getString("metin") ?: return@mapNotNull null,
                zaman = d.getTimestamp("zaman")?.toDate()?.time ?: System.currentTimeMillis(),
                begenenler = (d.get("begenenler") as? List<*>)?.map { it.toString() } ?: emptyList(),
                sikayetEdenler = (d.get("sikayetEdenler") as? List<*>)?.map { it.toString() } ?: emptyList(),
            )
        }.filter { it.sikayetEdenler.size < GIZLEME_SIKAYET }.sortedByDescending { it.zaman }
    }

    suspend fun yorumYaz(uid: String, takmaAd: String, kisa: String, metin: String) {
        db.collection("kisaYorum").add(
            mapOf(
                "uid" to uid,
                "takmaAd" to takmaAd,
                "kisa" to kisa,
                "metin" to metin.trim().take(YORUM_SINIR),
                "zaman" to FieldValue.serverTimestamp(),
                "begenenler" to emptyList<String>(),
                "sikayetEdenler" to emptyList<String>(),
            )
        ).await()
    }

    fun yorumSil(id: String) {
        db.collection("kisaYorum").document(id).delete()
    }

    fun yorumBegen(id: String, uid: String, ekle: Boolean) {
        db.collection("kisaYorum").document(id)
            .update("begenenler", if (ekle) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid))
    }

    fun bildir(id: String, uid: String) {
        db.collection("kisaYorum").document(id).update("sikayetEdenler", FieldValue.arrayUnion(uid))
    }
}
