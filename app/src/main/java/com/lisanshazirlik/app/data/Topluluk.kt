package com.lisanshazirlik.app.data

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Sıralamada görünen bir satır. */
data class SiralamaSatiri(val uid: String, val takmaAd: String, val seviye: String, val deger: Long)

/** Bir sıralama tablosu: hangi alana göre ve nasıl gösterileceği. */
data class Tablo(val id: String, val ad: String, val alan: String, val birim: String, val aciklama: String)

/**
 * Sıralamalar ve davet sistemi.
 *
 * siralama/{uid}   Sadece katılmayı seçenler: takma ad, seviye, puanlar. Giriş yapmış herkes okuyabilir.
 * kodlar/{kod}     Davet kodunun sahibi.
 * davetler/{uid}   Davet edilen kişinin, kimin koduyla geldiği ve geçerli olup olmadığı.
 *
 * Not: Şimdilik puanları uygulama hesaplıyor (basit koruma). Premium ödülü açılmadan önce
 * hesaplama sunucuya (Cloud Functions) taşınacak.
 */
object Topluluk {
    private val db by lazy { FirebaseFirestore.getInstance() }

    fun tablolar(banka: SoruBankasi, lisans: String?): List<Tablo> = listOf(
        Tablo("lig", "Haftalık Lig", "xp_${Oyun.hafta()}", "XP", "Pazartesi sıfırlanır. İlk 3'e ${Oyun.HAFTALIK_ODUL_GUN} gün Premium."),
        Tablo("soru", "En Çok Soru", "cozulen", "soru", "Bugüne Kadar Çözülen Soru Sayısı."),
        Tablo("oran", "Doğru Oranı", "oranUygun", "%", "En Az 50 Soru Çözenler Arasında."),
        Tablo("deneme", "Deneme Şampiyonları", "enIyiDeneme", "puan", "Süreli Denemelerdeki En Yüksek Puan."),
        Tablo("seri", "En Uzun Seri", "enUzunSeri", "Gün", "Üst Üste En Çok Gün Çalışanlar."),
        Tablo("gelisim", "En Çok Gelişen", "gelisim", "puan", "İlk Denemesine Göre En Çok Puan Artıranlar."),
    ) + banka.lisansDersleri(lisans).map { d ->
        Tablo("ders_${d.kod}", d.kisaAd, "dersDogru_${d.kod}", "Doğru", "${d.ad} Dersinde En Çok Doğru Yapanlar.")
    } + Tablo("duvar", "Geçenler Duvarı", "gectiZaman", "", "Sınavı geçtiğini paylaşanlar. Tebrikler!")

    // ---------------------------------------------------------------- sıralama

    // ---- Takma adlar: her ad tek bir kullanıcıya ayrılır (takmaAdlar/{küçük harf ad} → uid).

    fun adAnahtari(ad: String): String = ad.trim().lowercase(java.util.Locale.ROOT)
    private val AD_DESENI = Regex("^[\\p{L}\\p{N}_.\\- ]{2,20}$")
    fun adGecerli(ad: String): Boolean = AD_DESENI.matches(ad.trim())

    class AdAlinmis : Exception("Bu takma ad başka biri tarafından kullanılıyor. Başka bir ad seç.")

    private var ayrilmis: String? = null

