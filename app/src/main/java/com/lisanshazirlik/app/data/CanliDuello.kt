package com.lisanshazirlik.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/** Canlı maçta bir oyuncunun durumu. */
data class OyuncuDurumu(val cevaplanan: Int = 0, val dogru: Int = 0, val sure: Int = 0, val bitti: Boolean = false)

data class CanliMac(
    val id: String,
    val oyuncular: List<String>,
    val adlar: Map<String, String>,
    val sorular: List<String>,
    val ders: String,
    val ilerleme: Map<String, OyuncuDurumu>,
)

/**
 * Canlı düello eşleştirmesi (sunucusuz).
 *
 * bekleyenler/{uid}  Rakip arayan kişi: ders, lisans, ad, mac (eşleşince maç kimliği yazılır).
 * maclar/{id}        İki oyuncu, ortak 5 soru, her oyuncunun canlı ilerlemesi.
 *
 * Çift eşleşmeyi önlemek için yalnızca uid'si kendininkinden küçük olan bekleyeni "kapar";
 * büyük olan bekler ve kapılmayı dinler. Kapma işlemi tek bir işlem (transaction) içinde yapılır.
 */
object CanliDuello {
    private val db by lazy { FirebaseFirestore.getInstance() }
    const val BEKLEME_SN = 20
    private const val TAZE_MS = 30_000L

    /** Yabancılara gerçek ad gitmez: takma ad ya da uid'den türetilen anonim ad. */
    fun anonimAd(k: Kullanici?, uid: String): String =
        k?.takmaAd?.takeIf { it.isNotBlank() } ?: "Yatırımcı#" + (uid.hashCode().toUInt() % 10000u).toString().padStart(4, '0')

    private fun bekleyen(uid: String) = db.collection("bekleyenler").document(uid)

    /** Bekleyen birini kapmayı dener; başarılıysa maç kimliğini döner. */
    suspend fun kapmayiDene(uid: String, ad: String, ders: String, lisans: String?, sorular: List<String>): String? {
        val simdi = System.currentTimeMillis()
        val adaylar = db.collection("bekleyenler").whereEqualTo("ders", ders).limit(10).get().await().documents
            .filter { d ->
                d.id != uid && d.id < uid && d.get("mac") == null &&
                    (d.getTimestamp("zaman")?.toDate()?.time ?: 0L) > simdi - TAZE_MS
            }
        for (aday in adaylar) {
            val macRef = db.collection("maclar").document()
            val basarili = runCatching {
                db.runTransaction { tr ->
                    val benim = tr.get(bekleyen(uid))
                    if (benim.exists() && benim.get("mac") != null) return@runTransaction false // beni biri kaptı
                    val o = tr.get(aday.reference)
                    if (!o.exists() || o.get("mac") != null) return@runTransaction false
                    val oAd = o.getString("ad") ?: "Rakip"
                    tr.set(
                        macRef,
                        mapOf(
                            "oyuncular" to listOf(aday.id, uid),
                            "adlar" to mapOf(aday.id to oAd, uid to ad),
                            "sorular" to sorular,
                            "ders" to ders,
                            "zaman" to FieldValue.serverTimestamp(),
                            "ilerleme" to mapOf<String, Any>(),
                        ),
                    )
                    tr.update(aday.reference, "mac", macRef.id)
                    if (benim.exists()) tr.delete(bekleyen(uid))
                    true
                }.await()
            }.getOrDefault(false)
            if (basarili) return macRef.id
        }
        return null
    }

    suspend fun bekle(uid: String, ad: String, ders: String, lisans: String?) {
        bekleyen(uid).set(
            mapOf("uid" to uid, "ad" to ad, "ders" to ders, "lisans" to (lisans ?: ""), "zaman" to FieldValue.serverTimestamp(), "mac" to null)
        ).await()
    }

    fun beklemeyiDinle(uid: String, eslesti: (String) -> Unit): ListenerRegistration =
        bekleyen(uid).addSnapshotListener { s, _ -> s?.getString("mac")?.let(eslesti) }

    /** Beklemeyi bırakır. Bu arada biri kaptıysa maç kimliğini döner. */
    suspend fun birak(uid: String): String? = runCatching {
        val s = bekleyen(uid).get().await()
        val mac = s.getString("mac")
        bekleyen(uid).delete().await()
        mac
    }.getOrNull()

    fun maciDinle(id: String, guncel: (CanliMac) -> Unit): ListenerRegistration =
        db.collection("maclar").document(id).addSnapshotListener { d, _ ->
            if (d == null || !d.exists()) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            val il = (d.get("ilerleme") as? Map<String, Map<String, Any?>>).orEmpty().mapValues { (_, v) ->
                OyuncuDurumu(
                    (v["c"] as? Number)?.toInt() ?: 0,
                    (v["d"] as? Number)?.toInt() ?: 0,
                    (v["t"] as? Number)?.toInt() ?: 0,
                    v["b"] as? Boolean ?: false,
                )
            }
            @Suppress("UNCHECKED_CAST")
            guncel(
                CanliMac(
                    id = d.id,
                    oyuncular = (d.get("oyuncular") as? List<String>).orEmpty(),
                    adlar = (d.get("adlar") as? Map<String, String>).orEmpty(),
                    sorular = (d.get("sorular") as? List<String>).orEmpty(),
                    ders = d.getString("ders").orEmpty(),
                    ilerleme = il,
                )
            )
        }

    fun ilerlemeYaz(id: String, uid: String, o: OyuncuDurumu) {
        db.collection("maclar").document(id).update(
            "ilerleme.$uid", mapOf("c" to o.cevaplanan, "d" to o.dogru, "t" to o.sure, "b" to o.bitti)
        )
    }
}
