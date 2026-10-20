package com.lisanshazirlik.app.data

/**
 * Odak ders: kullanıcının şu an çalıştığı ders. Shorts, Günün 5 dakikası, Kulaklık ve Düello
 * yalnızca bu dersten içerik getirir. Sıra: son açılan ders → en yakın sınav hedefi →
 * içeriği olan ilk ders → lisansın ilk dersi.
 */
object Odak {
    fun ders(banka: SoruBankasi, depo: KullaniciDeposu): Ders? {
        val dersler = banka.lisansDersleri(depo.kullanici?.lisans)
        val kodlar = dersler.map { it.kod }
        val kod = depo.kullanici?.sonPaket?.takeIf { it in kodlar }
            ?: depo.aktifHedefler().filter { it.ders in kodlar }.minByOrNull { it.kalanGun() ?: Long.MAX_VALUE }?.ders
            ?: dersler.firstOrNull { banka.dersSorulari(it.kod).isNotEmpty() }?.kod
            ?: dersler.firstOrNull()?.kod
        return kod?.let { banka.ders(it) }
    }

    fun sec(depo: KullaniciDeposu, kod: String) = depo.alanYaz(mapOf("sonPaket" to kod))
}
