package com.lisanshazirlik.app.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Ürün ölçümü: hangi ekranlar kullanılıyor, nerede bırakılıyor, hangi özellik işe yarıyor.
 * Ad, e-posta gibi kimlik bilgisi gönderilmez; reklam kimliği toplanmaz (AndroidManifest).
 */
object Olcum {
    private var fa: FirebaseAnalytics? = null

    fun baslat(baglam: Context) {
        fa = FirebaseAnalytics.getInstance(baglam)
    }

    fun ekran(ad: String) = olay(FirebaseAnalytics.Event.SCREEN_VIEW, FirebaseAnalytics.Param.SCREEN_NAME to ad)

    fun olay(ad: String, vararg p: Pair<String, Any?>) {
        val b = Bundle()
        p.forEach { (k, v) ->
            when (v) {
                is Int -> b.putLong(k, v.toLong())
                is Long -> b.putLong(k, v)
                is Boolean -> b.putLong(k, if (v) 1 else 0)
                null -> Unit
                else -> b.putString(k, v.toString().take(100))
            }
        }
        runCatching { fa?.logEvent(ad, b) }
    }

    fun ozellik(ad: String, deger: String?) = runCatching { fa?.setUserProperty(ad, deger) }

    /** Hata raporlarında kullanıcıyı ayırt etmek için yalnızca anonim kimlik. */
    fun kullanici(uid: String?) = runCatching { FirebaseCrashlytics.getInstance().setUserId(uid ?: "") }
}
