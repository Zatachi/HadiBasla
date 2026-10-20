package com.lisanshazirlik.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lisanshazirlik.app.data.Hesap
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Oyun
import com.lisanshazirlik.app.data.SiralamaSatiri
import com.lisanshazirlik.app.data.SoruBankasi
import com.lisanshazirlik.app.data.Tablo
import com.lisanshazirlik.app.data.Topluluk
import kotlinx.coroutines.launch

enum class BasariBolumu(val ad: String) { BEN("Ben"), SIRALAMA("Sıralama"), DAVET("Davet") }

/** Başarı sekmesi: seviye ve rozetler + istatistik, sıralamalar ve davet. */
@Composable
fun BasariSekme(
    banka: SoruBankasi,
    depo: KullaniciDeposu,
    hesap: Hesap,
    davetDurumu: Topluluk.DavetDurumu?,
    konuAc: (String) -> Unit,
    sonuclarAc: () -> Unit,
    ic: PaddingValues,
    ilkBolum: BasariBolumu = BasariBolumu.BEN,
) {
    var bolum by remember(ilkBolum) { mutableStateOf(ilkBolum) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.background(Yesil)) {
            SekmeBasligi("Başarı")
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasariBolumu.entries.forEach { b ->
                    val s = b == bolum
                    Text(
                        b.ad,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (s) YesilKoyu else Krem,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (s) Krem else YesilKoyu)
                            .clickableIf(true) { bolum = b }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
        when (bolum) {
            BasariBolumu.BEN -> IstatistikSekme(
                banka, depo, depo.kullanici?.lisans, konuAc, sonuclarAc, ic, baslikGoster = false,
            ) {
                SeviyeKarti(depo)
                RozetKarti(depo)
            }
            BasariBolumu.SIRALAMA -> SiralamaBolumu(banka, depo, hesap, ic)
            BasariBolumu.DAVET -> DavetBolumu(depo, hesap, davetDurumu, ic)
        }
    }
}

// ---------------------------------------------------------------- Ben

@Composable
fun SeviyeKarti(depo: KullaniciDeposu) {
    val xp = depo.kullanici?.xp ?: 0
    val seviye = Oyun.seviye(xp)
    val (oran, sonraki) = Oyun.ilerleme(xp)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Yesil).padding(18.dp),
    ) {
        Text("SEVİYEN", style = MaterialTheme.typography.labelMedium, color = Altin)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(seviye.ad, fontFamily = Baslik, fontSize = 30.sp, color = Krem, modifier = Modifier.weight(1f))
            Text("%,d XP".format(xp).replace(',', '.'), style = MaterialTheme.typography.titleMedium, color = Krem)
        }
        Bosluk(12)
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(YesilKoyu)) {
            Box(Modifier.fillMaxWidth(oran.coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Altin))
        }
        Bosluk(8)
        Text(
            if (sonraki == null) "En üst seviyedesin. Tebrikler!"
            else "${sonraki.ad} için ${"%,d".format(sonraki.altSinir - xp).replace(',', '.')} XP kaldı",
            style = MaterialTheme.typography.bodyMedium,
            color = KremSoluk,
        )
        Bosluk(6)
        Text(
            "Bu hafta: ${depo.kullanici?.buHaftaXp() ?: 0} XP · Doğru +${Oyun.XP_DOGRU}, deneme +${Oyun.XP_DENEME}, günlük hedef +${Oyun.XP_GUNLUK_HEDEF}",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            color = KremSoluk,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RozetKarti(depo: KullaniciDeposu) {
    val kazanilan = depo.kullanici?.rozetler.orEmpty()
    var secili by remember { mutableStateOf<Oyun.Rozet?>(null) }
    KartKutu {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rozetlerin", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${kazanilan.size} / ${Oyun.ROZETLER.size}", style = MaterialTheme.typography.labelLarge, color = Yesil)
        }
        Bosluk(12)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Oyun.ROZETLER.forEach { r ->
                val var_ = r.id in kazanilan
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (var_) YesilAcik else Zemin)
                        .border(2.dp, if (var_) Altin else Cizgi, CircleShape)
                        .clickableIf(true) { secili = r },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (var_) r.simge else "🔒", fontSize = if (var_) 24.sp else 16.sp)
                }
            }
        }
        secili?.let { r ->
            Bosluk(12)
            Text("${r.simge} ${r.ad}", style = MaterialTheme.typography.titleMedium)
            Text(
                r.aciklama + (kazanilan[r.id]?.let { " · Kazanıldı" } ?: " · Henüz Kazanılmadı"),
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
            )
        }
    }
}

