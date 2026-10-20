package com.lisanshazirlik.app.data

/**
 * Hazır olma skoru (0-100): bir derste sınava ne kadar hazır olduğunu tek sayıda gösterir.
 *
 *  Kapsam    %30  Dersin sorularının ne kadarını gördün
 *  Doğruluk  %30  Gördüğün soruları ne kadar doğru biliyorsun (son cevaba göre)
 *  Deneme    %30  Son 3 ders denemesinin ortalaması (deneme yoksa ağırlığı diğerlerine dağılır)
 *  Hafıza    %10  Aralıklı tekrarda soruların ne kadar üst kutuya çıktı
 */
data class HazirlikSkoru(
    val ders: String,
    val toplam: Int,
    val kapsam: Int,
    val dogruluk: Int,
    val deneme: Int?,
    val hafiza: Int?,
) {
    val etiket: String
        get() = when {
            toplam >= 75 -> "Hazırsın"
            toplam >= 60 -> "Sınava Yakın"
            toplam >= 40 -> "Gelişiyor"
            else -> "Başlangıç"
        }

    /** En çok puan kazandıracak eksik. */
    val oneri: String
        get() = when {
            kapsam < 50 -> "Konuları bitir: dersin sorularının yarısından azını gördün."
            deneme == null -> "İlk ders denemeni çöz; skorun gerçek sınava daha çok benzesin."
            dogruluk < 60 -> "Yanlışlarını tekrar et; doğruluk oranın %60'ın altında."
            (deneme ?: 100) < 60 -> "Deneme puanın 60'ın altında; zayıf konularını çalışıp yeni deneme çöz."
            (hafiza ?: 100) < 50 -> "Günün 5 dakikası ile tekrar yap; öğrendiklerin kalıcı olsun."
            else -> "Harika gidiyorsun. Haftada bir deneme ile formunu koru."
        }
}

object Hazirlik {
    fun ders(banka: SoruBankasi, depo: KullaniciDeposu, ders: String): HazirlikSkoru? {
        val sorular = banka.dersSorulari(ders)
        if (sorular.isEmpty()) return null
        val il = depo.ilerleme
        val gorulen = sorular.filter { it.id in il }
        val kapsam = gorulen.size / sorular.size.toDouble()
        val dogruluk = if (gorulen.isEmpty()) 0.0 else gorulen.count { il[it.id] == 1 } / gorulen.size.toDouble()
        val son = depo.denemeler.filter { it.kapsam == ders }.sortedByDescending { it.zaman }.take(3)
        val deneme = if (son.isEmpty()) null else son.map { it.ortalama }.average() / 100.0
        val kutular = sorular.mapNotNull { depo.tekrar[it.id]?.first }
        val hafiza = if (kutular.isEmpty()) null else kutular.average() / 4.0

        val parcalar = listOfNotNull(0.3 to kapsam, 0.3 to dogruluk, deneme?.let { 0.3 to it }, hafiza?.let { 0.1 to it })
        val agirlik = parcalar.sumOf { it.first }
        var toplam = parcalar.sumOf { it.first * it.second } / agirlik
        // Deneme yoksa skor 70'i geçmez: gerçek sınav performansı görülmeden "hazırsın" denmez.
        if (deneme == null) toplam = minOf(toplam, 0.70)
        fun y(x: Double) = Math.round(x * 100).toInt()
        return HazirlikSkoru(ders, y(toplam), y(kapsam), y(dogruluk), deneme?.let(::y), hafiza?.let(::y))
    }

    /** Lisansın genel durumu: her ders en az 60 ve ortalama 70 ise lisansa hazır. */
    fun lisans(banka: SoruBankasi, depo: KullaniciDeposu, lisans: String?): Pair<Int, List<HazirlikSkoru>> {
        val skorlar = banka.lisansDersleri(lisans).mapNotNull { ders(banka, depo, it.kod) }
        val ort = if (skorlar.isEmpty()) 0 else skorlar.map { it.toplam }.average().toInt()
        return ort to skorlar
    }
}
