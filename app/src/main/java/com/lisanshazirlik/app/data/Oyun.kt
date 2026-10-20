package com.lisanshazirlik.app.data

import java.time.LocalDate
import java.time.temporal.IsoFields

/** Puan (XP), seviye, rozet ve davet ödülü kuralları tek yerde. */
object Oyun {
    // ---- XP kuralları ----
    const val XP_DOGRU = 10
    const val XP_YANLIS = 2
    const val XP_YENI_GUN = 20
    const val XP_GUNLUK_HEDEF = 50
    const val XP_DENEME = 100 // + deneme ortalaması kadar ek
    const val XP_ROZET = 50
    const val XP_PAYLASIM = 20
    const val PAYLASIM_GUNLUK_SINIR = 3
    const val XP_DAVET_EDEN = 500
    const val XP_DAVET_EDILEN = 200

    /** Davet edilen kişinin sayılması için çözmesi gereken soru sayısı. */
    const val DAVET_GECERLI_SORU = 20

    /** Haftalık ligde ilk 3'e girenlere verilen Premium günü. */
    const val HAFTALIK_ODUL_GUN = 7

    /** Kaç geçerli davette kaç gün Premium kazanılır (birikimli). */
    val DAVET_ODULLERI = listOf(3 to 7, 5 to 30, 10 to 90)

