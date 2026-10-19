# Lisansla

Sermaye piyasası lisanslama sınavlarına (şimdilik Düzey 1) hazırlık için Android uygulaması.

## İndir

En son sürüm: https://github.com/Zatachi/HadiBasla/releases/latest/download/Lisansla.apk

## Nasıl çalışır

- **Sorular** `sorular/sorular.json` dosyasındadır. Uygulama her açılışta bu dosyanın güncel halini indirir; yeni soru eklemek için uygulamayı yeniden derlemek gerekmez. İnternet yoksa son indirilen sorular kullanılır.
- **Geri bildirim** Firebase'e (proje: `lisans-hazirlik`) anonim olarak gider:
  - `cozumler`: her cevaplanan soru, seçilen şık, doğru mu
  - `oylar`: "Bu soru işine yaradı mı?" oyları (soru başına kişi başı bir oy)
  - `hatalar`: "Hata bildir" mesajları
  - `gorusler`: "Görüşünü yaz" ekranından gelen yorumlar
- **Güvenlik kuralları** `firestore.rules` dosyasındadır; Firebase konsolunda Firestore → Rules bölümüne yapıştırılır.
- **Derleme** `main` dalına kod gönderilince GitHub Actions'da otomatik yapılır ve APK "Releases" sayfasına konur.

## Soru formatı

```json
{
  "id": "1001-0006",
  "ders": "1001",
  "konu": "1001-02",
  "soru": "Soru metni",
  "secenekler": ["A", "B", "C", "D", "E"],
  "dogru": 2,
  "aciklama": "Doğru cevabın nedeni",
  "kaynak": "Dayandığı kanun/tebliğ maddesi"
}
```

`dogru` sıfırdan başlar (0 = A, 4 = E). Soru eklendikten sonra `surum` bir artırılır.

## Telif notu

Sorular ve açıklamalar özgün olarak yazılır. SPL çalışma notları yalnızca hangi konuların sorulduğunu belirlemek için rehber olarak kullanılır; metinleri ve soruları kopyalanmaz.

## Play Store öncesi yapılacaklar

- `imza/lisansla-test.jks` yalnızca test içindir. Play Store'a yüklemeden önce gizli tutulan ayrı bir yükleme anahtarı oluşturulup GitHub Secrets'a taşınacak.
- `targetSdk` Play Store'un o tarihteki zorunlu seviyesine yükseltilecek.
- Gizlilik politikası ve KVKK aydınlatma metni hazırlanacak.
