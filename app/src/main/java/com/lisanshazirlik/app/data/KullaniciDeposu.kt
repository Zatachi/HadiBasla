package com.lisanshazirlik.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Kullanici(
    val ad: String,
    val aydinlatmaSurum: Int,
    val seri: Int,
    val sonGun: String,
    val sonKonu: String?,
    val lisans: String?,
    val premium: Boolean,
)

/** Gerçek lisans sınavında alınan puan. */
data class SinavSonucu(val ders: String, val puan: Int, val tarih: LocalDate?)

data class GunKaydi(val tarih: LocalDate, val sayi: Int, val dogru: Int)

data class Hedef(val ders: String, val sinavTarihi: LocalDate?, val gecti: Boolean) {
    /** Sınav tarihi seçilmediyse null. */
    fun kalanGun(bugun: LocalDate = LocalDate.now()): Long? = sinavTarihi?.let { ChronoUnit.DAYS.between(bugun, it) }
}

data class DenemeKaydi(
    val id: String,
    val zaman: Long,
    val kapsam: String, // "genel" ya da ders kodu
    val ortalama: Int,
    val gecti: Boolean,
    val dersPuanlari: Map<String, Int>,
    val soruSayisi: Int,
)

/**
 * Kullanıcının bulutta tutulan verileri. Firestore çevrimdışı önbelleği sayesinde internet yokken de çalışır,
 * bağlantı gelince eşitlenir. Ekranlar buradaki durumları okur.
 *
 * kullanicilar/{uid}                   ad, aydınlatma onayı, seri, son konu
 * kullanicilar/{uid}/hedefler/{ders}   sınav tarihi, geçti mi
 * kullanicilar/{uid}/ilerleme/{ders}   s: { soruId: 1 (doğru) | 0 (yanlış) }  (son cevap)
 * kullanicilar/{uid}/gunler/{tarih}    sayi: o gün çözülen soru
 * kullanicilar/{uid}/denemeler/{id}    deneme sonuçları (kapsam: genel, ders kodu ya da konu:ID)
 * kullanicilar/{uid}/sonuclar/{ders}   gerçek sınavda alınan puan
 */
class KullaniciDeposu(val uid: String) {
    private val db = FirebaseFirestore.getInstance()
    private val kok = db.collection("kullanicilar").document(uid)
    private val dinleyiciler = mutableListOf<ListenerRegistration>()
    private val bugunKey = LocalDate.now().toString()

    var yuklendi by mutableStateOf(false)
        private set
    var kullanici by mutableStateOf<Kullanici?>(null)
        private set
    var hedefler by mutableStateOf<List<Hedef>>(emptyList())
        private set
    var ilerleme by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var bugunCozulen by mutableIntStateOf(0)
        private set
    var denemeler by mutableStateOf<List<DenemeKaydi>>(emptyList())
        private set
    var sonuclar by mutableStateOf<Map<String, SinavSonucu>>(emptyMap())
        private set
    /** Son 30 günün çalışma kayıtları, eskiden yeniye. */
    var gunler by mutableStateOf<List<GunKaydi>>(emptyList())
        private set

    private val dersIlerleme = mutableMapOf<String, Map<String, Int>>()