    /** ISO hafta anahtarı, ör. 2026W43. Haftalık lig pazartesi sıfırlanır. */
    fun hafta(t: LocalDate = LocalDate.now()): String =
        "%dW%02d".format(t.get(IsoFields.WEEK_BASED_YEAR), t.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

    fun gecenHafta(): String = hafta(LocalDate.now().minusWeeks(1))

    // ---- Seviyeler (piyasa temalı) ----
    data class Seviye(val ad: String, val altSinir: Int)

    val SEVIYELER = listOf(
        Seviye("Gözlemci", 0),
        Seviye("Yatırımcı", 1_000),
        Seviye("Analist", 5_000),
        Seviye("Stratejist", 15_000),
        Seviye("Piyasa Kurdu", 40_000),
    )

    fun seviye(xp: Int): Seviye = SEVIYELER.last { xp >= it.altSinir }

    /** Bir sonraki seviyeye ilerleme 0..1 ve sonraki seviye (en üstteyse null). */
    fun ilerleme(xp: Int): Pair<Float, Seviye?> {
        val i = SEVIYELER.indexOfLast { xp >= it.altSinir }
        val sonraki = SEVIYELER.getOrNull(i + 1) ?: return 1f to null
        val su = SEVIYELER[i]
        return (xp - su.altSinir) / (sonraki.altSinir - su.altSinir).toFloat() to sonraki
    }

    // ---- Rozetler ----
    data class Rozet(val id: String, val simge: String, val ad: String, val aciklama: String)

    val ROZETLER = listOf(
        Rozet("ilk_soru", "🌱", "İlk adım", "İlk sorunu çözdün"),
        Rozet("soru_100", "💯", "Yüzlük", "100 soru çözdün"),
        Rozet("soru_500", "📈", "Yükselen trend", "500 soru çözdün"),
        Rozet("soru_1000", "🏦", "Bin işlem", "1000 soru çözdün"),
        Rozet("seri_7", "🔥", "Bir hafta", "7 gün üst üste çalıştın"),
        Rozet("seri_30", "☄️", "Bir ay", "30 gün üst üste çalıştın"),
        Rozet("deneme_ilk", "⏱", "İlk deneme", "İlk deneme sınavını bitirdin"),
        Rozet("deneme_80", "🎯", "Keskin nişancı", "Bir denemeden 80 ve üzeri aldın"),
        Rozet("konu_tam", "✅", "Konu tamam", "Bir konunun bütün sorularını doğru yaptın"),
        Rozet("ders_usta", "🧠", "Ders ustası", "Bir dersin sorularının %80'ini doğru yaptın"),
        Rozet("paket_tamam", "📦", "Paket tamam", "Bir eğitim paketini baştan sona bitirdin"),
        Rozet("sinav_gecti", "🎓", "Geçtim!", "Gerçek sınavda bir dersten geçtin"),
        Rozet("lisans", "🏆", "Lisanslı", "Lisans koşulunu sağladın"),
        Rozet("davet_1", "🤝", "Tavsiye", "Bir arkadaşını getirdin"),
        Rozet("davet_5", "📣", "Elçi", "5 arkadaşını getirdin"),
        Rozet("davet_10", "🌟", "Fenomen", "10 arkadaşını getirdin"),
        Rozet("lig_ilk3", "🥇", "Kürsü", "Haftalık ligde ilk 3'e girdin"),
    )

    /** Kullanıcının o anki durumuna göre hak ettiği rozetler. */
    fun hakEdilenRozetler(banka: SoruBankasi, depo: KullaniciDeposu, gecerliDavet: Int): Set<String> {
        val il = depo.ilerleme
        val k = depo.kullanici
        val s = mutableSetOf<String>()
        val cozulen = il.size
        if (cozulen >= 1) s += "ilk_soru"
        if (cozulen >= 100) s += "soru_100"
        if (cozulen >= 500) s += "soru_500"
        if (cozulen >= 1000) s += "soru_1000"
        val enUzun = maxOf(k?.enUzunSeri ?: 0, depo.seri())
        if (enUzun >= 7) s += "seri_7"
        if (enUzun >= 30) s += "seri_30"
        val denemeler = depo.denemeler.filter { !it.kapsam.startsWith("konu:") }
        if (denemeler.isNotEmpty()) s += "deneme_ilk"
        if (denemeler.any { it.ortalama >= 80 && it.soruSayisi >= 10 }) s += "deneme_80"
        if (banka.dersler.any { d -> d.konular.any { k2 ->
                val q = banka.konuSorulari(k2.id); q.size >= 3 && q.all { il[it.id] == 1 } } }) s += "konu_tam"
        if (banka.dersler.any { d ->
                val q = banka.dersSorulari(d.kod); q.size >= 10 && q.count { il[it.id] == 1 } >= q.size * 0.8 }) s += "ders_usta"
        val sonuclar = depo.sonuclar.values
        if (sonuclar.any { it.puan >= 50 }) s += "sinav_gecti"
        val lisansDersleri = banka.lisansDersleri(k?.lisans)
        val puanlar = lisansDersleri.mapNotNull { depo.sonuclar[it.kod]?.puan }
        if (lisansDersleri.isNotEmpty() && puanlar.size == lisansDersleri.size && puanlar.all { it >= 50 } && puanlar.average() >= 60) s += "lisans"
        if (gecerliDavet >= 1) s += "davet_1"
        if (gecerliDavet >= 5) s += "davet_5"
        if (gecerliDavet >= 10) s += "davet_10"
        if (k?.rozetler?.containsKey("lig_ilk3") == true) s += "lig_ilk3"
        if (banka.dersler.any { Paket.tamamMi(banka, depo, it.kod) }) s += "paket_tamam"
        return s
    }

    // ---- Davet kodu ----
    private const val HARFLER = "ABCDEFGHJKLMNPRSTUVYZ23456789"

    fun davetKoduUret(tohum: String, deneme: Int = 0): String {
        var h = (tohum + deneme).hashCode().toLong() and 0xffffffffL
        val sb = StringBuilder("LSN")
        repeat(5) { sb.append(HARFLER[(h % HARFLER.length).toInt()]); h /= HARFLER.length }
        return sb.toString()
    }

    fun koduDuzelt(girdi: String): String = girdi.uppercase().filter { it.isLetterOrDigit() }.let {
        if (it.startsWith("LSN")) it else "LSN$it"
    }
}
