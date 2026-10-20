package com.lisanshazirlik.app.data

/** Eğitim paketi: bir dersin konularını sırayla öğren → tekrar et → soru çöz, sonunda ders denemesi. */
object Paket {
    enum class Tur(val simge: String, val ad: String) {
        VIDEO("🎬", "Video Anlatım"),
        OZET("📖", "Konu Özeti"),
        KARTLAR("🃏", "Bilgi Kartları"),
        SORU("✏️", "Konu Soruları"),
        DENEME("⏱", "Ders Denemesi"),
    }

    data class Adim(val id: String, val tur: Tur, val konu: Konu?, val baslik: String)

    const val XP_PAKET = 300

    /** İçeriği olmayan konular atlanır; içerik eklendikçe paket kendiliğinden uzar. */
    fun adimlar(banka: SoruBankasi, dersKod: String): List<Adim> {
        val ders = banka.ders(dersKod) ?: return emptyList()
        val liste = mutableListOf<Adim>()
        ders.konular.forEach { k ->
            when {
                k.video.isNotBlank() -> liste += Adim("${k.id}:video", Tur.VIDEO, k, k.ad)
                k.ozet.isNotBlank() -> liste += Adim("${k.id}:ozet", Tur.OZET, k, k.ad)
            }
            if (k.kartlar.isNotEmpty()) liste += Adim("${k.id}:kartlar", Tur.KARTLAR, k, k.ad)
            if (banka.konuSorulari(k.id).isNotEmpty()) liste += Adim("${k.id}:soru", Tur.SORU, k, k.ad)
        }
        if (banka.dersSorulari(dersKod).isNotEmpty()) liste += Adim("$dersKod:deneme", Tur.DENEME, null, ders.kisaAd)
        return liste
    }

    fun adim(banka: SoruBankasi, dersKod: String, id: String): Adim? = adimlar(banka, dersKod).firstOrNull { it.id == id }

    fun tamamlananlar(depo: KullaniciDeposu, dersKod: String): Set<String> =
        depo.kullanici?.paketler?.get(dersKod).orEmpty().toSet()

    fun siradaki(banka: SoruBankasi, depo: KullaniciDeposu, dersKod: String): Adim? {
        val bitti = tamamlananlar(depo, dersKod)
        return adimlar(banka, dersKod).firstOrNull { it.id !in bitti }
    }

    fun tamamMi(banka: SoruBankasi, depo: KullaniciDeposu, dersKod: String): Boolean {
        val a = adimlar(banka, dersKod)
        return a.isNotEmpty() && a.all { it.id in tamamlananlar(depo, dersKod) }
    }
}
