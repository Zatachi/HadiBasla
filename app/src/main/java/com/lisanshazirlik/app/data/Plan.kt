package com.lisanshazirlik.app.data

import java.time.LocalDate

enum class PlanTuru { ADIM, TEKRAR, SORU, DENEME, SHORT }

data class PlanGorevi(
    val tur: PlanTuru,
    val simge: String,
    val baslik: String,
    val alt: String,
    val tamam: Boolean,
    /** Eğitim adımı için adım kimliği. */
    val adimId: String? = null,
)

/**
 * Bugünkü plan: odak dersten kişiye özel 3-5 görev. Günün başında hangi adımın sırada olduğu
 * ve kaç tekrarın beklediği kaydedilir; gün içinde tamamlananlar buna göre işaretlenir.
 * kullanicilar/{uid}.plan = { gun, adim, tekrar }
 */
object Plan {
    const val SHORT_HEDEF = 3

    /** Günün ilk açılışında başlangıç durumunu kaydeder. */
    fun gunuBaslat(banka: SoruBankasi, depo: KullaniciDeposu) {
        val k = depo.kullanici ?: return
        val bugun = LocalDate.now().toString()
        if (k.planGun == bugun) return
        val odak = Odak.ders(banka, depo)?.kod ?: return
        depo.alanYaz(
            mapOf(
                "plan" to mapOf(
                    "gun" to bugun,
                    "adim" to Paket.siradaki(banka, depo, odak)?.id,
                    "tekrar" to Tekrar.bekleyenler(banka, depo, listOf(odak)).size,
                )
            )
        )
    }

    fun bugun(banka: SoruBankasi, depo: KullaniciDeposu, gunlukSoru: Int): List<PlanGorevi> {
        val k = depo.kullanici ?: return emptyList()
        val ders = Odak.ders(banka, depo) ?: return emptyList()
        val bugunKey = LocalDate.now().toString()
        val basladi = k.planGun == bugunKey
        val liste = mutableListOf<PlanGorevi>()

        // 1) Eğitim yolundaki sıradaki adım
        val baslangicAdim = if (basladi) k.planAdim else Paket.siradaki(banka, depo, ders.kod)?.id
        if (baslangicAdim != null) {
            val adim = Paket.adim(banka, ders.kod, baslangicAdim)
            if (adim != null) {
                val bitti = baslangicAdim in Paket.tamamlananlar(depo, ders.kod)
                liste += PlanGorevi(PlanTuru.ADIM, adim.tur.simge, adim.tur.ad, adim.konu?.ad ?: ders.kisaAd, bitti, baslangicAdim)
            }
        }
        // 2) Zamanı gelen tekrarlar
        val bekleyen = Tekrar.bekleyenler(banka, depo, listOf(ders.kod)).size
        val baslangicTekrar = if (basladi) k.planTekrar else bekleyen
        if (baslangicTekrar > 0) {
            liste += PlanGorevi(
                PlanTuru.TEKRAR, "🔁", "Günün 5 Dakikası",
                if (bekleyen == 0) "Tekrarlar Bitti" else "$bekleyen Soru Tekrar Zamanında", bekleyen == 0,
            )
        }
        // 3) Günlük soru hedefi
        if (banka.dersSorulari(ders.kod).isNotEmpty()) {
            liste += PlanGorevi(
                PlanTuru.SORU, "✏️", "$gunlukSoru Soru Çöz",
                "Bugün ${depo.bugunCozulen}/$gunlukSoru", depo.bugunCozulen >= gunlukSoru,
            )
        }
        // 4) Haftalık deneme: son 7 günde bu dersten deneme yoksa
        val sonDeneme = depo.denemeler.filter { it.kapsam == ders.kod }.maxByOrNull { it.zaman }
        val sonGun = sonDeneme?.let { java.time.Instant.ofEpochMilli(it.zaman).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
        val bugunDeneme = sonGun == LocalDate.now()
        if (banka.dersSorulari(ders.kod).isNotEmpty() && (sonGun == null || sonGun.isBefore(LocalDate.now().minusDays(6)) || bugunDeneme)) {
            liste += PlanGorevi(PlanTuru.DENEME, "⏱", "Haftalık Deneme", "${ders.kod} · 25 Soru, 45 Dakika", bugunDeneme)
        }
        // 5) Kısa videolar
        if (banka.kisalar.any { it.ders == ders.kod }) {
            val izlenen = depo.bugunShort()
            liste += PlanGorevi(PlanTuru.SHORT, "▶", "$SHORT_HEDEF Kısa Video İzle", "Bugün ${minOf(izlenen, SHORT_HEDEF)}/$SHORT_HEDEF", izlenen >= SHORT_HEDEF)
        }
        return liste
    }
}
