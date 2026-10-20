package com.lisanshazirlik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lisanshazirlik.app.data.KullaniciDeposu
import com.lisanshazirlik.app.data.Odak
import com.lisanshazirlik.app.data.SoruBankasi

/** Lisansın dersleri yan yana çip olarak; seçilen ders tüm "odak" ekranlarında geçerli olur. */
@Composable
fun OdakSecici(banka: SoruBankasi, depo: KullaniciDeposu, secili: String?, koyu: Boolean = false, degisti: (String) -> Unit = {}) {
    val dersler = banka.lisansDersleri(depo.kullanici?.lisans)
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(dersler, key = { it.kod }) { d ->
            val s = d.kod == secili
            val bos = banka.dersSorulari(d.kod).isEmpty()
            Text(
                d.kod + " · " + d.kisaAd.take(22) + if (d.kisaAd.length > 22) "…" else "",
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    s -> Krem
                    koyu -> if (bos) Color(0x88F3EBD8) else Krem
                    bos -> MetinSoluk
                    else -> Metin
                },
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (s) dersRengi(d.kod) else if (koyu) Color(0x22FFFFFF) else Kart)
                    .border(1.dp, if (s) dersRengi(d.kod) else if (koyu) Color(0x33FFFFFF) else Cizgi, RoundedCornerShape(50))
                    .clickable { Odak.sec(depo, d.kod); degisti(d.kod) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}
