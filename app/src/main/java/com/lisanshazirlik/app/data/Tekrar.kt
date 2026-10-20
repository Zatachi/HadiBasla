package com.lisanshazirlik.app.data

import java.time.LocalDate

/**
 * Aralıklı tekrar (Leitner kutuları). Doğru cevap soruyu bir üst kutuya taşır ve daha geç sorulur;
 * yanlış cevap soruyu ilk kutuya geri atar ve ertesi gün tekrar sorulur.
 */
object Tekrar {
    private val ARALIK = listOf(1L, 3L, 7L, 21L, 60L)
    const val OTURUM = 10

    fun sonraki(kutu: Int?, dogru: Boolean): Pair<Int, LocalDate> {
        val yeni = if (dogru) ((kutu ?: -1) + 1).coerceAtMost(ARALIK.lastIndex) else 0
        return yeni to LocalDate.now().plusDays(if (dogru) ARALIK[yeni] else 1L)
    }

    /** Bugün tekrar zamanı gelmiş sorular (en eskisi önce). */
    fun bekleyenler(banka: SoruBankasi, depo: KullaniciDeposu, dersler: Collection<String>): List<Soru> {
        val bugun = LocalDate.now()
        return depo.tekrar.filter { (_, v) -> !v.second.isAfter(bugun) }
            .entries.sortedBy { it.value.second }
            .mapNotNull { banka.bul(it.key) }
            .filter { it.ders in dersler }
    }

    /** Günün 5 dakikası: önce zamanı gelenler, sonra eski yanlışlar, sonra hiç görülmemiş sorular. */
    fun oturum(banka: SoruBankasi, depo: KullaniciDeposu, dersler: Collection<String>): List<Soru> {
        val secilen = LinkedHashSet<Soru>()
        secilen += bekleyenler(banka, depo, dersler)
        if (secilen.size < OTURUM) secilen += depo.yanlislar().mapNotNull { banka.bul(it) }.filter { it.ders in dersler && it.id !in depo.tekrar }
        if (secilen.size < OTURUM) {
            // Yeni sorularda önce üzerinde çalıştığın konular (en az bir soru çözdüğün ya da son açtığın konu).
            val calisilan = depo.ilerleme.keys.mapNotNull { banka.bul(it)?.konu }.toSet() + listOfNotNull(depo.kullanici?.sonKonu)
            secilen += banka.sorular.filter { it.ders in dersler && it.id !in depo.ilerleme }.shuffled().sortedByDescending { it.konu in calisilan }
        }
        return secilen.take(OTURUM)
    }
}
