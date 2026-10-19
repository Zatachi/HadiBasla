package com.lisanshazirlik.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lisanshazirlik.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Soru kalitesi için geri bildirim: çözüm, oy, hata bildirimi ve görüş.
 * Kayıtlar kullanıcı kimliğiyle tutulur ki hesap silinince hepsi silinebilsin.
 * İnternet yoksa Firestore bunları sıraya alır, bağlantı gelince gönderir.
 * Hiçbir hata uygulamayı durdurmaz.
 */
object GeriBildirim {
    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Giriş yapılmamışsa hiçbir şey gönderilmez.
    private fun kimlik(): String? = auth.currentUser?.uid

    private fun gonder(is_: suspend (String) -> Unit) {
        kapsam.launch {
            val uid = kimlik() ?: return@launch
            runCatching { is_(uid) }
        }
    }

    fun cozum(soruId: String, secim: Int, dogru: Boolean, mod: String) = gonder { uid ->
        db.collection("cozumler").add(
            mapOf(
                "uid" to uid,
                "soru" to soruId,
                "secim" to secim,
                "dogru" to dogru,
                "mod" to mod,
                "zaman" to FieldValue.serverTimestamp(),
                "surum" to BuildConfig.VERSION_NAME,
            )
        ).await()
    }

    fun oy(soruId: String, deger: Int) = gonder { uid ->
        db.collection("oylar").document("${soruId}__$uid").set(
            mapOf(
                "uid" to uid,
                "soru" to soruId,
                "oy" to deger,
                "zaman" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    fun hata(soruId: String, metin: String) = gonder { uid ->
        db.collection("hatalar").add(
            mapOf(
                "uid" to uid,
                "soru" to soruId,
                "metin" to metin.take(500),
                "zaman" to FieldValue.serverTimestamp(),
                "surum" to BuildConfig.VERSION_NAME,
            )
        ).await()
    }

    fun gorus(metin: String, puan: Int) = gonder { uid ->
        db.collection("gorusler").add(
            mapOf(
                "uid" to uid,
                "metin" to metin.take(2000),
                "puan" to puan,
                "zaman" to FieldValue.serverTimestamp(),
                "surum" to BuildConfig.VERSION_NAME,
            )
        ).await()
    }
}
