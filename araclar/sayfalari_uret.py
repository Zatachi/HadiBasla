#!/usr/bin/env python3
"""metinler/aydinlatma.txt dosyasından docs/ altındaki web sayfalarını üretir.

Kullanım: python3 araclar/sayfalari_uret.py
GitHub Pages docs/ klasörünü yayınlar; Play Store gizlilik ve hesap silme bağlantıları buraya işaret eder.
"""
import html
import pathlib

KOK = pathlib.Path(__file__).resolve().parent.parent
METIN = KOK / "metinler"
CIKTI = KOK / "docs"


def bilgiler():
    sonuc = {}
    for satir in (METIN / "bilgiler.properties").read_text(encoding="utf-8").splitlines():
        satir = satir.strip()
        if not satir or satir.startswith("#") or "=" not in satir:
            continue
        k, v = satir.split("=", 1)
        sonuc[k.strip()] = v.strip()
    return sonuc


def doldur(metin, b):
    for k, v in b.items():
        metin = metin.replace("{{" + k + "}}", v)
    return metin


def html_govde(metin):
    """Basit biçim: '# ' başlık, '## ' alt başlık, '- ' madde, boş satır paragraf ayırır."""
    parcalar, liste = [], []

    def listeyi_kapat():
        if liste:
            parcalar.append("<ul>" + "".join(f"<li>{x}</li>" for x in liste) + "</ul>")
            liste.clear()

    for blok in metin.split("\n\n"):
        for satir in blok.strip().splitlines():
            s = html.escape(satir.strip())
            if s.startswith("## "):
                listeyi_kapat(); parcalar.append(f"<h2>{s[3:]}</h2>")
            elif s.startswith("# "):
                listeyi_kapat(); parcalar.append(f"<h1>{s[2:]}</h1>")
            elif s.startswith("- "):
                liste.append(s[2:])
            elif s:
                listeyi_kapat(); parcalar.append(f"<p>{s}</p>")
        listeyi_kapat()
    return "\n".join(parcalar)


STIL = """
:root{--zemin:#F7F3EA;--yazi:#1F2A24;--soluk:#5E6B63;--yesil:#1D4D3A;--krem:#F3E7CC;--altin:#D9A441;--cizgi:#E4DCC9}
@media (prefers-color-scheme:dark){:root{--zemin:#121a16;--yazi:#ECE6D8;--soluk:#A9B3AC;--yesil:#8FC3A8;--cizgi:#2B3830}}
*{box-sizing:border-box}
body{margin:0;background:var(--zemin);color:var(--yazi);font:17px/1.65 -apple-system,"Segoe UI",Roboto,sans-serif}
header{background:#1D4D3A;color:#F3E7CC;padding:28px max(20px,env(safe-area-inset-left))}
header a{color:#F3E7CC;text-decoration:none;font:28px Georgia,"Times New Roman",serif}
header span{display:block;color:#CBBF9F;font-size:13px;letter-spacing:.12em;margin-top:4px}
main{max-width:46rem;margin:0 auto;padding:24px max(20px,env(safe-area-inset-left)) 64px}
h1{font:400 30px/1.25 Georgia,"Times New Roman",serif;text-wrap:balance;margin:.4em 0}
h2{font:400 22px/1.3 Georgia,"Times New Roman",serif;margin:1.6em 0 .4em;padding-top:.6em;border-top:1px solid var(--cizgi)}
a{color:var(--yesil)}
li{margin:.3em 0}
.not{background:rgba(217,164,65,.14);border:1px solid rgba(217,164,65,.5);padding:14px 16px;border-radius:10px}
footer{color:var(--soluk);font-size:14px;margin-top:48px}
"""


def sayfa(baslik, govde):
    return f"""<!doctype html>
<html lang="tr"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<title>{html.escape(baslik)}</title><style>{STIL}</style></head>
<body><header><a href="./">Lisansla</a><span>SINAV HAZIRLIK</span></header>
<main>{govde}
<footer>Lisansla bağımsız bir çalışma aracıdır; SPL veya SPK ile bağlantılı değildir.</footer></main></body></html>
"""


