package com.lisanshazirlik.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.lisanshazirlik.app.MainActivity
import com.lisanshazirlik.app.R
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Her gün seçilen saatte "çalışma zamanı" bildirimi. Ayar sadece bu telefonda tutulur. */
object Hatirlatma {
    private const val IS_ADI = "gunluk_hatirlatma"
    const val KANAL = "hatirlatma"

    fun kur(baglam: Context, saat: Int, dakika: Int) {
        val simdi = LocalDateTime.now()
        var ilk = simdi.with(LocalTime.of(saat, dakika))
        if (!ilk.isAfter(simdi)) ilk = ilk.plusDays(1)
        val gecikme = Duration.between(simdi, ilk).toMinutes()
        val istek = PeriodicWorkRequestBuilder<HatirlatmaIsi>(24, TimeUnit.HOURS)
            .setInitialDelay(gecikme, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(baglam).enqueueUniquePeriodicWork(IS_ADI, ExistingPeriodicWorkPolicy.UPDATE, istek)
    }

    fun kapat(baglam: Context) {
        WorkManager.getInstance(baglam).cancelUniqueWork(IS_ADI)
    }

    fun izinVar(baglam: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(baglam, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun kanalOlustur(baglam: Context) {
        val kanal = NotificationChannel(KANAL, "Günlük hatırlatma", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Her gün seçtiğin saatte çalışma hatırlatması"
        }
        baglam.getSystemService(NotificationManager::class.java).createNotificationChannel(kanal)
    }
}

class HatirlatmaIsi(baglam: Context, parametreler: WorkerParameters) : Worker(baglam, parametreler) {
    override fun doWork(): Result {
        val b = applicationContext
        if (!Hatirlatma.izinVar(b)) return Result.success()
        Hatirlatma.kanalOlustur(b)
        val ac = PendingIntent.getActivity(
            b, 0,
            Intent(b, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val mesajlar = listOf(
            "Bugünkü hedefin seni bekliyor. Birkaç soruyla seriyi koru.",
            "Sınava bir gün daha yaklaştın. 10 dakikalık bir tekrar yapalım mı?",
            "Yanlışlarını tekrar çözmek için iyi bir zaman.",
        )
        val bildirim = NotificationCompat.Builder(b, Hatirlatma.KANAL)
            .setSmallIcon(R.drawable.ic_bildirim)
            .setContentTitle("Çalışma zamanı")
            .setContentText(mesajlar[(System.currentTimeMillis() / 86_400_000L % mesajlar.size).toInt()])
            .setContentIntent(ac)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(b).notify(1, bildirim)
        } catch (_: SecurityException) {
        }
        return Result.success()
    }
}
