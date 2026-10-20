package com.lisanshazirlik.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.lisanshazirlik.app.R
import kotlinx.coroutines.tasks.await

/** Giriş durumunu tutar. Uygulama giriş yöntemi olarak yalnızca Google ve misafir (anonim) kullanır. */
class Hesap {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var uid by mutableStateOf(auth.currentUser?.uid)
        private set
    var misafir by mutableStateOf(auth.currentUser?.isAnonymous ?: false)
        private set
    var eposta by mutableStateOf(auth.currentUser?.email)
        private set

    private val dinleyici = FirebaseAuth.AuthStateListener { yenile() }

    init {
        auth.addAuthStateListener(dinleyici)
    }

    private fun yenile() {
        val k = auth.currentUser
        uid = k?.uid
        misafir = k?.isAnonymous ?: false
        eposta = k?.email
    }

    /** Google'dan kimlik alır. Kullanıcı vazgeçerse [Vazgecildi] fırlatır. */
    private suspend fun googleKimligi(baglam: Context): Pair<AuthCredential, String?> {
        val secenek = GetSignInWithGoogleOption.Builder(baglam.getString(R.string.default_web_client_id)).build()
        val istek = GetCredentialRequest.Builder().addCredentialOption(secenek).build()
        val sonuc = try {
            CredentialManager.create(baglam).getCredential(baglam, istek)
        } catch (e: GetCredentialCancellationException) {
            throw Vazgecildi()
        } catch (e: NoCredentialException) {
            throw HesapHatasi("Telefonunda kullanılabilir bir Google hesabı bulunamadı. Ayarlar'dan bir Google hesabı ekleyip tekrar dene.")
        }
        val kimlik = sonuc.credential
        if (kimlik is CustomCredential && kimlik.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val g = GoogleIdTokenCredential.createFrom(kimlik.data)
            return GoogleAuthProvider.getCredential(g.idToken, null) to g.givenName
        }
        throw HesapHatasi("Google girişi tamamlanamadı. Tekrar dene.")
    }

    /**
     * Google ile giriş. Misafir olarak kullanılıyorsa hesap Google'a bağlanır, ilerleme korunur.
     * Dönen değer Google hesabındaki ilk ad (ad alanını önceden doldurmak için).
     */
    suspend fun googleIleGiris(baglam: Context): String? {
        val (kimlik, ad) = googleKimligi(baglam)
        val mevcut = auth.currentUser
        if (mevcut != null && mevcut.isAnonymous) {
            try {
                mevcut.linkWithCredential(kimlik).await()
            } catch (e: FirebaseAuthUserCollisionException) {
                throw ZatenKayitli(e.updatedCredential ?: kimlik)
            }
        } else {
            auth.signInWithCredential(kimlik).await()
        }
        yenile()
        return ad
    }

    /** Bu Google hesabı daha önce kullanılmışsa: misafir verilerini silip o hesaba geçer. */
    suspend fun misafiriBirakGoogleaGec(kimlik: AuthCredential) {
        verileriSil()
        runCatching { auth.currentUser?.delete()?.await() }
        auth.signInWithCredential(kimlik).await()
        yenile()
    }

    suspend fun misafirGiris() {
        auth.signInAnonymously().await()
        yenile()
    }

    suspend fun cikis(baglam: Context) {
        auth.signOut()
        runCatching { CredentialManager.create(baglam).clearCredentialState(ClearCredentialStateRequest()) }
        yenile()
    }

    /** Hesabı ve kullanıcıya bağlı bütün kayıtları kalıcı olarak siler. */
    suspend fun hesabiSil(baglam: Context) {
        val k = auth.currentUser ?: return
        if (!k.isAnonymous) {
            // Google hesabı silmeden önce kimliğin yeniden doğrulanmasını ister.
            val (kimlik, _) = googleKimligi(baglam)
            k.reauthenticate(kimlik).await()
        }
        verileriSil()
        k.delete().await()
        runCatching { CredentialManager.create(baglam).clearCredentialState(ClearCredentialStateRequest()) }
        yenile()
    }

    private suspend fun verileriSil() {
        val u = auth.currentUser?.uid ?: return
        val kok = db.collection("kullanicilar").document(u)
        val silinecekler = mutableListOf<com.google.firebase.firestore.DocumentReference>()
        for (alt in KullaniciDeposu.ALT_KOLEKSIYONLAR) {
            silinecekler += kok.collection(alt).get().await().documents.map { it.reference }
        }
        for (kol in listOf("cozumler", "oylar", "hatalar", "gorusler")) {
            silinecekler += db.collection(kol).whereEqualTo("uid", u).get().await().documents.map { it.reference }
        }
        silinecekler += db.collection("siralama").document(u)
        silinecekler += db.collection("takmaAdlar").whereEqualTo("uid", u).get().await().documents.map { it.reference }
        silinecekler += db.collection("duellolar").whereEqualTo("kurucu", u).get().await().documents.map { it.reference }
        silinecekler += db.collection("bekleyenler").document(u)
        silinecekler += db.collection("kodlar").whereEqualTo("uid", u).get().await().documents.map { it.reference }
        silinecekler += db.collection("davetler").whereEqualTo("davetEden", u).get().await().documents.map { it.reference }
        silinecekler += db.collection("davetler").document(u)
        silinecekler += kok
        silinecekler.chunked(400).forEach { parca ->
            val toplu = db.batch()
            parca.forEach { toplu.delete(it) }
            toplu.commit().await()
        }
    }

    class Vazgecildi : Exception()
    class HesapHatasi(mesaj: String) : Exception(mesaj)
    class ZatenKayitli(val kimlik: AuthCredential) : Exception()
}