    /** Adı bu kullanıcıya ayırır. Başkasına aitse false döner. */
    suspend fun adAyir(uid: String, ad: String): Boolean {
        val anahtar = adAnahtari(ad)
        if (ayrilmis == "$uid/$anahtar") return true
        val ref = db.collection("takmaAdlar").document(anahtar)
        val snap = ref.get(com.google.firebase.firestore.Source.SERVER).await()
        val sonuc = if (snap.exists()) {
            snap.getString("uid") == uid
        } else {
            try {
                ref.set(mapOf("uid" to uid)).await(); true
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) false else throw e
            }
        }
        if (sonuc) ayrilmis = "$uid/$anahtar"
        return sonuc
    }

    private fun eskiAdlariBirak(uid: String, haric: String?) {
        db.collection("takmaAdlar").whereEqualTo("uid", uid).get().addOnSuccessListener { q ->
            q.documents.filter { it.id != haric }.forEach { it.reference.delete() }
        }
    }

    /** Kullanıcının herkese açık sıralama kaydını günceller (sadece katıldıysa). */
    suspend fun profilYaz(uid: String, depo: KullaniciDeposu, banka: SoruBankasi) {
        val k = depo.kullanici ?: return
        if (!k.siralamaKatil) return
        // Ad başkasına aitse (eski kayıtlarda aynı ad olabilir) kullanıcıdan yeni ad istenir.
        val sahip = try { adAyir(uid, k.takmaAd) } catch (e: Exception) { return }
        if (!sahip) {
            depo.alanYaz(mapOf("siralamaKatil" to false, "adCakisma" to true))
            db.collection("siralama").document(uid).delete()
            return
        }
        val il = depo.ilerleme
        val cozulen = il.size
        val dogru = il.values.count { it == 1 }
        val denemeler = depo.denemeler.filter { !it.kapsam.startsWith("konu:") && it.soruSayisi >= 5 }
        val enIyi = denemeler.maxOfOrNull { it.ortalama }
        val gelisim = if (denemeler.size >= 2) denemeler.maxOf { it.ortalama } - denemeler.last().ortalama else null
        val gecilen = depo.sonuclar.values.filter { it.puan >= 50 }
        val sil = FieldValue.delete()
        val veri = mutableMapOf<String, Any?>(
            "takmaAd" to k.takmaAd,
            "seviye" to Oyun.seviye(k.xp).ad,
            "xp" to k.xp,
            "xp_${Oyun.hafta()}" to k.buHaftaXp(),
            "cozulen" to cozulen,
            "dogru" to dogru,
            "oranUygun" to if (cozulen >= 50) Math.round(dogru * 100f / cozulen) else sil,
            "enIyiDeneme" to (enIyi ?: sil),
            "gelisim" to if (gelisim != null && gelisim > 0) gelisim else sil,
            "enUzunSeri" to maxOf(k.enUzunSeri, depo.seri()),
            "gectiDers" to gecilen.size,
            "gectiZaman" to if (k.duvar && gecilen.isNotEmpty()) (gecilen.mapNotNull { it.tarih }.maxOrNull()?.toEpochDay() ?: 1L) else sil,
            "zaman" to FieldValue.serverTimestamp(),
        )
        banka.dersler.forEach { d ->
            val n = banka.dersSorulari(d.kod).count { il[it.id] == 1 }
            veri["dersDogru_${d.kod}"] = if (n > 0) n else sil
        }
        db.collection("siralama").document(uid).set(veri, SetOptions.merge())
    }

    /** Adı ayırır ve sıralamaya katılır. Ad alınmışsa [AdAlinmis] fırlatır. */
    suspend fun katil(uid: String, depo: KullaniciDeposu, banka: SoruBankasi, takmaAd: String, duvar: Boolean) {
        val temiz = takmaAd.trim().replace(Regex("\\s+"), " ").take(20)
        if (!adAyir(uid, temiz)) throw AdAlinmis()
        eskiAdlariBirak(uid, adAnahtari(temiz))
        depo.alanYaz(mapOf("siralamaKatil" to true, "takmaAd" to temiz, "duvar" to duvar, "adCakisma" to false))
        // Kullanıcı kaydı güncellenince profilYaz ana ekrandan tekrar çağrılır; ilk kaydı hemen yazalım.
        val k = depo.kullanici ?: return
        db.collection("siralama").document(uid).set(
            mapOf("takmaAd" to temiz, "seviye" to Oyun.seviye(k.xp).ad, "xp" to k.xp, "zaman" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        ).await()
    }

    fun ayril(uid: String, depo: KullaniciDeposu) {
        depo.alanYaz(mapOf("siralamaKatil" to false))
        db.collection("siralama").document(uid).delete()
        ayrilmis = null
        eskiAdlariBirak(uid, null)
    }

    suspend fun liste(tablo: Tablo, sinir: Long = 50): List<SiralamaSatiri> {
        val q = db.collection("siralama").orderBy(tablo.alan, Query.Direction.DESCENDING).limit(sinir).get().await()
        return q.documents.map { d ->
            SiralamaSatiri(
                uid = d.id,
                takmaAd = d.getString("takmaAd") ?: "Anonim",
                seviye = d.getString("seviye") ?: "",
                deger = d.getLong(tablo.alan) ?: 0L,
            )
        }
    }

    /** Kullanıcının o tablodaki sırası (kendisinden yüksek olanların sayısı + 1). */
    suspend fun sira(tablo: Tablo, deger: Long): Long =
        db.collection("siralama").whereGreaterThan(tablo.alan, deger).count().get(AggregateSource.SERVER).await().count + 1

    suspend fun benimDegerim(uid: String, tablo: Tablo): Long? =
        db.collection("siralama").document(uid).get().await().getLong(tablo.alan)

    /** Geçen haftanın ligine bakar; ilk 3'teyse Premium ödülünü bir kez verir. */
    suspend fun haftalikOdulKontrol(uid: String, depo: KullaniciDeposu) {
        val k = depo.kullanici ?: return
        val gecen = Oyun.gecenHafta()
        if (!k.siralamaKatil || k.haftaOdulu == gecen) return
        val ilk3 = db.collection("siralama").orderBy("xp_$gecen", Query.Direction.DESCENDING).limit(3).get().await()
            .documents.filter { (it.getLong("xp_$gecen") ?: 0) > 0 }.map { it.id }
        if (uid in ilk3) {
            depo.premiumUzat(Oyun.HAFTALIK_ODUL_GUN, mapOf("haftaOdulu" to gecen, "rozetler" to mapOf("lig_ilk3" to gecen)))
        } else {
            depo.alanYaz(mapOf("haftaOdulu" to gecen))
        }
    }

    // ---------------------------------------------------------------- davet

    /** Kullanıcının davet kodunu getirir; yoksa üretip kaydeder. */
    suspend fun davetKodu(uid: String, depo: KullaniciDeposu): String? {
        depo.kullanici?.davetKodu?.let { return it }
        for (deneme in 0 until 5) {
            val kod = Oyun.davetKoduUret(uid, deneme)
            val belge = db.collection("kodlar").document(kod)
            val mevcut = belge.get().await()
            if (mevcut.exists() && mevcut.getString("uid") != uid) continue
            if (!mevcut.exists()) belge.set(mapOf("uid" to uid)).await()
            depo.alanYaz(mapOf("davetKodu" to kod))
            return kod
        }
        return null
    }

    class DavetHatasi(mesaj: String) : Exception(mesaj)

    /** Davet kodunu kullanır. Sadece Google hesabıyla ve bir kez kullanılabilir. */
    suspend fun koduKullan(uid: String, misafir: Boolean, depo: KullaniciDeposu, girdi: String) {
        val k = depo.kullanici ?: throw DavetHatasi("Hesap bilgisi yüklenemedi. Tekrar dene.")
        if (misafir) throw DavetHatasi("Davet Kodu Kullanmak İçin Önce Profil'den Google Hesabına Bağlan.")
        if (k.davetEden != null) throw DavetHatasi("Daha Önce Bir Davet Kodu Kullandın.")
        val kod = Oyun.koduDuzelt(girdi)
        val sahip = db.collection("kodlar").document(kod).get().await().getString("uid")
            ?: throw DavetHatasi("Bu kod bulunamadı. Harfleri kontrol edip tekrar dene.")
        if (sahip == uid) throw DavetHatasi("Kendi kodunu kullanamazsın.")
        db.collection("davetler").document(uid).set(
            mapOf("davetEden" to sahip, "kod" to kod, "gecerli" to false, "zaman" to FieldValue.serverTimestamp())
        ).await()
        depo.xpEkle(Oyun.XP_DAVET_EDILEN, mapOf("davetEden" to sahip))
        depo.kutlama = "🎁 Hoş geldin bonusu: +${Oyun.XP_DAVET_EDILEN} XP"
    }

    /** Davet edilen kişi yeterince soru çözünce davet "Geçerli" olur ve davet edene sayılır. */
    fun gecerlilikKontrol(uid: String, depo: KullaniciDeposu) {
        val k = depo.kullanici ?: return
        if (k.davetEden == null || k.davetGecerli || depo.ilerleme.size < Oyun.DAVET_GECERLI_SORU) return
        db.collection("davetler").document(uid).set(mapOf("gecerli" to true), SetOptions.merge())
        depo.alanYaz(mapOf("davetGecerli" to true))
    }

    data class DavetDurumu(val toplam: Int, val gecerli: Int)

    suspend fun davetDurumu(uid: String): DavetDurumu {
        val q = db.collection("davetler").whereEqualTo("davetEden", uid).get().await()
        return DavetDurumu(q.size(), q.documents.count { it.getBoolean("gecerli") == true })
    }

    /** Yeni geçerli davetler için XP ve kademeli Premium ödüllerini verir. */
    fun davetOdulleri(depo: KullaniciDeposu, gecerli: Int) {
        val k = depo.kullanici ?: return
        val yeniXp = (gecerli - k.davetXp).coerceAtLeast(0)
        if (yeniXp > 0) depo.xpEkle(Oyun.XP_DAVET_EDEN * yeniXp, mapOf("davetXp" to gecerli))
        val ulasilan = Oyun.DAVET_ODULLERI.count { gecerli >= it.first }
        if (ulasilan > k.davetOdulu) {
            val gun = Oyun.DAVET_ODULLERI.subList(k.davetOdulu, ulasilan).sumOf { it.second }
            depo.premiumUzat(gun, mapOf("davetOdulu" to ulasilan))
        }
    }
}
