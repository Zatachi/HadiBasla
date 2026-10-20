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
    // ---- oyunlaştırma ----
    val xp: Int = 0,
    val xpHafta: Int = 0,
    val hafta: String = "",
    val enUzunSeri: Int = 0,
    val rozetler: Map<String, String> = emptyMap(),
    val davetKodu: String? = null,
    val davetEden: String? = null,
    val odulPremiumBitis: LocalDate? = null,
    val siralamaKatil: Boolean = false,
    val takmaAd: String = "",
    val duvar: Boolean = false,
    val paylasimGun: String = "",
    val paylasimSayi: Int = 0,
    val hedefOdulGunu: String = "",
    val haftaOdulu: String = "",
    val davetOdulu: Int = 0,
    val davetXp: Int = 0,
    val davetGecerli: Boolean = false,
    /** Eğitim paketlerinde tamamlanan adımlar: ders kodu → adım kimlikleri. */
    val paketler: Map<String, List<String>> = emptyMap(),
    val sonPaket: String? = null,
) {
    /** Bu haftanın XP'si (hafta değiştiyse 0). */
    fun buHaftaXp(): Int = if (hafta == Oyun.hafta()) xpHafta else 0

    fun premiumAktif(): Boolean = premium || (odulPremiumBitis?.let { !it.isBefore(LocalDate.now()) } ?: false)
}

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
    /** Gösterilmeyi bekleyen yeni rozet ya da seviye (ekran gösterip temizler). */
    var kutlama by mutableStateOf<String?>(null)

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

    /** XP ekler; hafta değiştiyse haftalık XP sıfırdan başlar. Seviye atlanırsa kutlama gösterilir. */
    fun xpEkle(miktar: Int, ekAlanlar: Map<String, Any?> = emptyMap()) {
        if (miktar <= 0 && ekAlanlar.isEmpty()) return
        val k = kullanici
        val buHafta = Oyun.hafta()
        val alanlar = mutableMapOf<String, Any?>("xp" to FieldValue.increment(miktar.toLong()))
        if (k?.hafta == buHafta) alanlar["xpHafta"] = FieldValue.increment(miktar.toLong())
        else { alanlar["hafta"] = buHafta; alanlar["xpHafta"] = miktar }
        alanlar.putAll(ekAlanlar)
        kok.set(alanlar, SetOptions.merge())
        val once = Oyun.seviye(k?.xp ?: 0)
        val sonra = Oyun.seviye((k?.xp ?: 0) + miktar)
        if (sonra != once) kutlama = "Yeni seviye: ${sonra.ad}"
    }

    fun cevapKaydet(soru: Soru, dogru: Boolean, gunlukHedef: Int = Int.MAX_VALUE) {
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
        var xp = if (dogru) Oyun.XP_DOGRU else Oyun.XP_YANLIS
        val ek = mutableMapOf<String, Any?>()
        if (k?.sonGun != bugunKey) {
            ek["seri"] = yeniSeri
            ek["sonGun"] = bugunKey
            if (yeniSeri > (k?.enUzunSeri ?: 0)) ek["enUzunSeri"] = yeniSeri
            xp += Oyun.XP_YENI_GUN
        }
        if (bugunCozulen + 1 >= gunlukHedef && k?.hedefOdulGunu != bugunKey) {
            ek["hedefOdulGunu"] = bugunKey
            xp += Oyun.XP_GUNLUK_HEDEF
            kutlama = "Bugünkü hedefini tamamladın! +${Oyun.XP_GUNLUK_HEDEF} XP"
        }
        xpEkle(xp, ek)
    }

    /** Yeni kazanılan rozetleri kaydeder, her biri için XP verir. */
    fun rozetleriGuncelle(hakEdilen: Set<String>) {
        val k = kullanici ?: return
        val yeni = hakEdilen - k.rozetler.keys
        if (yeni.isEmpty()) return
        val bugun = bugunKey
        xpEkle(Oyun.XP_ROZET * yeni.size, mapOf("rozetler" to yeni.associateWith { bugun }))
        val r = Oyun.ROZETLER.firstOrNull { it.id in yeni }
        if (r != null) kutlama = "${r.simge} Yeni rozet: ${r.ad}"
    }

    fun paylasildi() {
        val k = kullanici ?: return
        val sayi = if (k.paylasimGun == bugunKey) k.paylasimSayi else 0
        if (sayi >= Oyun.PAYLASIM_GUNLUK_SINIR) return
        xpEkle(Oyun.XP_PAYLASIM, mapOf("paylasimGun" to bugunKey, "paylasimSayi" to sayi + 1))
    }

    /** Premium süresini gün olarak uzatır (ödül). */
    fun premiumUzat(gun: Int, ekAlanlar: Map<String, Any?> = emptyMap()) {
        val simdi = LocalDate.now()
        val bas = kullanici?.odulPremiumBitis?.takeIf { !it.isBefore(simdi) } ?: simdi
        kok.set(ekAlanlar + mapOf("odulPremiumBitis" to bas.plusDays(gun.toLong()).toString()), SetOptions.merge())
        kutlama = "🎁 $gun gün Premium kazandın!"
    }

    /** Paket adımını tamamlar; paketin son adımıysa ödül verir. */
    fun paketAdimTamam(banka: SoruBankasi, ders: String, adimId: String) {
        val once = Paket.tamamlananlar(this, ders)
        if (adimId in once) return
        kok.set(mapOf("paketler" to mapOf(ders to FieldValue.arrayUnion(adimId)), "sonPaket" to ders), SetOptions.merge())
        val tum = Paket.adimlar(banka, ders).map { it.id }.toSet()
        if (tum.isNotEmpty() && tum.all { it in once || it == adimId }) {
            xpEkle(Paket.XP_PAKET, mapOf("rozetler" to mapOf("paket_tamam" to bugunKey)))
            kutlama = "🎓 ${banka.ders(ders)?.kisaAd ?: ders} paketini bitirdin! +${Paket.XP_PAKET} XP"
        }
    }

    fun paketSifirla(ders: String) {
        kok.set(mapOf("paketler" to mapOf(ders to FieldValue.delete())), SetOptions.merge())
    }

    fun alanYaz(alanlar: Map<String, Any?>) {
        kok.set(alanlar, SetOptions.merge())
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
        xpEkle(Oyun.XP_DENEME + ortalama)
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
        xp = getLong("xp")?.toInt() ?: 0,
        xpHafta = getLong("xpHafta")?.toInt() ?: 0,
        hafta = getString("hafta").orEmpty(),
        enUzunSeri = getLong("enUzunSeri")?.toInt() ?: 0,
        rozetler = (get("rozetler") as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap(),
        davetKodu = getString("davetKodu"),
        davetEden = getString("davetEden"),
        odulPremiumBitis = getString("odulPremiumBitis")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        siralamaKatil = getBoolean("siralamaKatil") ?: false,
        takmaAd = getString("takmaAd").orEmpty(),
        duvar = getBoolean("duvar") ?: false,
        paylasimGun = getString("paylasimGun").orEmpty(),
        paylasimSayi = getLong("paylasimSayi")?.toInt() ?: 0,
        hedefOdulGunu = getString("hedefOdulGunu").orEmpty(),
        haftaOdulu = getString("haftaOdulu").orEmpty(),
        davetOdulu = getLong("davetOdulu")?.toInt() ?: 0,
        davetXp = getLong("davetXp")?.toInt() ?: 0,
        davetGecerli = getBoolean("davetGecerli") ?: false,
        paketler = (get("paketler") as? Map<*, *>)?.entries?.associate { (k, v) ->
            k.toString() to ((v as? List<*>)?.map { it.toString() } ?: emptyList())
        } ?: emptyMap(),
        sonPaket = getString("sonPaket"),
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