def main():
    b = bilgiler()
    CIKTI.mkdir(exist_ok=True)
    aydinlatma = html_govde(doldur((METIN / "aydinlatma.txt").read_text(encoding="utf-8"), b))
    eposta = html.escape(b["EPOSTA"])

    gizlilik = f"""
<h1>Gizlilik politikası</h1>
<p>Lisansla'yı kullanırken gizliliğin bizim için önemli. Kısaca:</p>
<ul>
<li>Sadece uygulamanın çalışması için gereken verileri tutarız: adın, çalışma ilerlemen, hedeflerin ve geri bildirimlerin.</li>
<li>E-posta adresin veritabanımıza kaydedilmez.</li>
<li>Reklam yoktur, verilerin satılmaz, başka şirketlerle pazarlama için paylaşılmaz.</li>
<li>Konum, rehber, fotoğraf ve mikrofon gibi izinler istenmez.</li>
<li>Verilerin aktarım sırasında şifrelenir (HTTPS) ve hesabın dışındaki kimse göremez.</li>
<li>Hesabını istediğin an uygulama içinden silebilirsin; silince sana bağlı bütün kayıtlar silinir.</li>
</ul>
<p>Ayrıntılar aşağıdaki aydınlatma metninde. Soruların için: {eposta}</p>
{aydinlatma}
<h2 id="hesap-silme">Hesabını ve verilerini silme</h2>
<p>Uygulamada <b>Profil → Hesabımı sil</b> adımlarını izle. Onay verdiğinde hesabın, adın, çalışma ilerlemen, hedeflerin, deneme sonuçların, puanların, rozetlerin, sıralama ve davet kayıtların, oyların, hata bildirimlerin ve görüşlerin kalıcı olarak silinir. Bu işlem geri alınamaz.</p>
<p>Uygulamaya erişemiyorsan {eposta} adresine, uygulamada kullandığın Google hesabının e-posta adresinden "Hesap silme talebi" konulu bir e-posta gönder. Talebin en geç 30 gün içinde yerine getirilir.</p>
"""
    (CIKTI / "gizlilik.html").write_text(sayfa("Lisansla gizlilik politikası", gizlilik), encoding="utf-8")

    hesap = f"""
<h1>Hesap silme</h1>
<p>Lisansla hesabını ve sana bağlı bütün verileri iki yoldan silebilirsin:</p>
<ul>
<li><b>Uygulamadan:</b> Profil → Hesabımı sil. Hesabın, adın, çalışma ilerlemen, hedeflerin, deneme sonuçların, puanların, rozetlerin, sıralama ve davet kayıtların, oyların, hata bildirimlerin ve görüşlerin kalıcı olarak silinir.</li>
<li><b>E-postayla:</b> {eposta} adresine, uygulamada kullandığın Google hesabının e-postasından "Hesap silme talebi" konulu bir e-posta gönder. Talebin en geç 30 gün içinde yerine getirilir.</li>
</ul>
<p>Silme işleminden sonra verilerin geri getirilemez. Ayrıntılar için <a href="gizlilik.html">gizlilik politikası</a>.</p>
"""
    (CIKTI / "hesap-silme.html").write_text(sayfa("Lisansla hesap silme", hesap), encoding="utf-8")

    ana = """
<h1>Sermaye piyasası lisans sınavlarına cebinden hazırlan</h1>
<p>Lisansla; konu konu soru çözme, ders bazında sınav tarihi ve geri sayım, deneme sınavları ve yanlışlarını tekrar etme imkânı sunan ücretsiz bir Android uygulamasıdır.</p>
<ul>
<li><a href="gizlilik.html">Gizlilik politikası ve aydınlatma metni</a></li>
<li><a href="hesap-silme.html">Hesap silme</a></li>
</ul>
"""
    (CIKTI / "index.html").write_text(sayfa("Lisansla", ana), encoding="utf-8")
    print("docs/ güncellendi")


if __name__ == "__main__":
    main()