    fun baslat() {
        dinleyiciler += kok.addSnapshotListener { s, _ ->
            if (s == null) return@addSnapshotListener
            kullanici = if (s.exists()) s.kullanici() else null
            yuklendi = true
        }
        dinleyiciler += kok.collection("hedefler").addSnapshotListener { q, _ ->
            if (q == null) return@addSnapshotListener
            hedefler = q.documents.mapNotNull { d ->
                runCatching {
                    Hedef(
                        ders = d.id,
                        sinavTarihi = d.getString("sinavTarihi")?.let { LocalDate.parse(it) },
                        gecti = d.getBoolean("gecti") ?: false,
                    )
                }.getOrNull()
            }.sortedWith(compareBy<Hedef, LocalDate?>(nullsLast()) { it.sinavTarihi })
        }
        dinleyiciler += kok.collection("ilerleme").addSnapshotListener { q, _ ->
            if (q == null) return@addSnapshotListener
            dersIlerleme.clear()
            q.documents.forEach { d ->
                @Suppress("UNCHECKED_CAST")
                val s = (d.get("s") as? Map<String, Any?>).orEmpty()
                dersIlerleme[d.id] = s.mapValues { (_, v) -> (v as? Number)?.toInt() ?: 0 }
            }
            ilerleme = dersIlerleme.values.fold(emptyMap()) { a, b -> a + b }
        }
        val otuzGunOnce = LocalDate.now().minusDays(29).toString()
        dinleyiciler += kok.collection("gunler")
            .whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), otuzGunOnce)
            .addSnapshotListener { q, _ ->
                if (q == null) return@addSnapshotListener
                gunler = q.documents.mapNotNull { d ->
                    runCatching {
                        GunKaydi(LocalDate.parse(d.id), d.getLong("sayi")?.toInt() ?: 0, d.getLong("dogru")?.toInt() ?: 0)
                    }.getOrNull()
                }.sortedBy { it.tarih }
                bugunCozulen = gunler.firstOrNull { it.tarih.toString() == bugunKey }?.sayi ?: 0
            }
        dinleyiciler += kok.collection("sonuclar").addSnapshotListener { q, _ ->
            if (q == null) return@addSnapshotListener
            sonuclar = q.documents.mapNotNull { d ->
                val puan = d.getLong("puan")?.toInt() ?: return@mapNotNull null
                d.id to SinavSonucu(d.id, puan, d.getString("tarih")?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
            }.toMap()
        }
        dinleyiciler += kok.collection("denemeler").orderBy("zaman", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { q, _ ->
                if (q == null) return@addSnapshotListener
                denemeler = q.documents.mapNotNull { it.deneme() }
            }
    }

    fun durdur() {
        dinleyiciler.forEach { it.remove() }
        dinleyiciler.clear()
    }

    // ---- Türetilmiş bilgiler ----

    fun cozulenler(): Set<String> = ilerleme.keys
    fun dogrular(): Set<String> = ilerleme.filterValues { it == 1 }.keys
    fun yanlislar(): Set<String> = ilerleme.filterValues { it == 0 }.keys

    fun aktifHedefler(): List<Hedef> = hedefler.filter { !it.gecti }
    fun hedef(ders: String): Hedef? = hedefler.firstOrNull { it.ders == ders }

    /** Son çalışma bugün ya da dün değilse seri bozulmuştur. */
    fun seri(): Int {
        val k = kullanici ?: return 0
        val bugun = LocalDate.now()
        return if (k.sonGun == bugun.toString() || k.sonGun == bugun.minusDays(1).toString()) k.seri else 0
    }

    // ---- Yazma ----

    fun profilOlustur(ad: String, aydinlatmaSurum: Int) {
        kok.set(
            mapOf(
                "ad" to ad.trim().take(40),
                "aydinlatmaSurum" to aydinlatmaSurum,
                "aydinlatmaZaman" to FieldValue.serverTimestamp(),
                "olusturma" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
    }

    fun adGuncelle(ad: String) {
        kok.set(mapOf("ad" to ad.trim().take(40)), SetOptions.merge())
    }

    fun aydinlatmaOnayla(surum: Int) {
        kok.set(mapOf("aydinlatmaSurum" to surum, "aydinlatmaZaman" to FieldValue.serverTimestamp()), SetOptions.merge())
    }

    fun hedefKaydet(ders: String, tarih: LocalDate?, gecti: Boolean = false) {
        kok.collection("hedefler").document(ders).set(
            mapOf("sinavTarihi" to tarih?.toString(), "gecti" to gecti, "zaman" to FieldValue.serverTimestamp())
        )
    }

    fun hedefSil(ders: String) {
        kok.collection("hedefler").document(ders).delete()
    }

    fun cevapKaydet(soru: Soru, dogru: Boolean) {
        kok.collection("ilerleme").document(soru.ders)
            .set(mapOf("s" to mapOf(soru.id to if (dogru) 1 else 0)), SetOptions.merge())
        kok.collection("gunler").document(bugunKey)
            .set(
                mapOf("sayi" to FieldValue.increment(1), "dogru" to FieldValue.increment(if (dogru) 1 else 0)),
                SetOptions.merge(),
            )

        val k = kullanici
        val dun = LocalDate.now().minusDays(1).toString()
        val yeniSeri = when (k?.sonGun) {
            bugunKey -> k.seri
            dun -> k.seri + 1
            else -> 1
        }
        if (k?.sonGun != bugunKey) {
            kok.set(mapOf("seri" to yeniSeri, "sonGun" to bugunKey), SetOptions.merge())
        }
    }

    fun lisansSec(kod: String) {
        kok.set(mapOf("lisans" to kod), SetOptions.merge())
    }

    /** Gerçek sınav puanı. 50 ve üzeri girilirse o dersin hedefi "geçildi" olur. */
    fun sonucKaydet(ders: String, puan: Int, tarih: LocalDate?) {
        kok.collection("sonuclar").document(ders).set(
            mapOf("puan" to puan.coerceIn(0, 100), "tarih" to tarih?.toString(), "zaman" to FieldValue.serverTimestamp())
        )
        val h = hedef(ders)
        if (puan >= 50) hedefKaydet(ders, h?.sinavTarihi ?: tarih, gecti = true)
        else if (h?.gecti == true) hedefKaydet(ders, h.sinavTarihi, gecti = false)
    }

    fun sonucSil(ders: String) {
        kok.collection("sonuclar").document(ders).delete()
    }

    fun sonKonu(konuId: String) {
        if (kullanici?.sonKonu != konuId) kok.set(mapOf("sonKonu" to konuId), SetOptions.merge())
    }

    fun denemeKaydet(kapsam: String, ortalama: Int, gecti: Boolean, dersPuanlari: Map<String, Int>, soruSayisi: Int) {
        kok.collection("denemeler").add(
            mapOf(
                "zaman" to FieldValue.serverTimestamp(),
                "kapsam" to kapsam,
                "ortalama" to ortalama,
                "gecti" to gecti,
                "dersPuanlari" to dersPuanlari,
                "soruSayisi" to soruSayisi,
            )
        )
    }

    private fun DocumentSnapshot.kullanici() = Kullanici(
        ad = getString("ad").orEmpty(),
        aydinlatmaSurum = getLong("aydinlatmaSurum")?.toInt() ?: 0,
        seri = getLong("seri")?.toInt() ?: 0,
        sonGun = getString("sonGun").orEmpty(),
        sonKonu = getString("sonKonu"),
        lisans = getString("lisans"),
        premium = getBoolean("premium") ?: false,
    )

    private fun DocumentSnapshot.deneme(): DenemeKaydi? = runCatching {
        @Suppress("UNCHECKED_CAST")
        val puanlar = (get("dersPuanlari") as? Map<String, Any?>).orEmpty()
            .mapValues { (_, v) -> (v as? Number)?.toInt() ?: 0 }
        DenemeKaydi(
            id = id,
            zaman = getTimestamp("zaman", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
                ?: System.currentTimeMillis(),
            kapsam = getString("kapsam") ?: "genel",
            ortalama = getLong("ortalama")?.toInt() ?: 0,
            gecti = getBoolean("gecti") ?: false,
            dersPuanlari = puanlar,
            soruSayisi = getLong("soruSayisi")?.toInt() ?: 0,
        )
    }.getOrNull()

    companion object {
        val ALT_KOLEKSIYONLAR = listOf("hedefler", "ilerleme", "gunler", "denemeler", "sonuclar")
    }
}