// ---------------------------------------------------------------- Sıralama

@Composable
private fun SiralamaBolumu(banka: SoruBankasi, depo: KullaniciDeposu, hesap: Hesap, ic: PaddingValues) {
    val k = depo.kullanici ?: return
    val uid = hesap.uid ?: return
    if (!k.siralamaKatil) {
        KatilEkrani(banka, depo, uid, ic)
        return
    }
    val tablolar = remember(k.lisans) { Topluluk.tablolar(banka, k.lisans) }
    var tablo by remember { mutableStateOf(tablolar.first()) }
    var satirlar by remember { mutableStateOf<List<SiralamaSatiri>?>(null) }
    var benimSiram by remember { mutableStateOf<Long?>(null) }
    var benimDegerim by remember { mutableStateOf<Long?>(null) }
    var hata by remember { mutableStateOf(false) }
    var ayrilSor by remember { mutableStateOf(false) }

    LaunchedEffect(tablo.id) {
        satirlar = null; hata = false; benimSiram = null; benimDegerim = null
        try {
            satirlar = Topluluk.liste(tablo)
            val d = Topluluk.benimDegerim(uid, tablo)
            benimDegerim = d
            if (d != null) benimSiram = Topluluk.sira(tablo, d)
        } catch (e: Exception) {
            hata = true
        }
    }

    Column(
        Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()).verticalScroll(rememberScrollState()).padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tablolar.forEach { t ->
                val s = t.id == tablo.id
                Text(
                    t.ad,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (s) Krem else Metin,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (s) Yesil else Kart)
                        .border(1.dp, if (s) Yesil else Cizgi, RoundedCornerShape(18.dp))
                        .clickableIf(true) { tablo = t }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Text(tablo.aciklama, style = MaterialTheme.typography.bodyMedium, color = MetinSoluk, modifier = Modifier.padding(horizontal = 20.dp))

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                hata -> Text("Sıralama yüklenemedi. İnternet bağlantını kontrol et.", color = YanlisKirmizi)
                satirlar == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Yesil)
                }
                satirlar!!.isEmpty() -> Text("Bu sıralamada henüz kimse yok. İlk sen ol!", style = MaterialTheme.typography.bodyLarge)
                else -> satirlar!!.forEachIndexed { i, s -> SiralamaSatirGorunumu(i + 1L, s, tablo, s.uid == uid) }
            }
            val listede = satirlar?.any { it.uid == uid } == true
            if (!listede && satirlar != null && !hata) {
                Bosluk(4)
                if (benimSiram != null && benimDegerim != null) {
                    SiralamaSatirGorunumu(benimSiram!!, SiralamaSatiri(uid, k.takmaAd, Oyun.seviye(k.xp).ad, benimDegerim!!), tablo, true)
                } else {
                    Text("Bu Sıralamada Henüz Yer Almıyorsun.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                }
            }
        }

        TextButton(onClick = { ayrilSor = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("Takma Adım: ${k.takmaAd} · Sıralamadan Ayrıl", color = MetinSoluk)
        }
    }
    if (ayrilSor) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { ayrilSor = false },
            title = { Text("Sıralamadan Ayrıl") },
            text = { Text("Takma adın ve puanların sıralamalardan kaldırılır. Puanların ve rozetlerin hesabında kalmaya devam eder.") },
            confirmButton = { TextButton(onClick = { Topluluk.ayril(uid, depo); ayrilSor = false }) { Text("Ayrıl", color = YanlisKirmizi) } },
            dismissButton = { TextButton(onClick = { ayrilSor = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun SiralamaSatirGorunumu(sira: Long, s: SiralamaSatiri, tablo: Tablo, ben: Boolean) {
    val madalya = when (sira) { 1L -> "🥇"; 2L -> "🥈"; 3L -> "🥉"; else -> null }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (ben) YesilAcik else Kart)
            .border(if (ben) 2.dp else 1.dp, if (ben) Yesil else Cizgi, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(madalya ?: "$sira", fontSize = if (madalya != null) 22.sp else 15.sp, fontWeight = FontWeight.SemiBold, color = MetinSoluk, modifier = Modifier.width(40.dp))
        Box(Modifier.size(36.dp).clip(CircleShape).background(Yesil), contentAlignment = Alignment.Center) {
            Text(basHarf(s.takmaAd), color = Krem, style = MaterialTheme.typography.labelLarge)
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(s.takmaAd + if (ben) " (Sen)" else "", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(s.seviye, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = MetinSoluk)
        }
        Text(
            if (tablo.id == "duvar") "🎓" else "${s.deger}${if (tablo.birim.isNotBlank()) " " + tablo.birim else ""}",
            style = MaterialTheme.typography.titleMedium,
            color = Yesil,
        )
    }
}

@Composable
private fun KatilEkrani(banka: SoruBankasi, depo: KullaniciDeposu, uid: String, ic: PaddingValues) {
    var ad by remember { mutableStateOf("") }
    var onay by remember { mutableStateOf(false) }
    var duvar by remember { mutableStateOf(false) }
    var akis by remember { mutableStateOf(false) }
    var hata by remember { mutableStateOf<String?>(null) }
    var bekle by remember { mutableStateOf(false) }
    val kapsam = rememberCoroutineScope()
    val gecerli = Topluluk.adGecerli(ad)
    Column(
        Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🏆", fontSize = 40.sp)
        Text("Sıralamalara Katıl", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Haftalık ligde yarış, ilk 3'e girersen ${Oyun.HAFTALIK_ODUL_GUN} gün Premium kazan. Sıralamalarda gerçek adın değil, seçtiğin takma ad görünür.",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (depo.kullanici?.adCakisma == true) {
            Text(
                "\"${depo.kullanici?.takmaAd}\" adı başka bir kullanıcıya ait. Sıralamada görünmek için kendine yeni bir takma ad seç.",
                style = MaterialTheme.typography.bodyMedium,
                color = YanlisKirmizi,
            )
        }
        OutlinedTextField(
            value = ad,
            onValueChange = { ad = it.take(20); hata = null },
            supportingText = {
                Text(
                    hata ?: "2-20 karakter; harf, rakam, boşluk, nokta, tire ve alt çizgi. Her ad tek kişiye ait olur.",
                    color = if (hata != null) YanlisKirmizi else MetinSoluk,
                )
            },
            label = { Text("Takma Adın") },
            placeholder = { Text("Boğa34") },
            singleLine = true,
            isError = hata != null || (ad.isNotEmpty() && !gecerli),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickableIf(true) { onay = !onay }) {
            Checkbox(checked = onay, onCheckedChange = { onay = it })
            Text("Takma Adımın, Seviyemin ve Puanlarımın Sıralamalarda Diğer Kullanıcılara Gösterilmesine Açık Rıza Veriyorum.", style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickableIf(true) { duvar = !duvar }) {
            Checkbox(checked = duvar, onCheckedChange = { duvar = it })
            Text("Sınavı Geçtiğimde \"Geçenler Duvarı\"nda da Görüneyim (İsteğe Bağlı).", style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickableIf(true) { akis = !akis }) {
            Checkbox(checked = akis, onCheckedChange = { akis = it })
            Text("Başarılarım (Sınavı Geçmek, Yüksek Deneme Puanı, Düello Galibiyeti, Yeni Rozet) Ana Sayfadaki Topluluk Akışında Takma Adımla Görünsün (İsteğe Bağlı).", style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = {
                bekle = true
                kapsam.launch {
                    try {
                        Topluluk.katil(uid, depo, banka, ad, duvar, akis)
                    } catch (e: Topluluk.AdAlinmis) {
                        hata = e.message
                    } catch (e: Exception) {
                        hata = "Bağlantı kurulamadı. İnternetini kontrol edip tekrar dene."
                    }
                    bekle = false
                }
            },
            enabled = gecerli && onay && !bekle,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Katıl") }
        Text(
            "İstediğin an ayrılabilirsin; ayrılınca sıralama kaydın silinir. Ayrıntılar aydınlatma metninde.",
            style = MaterialTheme.typography.bodyMedium,
            color = MetinSoluk,
        )
    }
}

// ---------------------------------------------------------------- Davet

@Composable
private fun DavetBolumu(depo: KullaniciDeposu, hesap: Hesap, durum: Topluluk.DavetDurumu?, ic: PaddingValues) {
    val baglam = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val uid = hesap.uid ?: return
    val k = depo.kullanici
    var kod by remember { mutableStateOf(k?.davetKodu) }
    var girilen by remember { mutableStateOf("") }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var bekliyor by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (kod == null) kod = runCatching { Topluluk.davetKodu(uid, depo) }.getOrNull()
    }

    val gecerli = durum?.gecerli ?: 0
    Column(
        Modifier.fillMaxSize().padding(bottom = ic.calculateBottomPadding()).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Yesil).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("DAVET KODUN", style = MaterialTheme.typography.labelMedium, color = Altin)
            Bosluk(6)
            Text(kod?.let { it.take(3) + "-" + it.drop(3) } ?: "…", fontFamily = Baslik, fontSize = 36.sp, color = Krem)
            Bosluk(12)
            Button(
                onClick = {
                    val k2 = kod ?: return@Button
                    val metin = "Sermaye piyasası lisans sınavına Lisansla ile hazırlanıyorum: konu özetleri, bilgi kartları, deneme sınavları ve video anlatımlar var. " +
                        "Kayıt olurken davet kodumu gir, ikimiz de puan kazanalım: ${k2.take(3)}-${k2.drop(3)}\n" +
                        "İndir: https://github.com/Zatachi/HadiBasla/releases/latest/download/Lisansla.apk"
                    val gonder = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, metin)
                    baglam.startActivity(Intent.createChooser(gonder, "Lisansla'yı Paylaş"))
                    depo.paylasildi()
                },
                enabled = kod != null,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Altin, contentColor = YesilKoyu),
            ) { Text("Arkadaşlarınla Paylaş") }
            Bosluk(6)
            Text("Paylaşım başına +${Oyun.XP_PAYLASIM} XP (günde ${Oyun.PAYLASIM_GUNLUK_SINIR} kez)", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = KremSoluk)
        }

        KartKutu {
            Text("Davetlerin", style = MaterialTheme.typography.titleMedium)
            Bosluk(6)
            BilgiSatiri("Kodunla Katılan", "${durum?.toplam ?: 0}")
            BilgiSatiri("Sayılan (en az ${Oyun.DAVET_GECERLI_SORU} soru çözen)", "$gecerli")
            Bosluk(6)
            Text(
                "Arkadaşın Google hesabıyla kayıt olup kodunu girer ve ${Oyun.DAVET_GECERLI_SORU} soru çözerse davetin sayılır. Her sayılan davet +${Oyun.XP_DAVET_EDEN} XP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MetinSoluk,
            )
        }

        KartKutu {
            Text("Davet Ödülleri", style = MaterialTheme.typography.titleMedium)
            Bosluk(8)
            OdulSatiri("🤝", "1 Arkadaş", "Tavsiye Rozeti", gecerli >= 1)
            Oyun.DAVET_ODULLERI.forEach { (n, gun) ->
                OdulSatiri("🎁", "$n Arkadaş", "$gun Gün Premium", gecerli >= n)
            }
            OdulSatiri("🌟", "10 Arkadaş", "Fenomen Rozeti", gecerli >= 10)
            k?.odulPremiumBitis?.let {
                Bosluk(6)
                Text("Premium: ${uzunTarih(it)} Tarihine Kadar", style = MaterialTheme.typography.bodyMedium, color = DogruYesil)
            }
        }

        if (k?.davetEden == null) {
            KartKutu {
                Text("Davet Kodun Var Mı?", style = MaterialTheme.typography.titleMedium)
                Bosluk(4)
                Text("Seni davet edenin kodunu gir, sen de +${Oyun.XP_DAVET_EDILEN} XP hoş geldin bonusu kazan.", style = MaterialTheme.typography.bodyMedium, color = MetinSoluk)
                Bosluk(8)
                OutlinedTextField(
                    value = girilen,
                    onValueChange = { girilen = it.take(12); mesaj = null },
                    singleLine = true,
                    placeholder = { Text("LSN-XXXXX") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Bosluk(8)
                OutlinedButton(
                    onClick = {
                        bekliyor = true
                        kapsam.launch {
                            mesaj = try {
                                Topluluk.koduKullan(uid, hesap.misafir, depo, girilen); "Kod Kabul Edildi!"
                            } catch (e: Topluluk.DavetHatasi) {
                                e.message
                            } catch (e: Exception) {
                                "Kod kullanılamadı. İnternet bağlantını kontrol edip tekrar dene."
                            }
                            bekliyor = false
                        }
                    },
                    enabled = girilen.length >= 5 && !bekliyor,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Kodu Kullan") }
                mesaj?.let { Bosluk(6); Text(it, color = if (it.startsWith("Kod Kabul")) DogruYesil else YanlisKirmizi) }
            }
        }
        Text(
            "Puanlar ve ödüller uygulama içi kullanım içindir; paraya çevrilemez ve devredilemez. Hileli kullanım tespit edilirse ödüller geri alınabilir.",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            color = MetinSoluk,
        )
    }
}

@Composable
private fun OdulSatiri(simge: String, kosul: String, odul: String, tamam: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(simge, fontSize = 20.sp, modifier = Modifier.width(34.dp))
        Text(kosul, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(if (tamam) "✓ $odul" else odul, style = MaterialTheme.typography.bodyMedium, color = if (tamam) DogruYesil else MetinSoluk)
    }
}
