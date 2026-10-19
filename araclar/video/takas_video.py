#!/usr/bin/env python3
"""Lisansla animasyonlu anlatım: Pay Piyasası'nda T+2 takas.
Dikey 1080x1920, 30 fps. Kareler PIL ile 2 kat büyük çizilip küçültülür (yumuşak kenarlar), ffmpeg ile MP4 yapılır.
Kullanım: python3 takas_video.py [cikti.mp4] [--kare SANIYE]  (--kare ile tek bir kareyi PNG olarak çizer)
"""
import math
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

W, H, K, FPS = 1080, 1920, 2, 30

YESIL = (29, 77, 58)
YESIL_KOYU = (19, 53, 40)
YESIL_ACIK = (220, 233, 225)
KREM = (243, 231, 204)
KREM_SOLUK = (203, 191, 159)
ALTIN = (217, 164, 65)
ZEMIN = (247, 243, 234)
METIN = (31, 42, 36)
SOLUK = (94, 107, 99)
CIZGI = (228, 220, 201)
KIRMIZI = (179, 64, 47)
KIRMIZI_ZEMIN = (248, 227, 223)
BEYAZ = (255, 255, 255)

SERIF = "/usr/share/fonts/truetype/google-fonts/Lora-Variable.ttf"
SANS = "/usr/share/fonts/opentype/inter/Inter-Regular.otf"
SANS_K = "/usr/share/fonts/opentype/inter/Inter-SemiBold.otf"
_fontlar = {}


def font(yol, boyut):
    a = (yol, boyut)
    if a not in _fontlar:
        _fontlar[a] = ImageFont.truetype(yol, boyut * K)
    return _fontlar[a]


# ---------------------------------------------------------------- yardımcılar

def kis(x):
    return max(0.0, min(1.0, x))


def yumusak(x):
    x = kis(x)
    return 1 - (1 - x) ** 3


def gir(t, bas, sure=0.6):
    """bas anından itibaren 0→1 yumuşak geçiş."""
    return yumusak((t - bas) / sure)


def karisim(a, b, o):
    return tuple(int(a[i] + (b[i] - a[i]) * o) for i in range(3))


class Tuval:
    def __init__(self, zemin):
        self.zemin = zemin
        self.img = Image.new("RGB", (W * K, H * K), zemin)
        self.d = ImageDraw.Draw(self.img)

    def yazi(self, x, y, metin, yol, boyut, renk, o=1.0, capa="mm", dy=0.0):
        """o: görünürlük (zemine göre karıştırılır), dy: aşağıdan kayarak giriş."""
        if o <= 0:
            return
        r = karisim(self.zemin_at(x, y), renk, o)
        yy = y + (1 - o) * dy
        self.d.text((x * K, yy * K), metin, font=font(yol, boyut), fill=r, anchor=capa)

    def zemin_at(self, x, y):
        return self.img.getpixel((min(W * K - 1, int(x * K)), min(H * K - 1, int(y * K))))

    def sar(self, x, y, metin, yol, boyut, renk, genislik, o=1.0, satir=1.35, capa="mm", dy=0.0):
        f = font(yol, boyut)
        satirlar = []
        for paragraf in metin.split("\n"):
            s = ""
            for kelime in paragraf.split(" "):
                dene = (s + " " + kelime).strip()
                if f.getlength(dene) / K > genislik and s:
                    satirlar.append(s)
                    s = kelime
                else:
                    s = dene
            satirlar.append(s)
        yuk = boyut * satir
        bas = y - yuk * (len(satirlar) - 1) / 2 if capa[1] == "m" else y
        for i, s in enumerate(satirlar):
            self.yazi(x, bas + i * yuk, s, yol, boyut, renk, o, capa=capa[0] + "m", dy=dy)
        return len(satirlar) * yuk

    def kutu(self, x0, y0, x1, y1, renk, r=24, cizgi=None, kalin=3, o=1.0):
        if o <= 0:
            return
        z = self.zemin_at((x0 + x1) / 2, (y0 + y1) / 2)
        dolgu = karisim(z, renk, o)
        kenar = karisim(z, cizgi, o) if cizgi else None
        self.d.rounded_rectangle((x0 * K, y0 * K, x1 * K, y1 * K), r * K, fill=dolgu, outline=kenar, width=int(kalin * K))

    def daire(self, x, y, r, renk, cizgi=None, kalin=3):
        self.d.ellipse(((x - r) * K, (y - r) * K, (x + r) * K, (y + r) * K), fill=renk, outline=cizgi, width=int(kalin * K))

    def cizgi(self, x0, y0, x1, y1, renk, kalin=6):
        self.d.line((x0 * K, y0 * K, x1 * K, y1 * K), fill=renk, width=max(1, int(round(kalin * K))))
        for x, y in ((x0, y0), (x1, y1)):
            self.daire(x, y, kalin / 2, renk)

    def ok(self, x0, y0, x1, y1, renk, kalin=6, ilerleme=1.0):
        if ilerleme <= 0:
            return
        xe, ye = x0 + (x1 - x0) * ilerleme, y0 + (y1 - y0) * ilerleme
        self.cizgi(x0, y0, xe, ye, renk, kalin)
        a = math.atan2(ye - y0, xe - x0)
        u = 22
        p = [(xe, ye), (xe - u * math.cos(a - 0.5), ye - u * math.sin(a - 0.5)), (xe - u * math.cos(a + 0.5), ye - u * math.sin(a + 0.5))]
        self.d.polygon([(px * K, py * K) for px, py in p], fill=renk)

    def bitir(self):
        return self.img.resize((W, H), Image.LANCZOS)


def kalkan_noktalari(cx, cy, olcek):
    """Uygulama ikonundaki rozet (108'lik tuvalde merkez 54,54)."""
    def p(x, y):
        return cx + (x - 54) * olcek, cy + (y - 54) * olcek

    def bez(a, b, c, d, n=24):
        return [(
            (1 - t) ** 3 * a[0] + 3 * (1 - t) ** 2 * t * b[0] + 3 * (1 - t) * t * t * c[0] + t ** 3 * d[0],
            (1 - t) ** 3 * a[1] + 3 * (1 - t) ** 2 * t * b[1] + 3 * (1 - t) * t * t * c[1] + t ** 3 * d[1],
        ) for t in (i / n for i in range(n + 1))]

    pts = [p(54, 25.25), p(77.75, 34), p(77.75, 54)]
    pts += bez(p(77.75, 54), p(77.75, 69), p(66.5, 77.75), p(54, 82.75))[1:]
    pts += bez(p(54, 82.75), p(41.5, 77.75), p(30.25, 69), p(30.25, 54))[1:]
    pts += [p(30.25, 34)]
    return pts


def rozet(tv, cx, cy, olcek, l_ilerleme=1.0):
    tv.d.polygon([(x * K, y * K) for x, y in kalkan_noktalari(cx, cy, olcek)], fill=KREM)
    a = (cx + (47.75 - 54) * olcek, cy + (42.75 - 54) * olcek)
    b = (cx + (47.75 - 54) * olcek, cy + (67.75 - 54) * olcek)
    c = (cx + (62.75 - 54) * olcek, cy + (67.75 - 54) * olcek)
    k = 5.6 * olcek
    p1 = kis(l_ilerleme * 2)
    p2 = kis(l_ilerleme * 2 - 1)
    if p1 > 0:
        tv.cizgi(a[0], a[1], a[0], a[1] + (b[1] - a[1]) * p1, YESIL, k)
    if p2 > 0:
        tv.cizgi(b[0], b[1], b[0] + (c[0] - b[0]) * p2, b[1], YESIL, k)


def ust_bant(tv, sahne_adi, t_toplam):
    """Açılış dışındaki sahnelerde üstte ders etiketi ve ilerleme çubuğu."""
    tv.yazi(80, 120, "LİSANSLA · 1012 TAKAS", SANS_K, 26, SOLUK, capa="lm")
    tv.yazi(W - 80, 120, sahne_adi, SANS, 26, SOLUK, capa="rm")
    tv.kutu(80, 160, W - 80, 168, CIZGI, r=4)
    tv.kutu(80, 160, 80 + (W - 160) * kis(t_toplam / SURE), 168, ALTIN, r=4)


# ---------------------------------------------------------------- sahneler

def s_acilis(t, T):
    tv = Tuval(YESIL)
    o = yumusak(t / 0.9)
    rozet(tv, W / 2, 640, 3.6 * (0.6 + 0.4 * o), l_ilerleme=kis((t - 0.6) / 0.8))
    tv.yazi(W / 2, 960, "Lisansla", SERIF, 92, KREM, gir(t, 1.2), dy=40)
    g = gir(t, 1.6, 0.6)
    if g > 0:
        tv.kutu(W / 2 - 90 * g, 1035, W / 2 + 90 * g, 1041, ALTIN, r=3)
    tv.yazi(W / 2, 1100, "TAKAS, SAKLAMA VE OPERASYON", SANS_K, 30, KREM_SOLUK, gir(t, 2.0), dy=20)
    tv.sar(W / 2, 1300, "Pay Piyasası'nda\ntakas süresi", SERIF, 78, KREM, 900, gir(t, 2.6), dy=40)
    return tv


def s_soru(t, T):
    tv = Tuval(ZEMIN)
    ust_bant(tv, "Giriş", T)
    tv.sar(W / 2, 640, "Borsada bir hisse aldın.", SERIF, 70, METIN, 900, gir(t, 0.3), dy=40)
    tv.sar(W / 2, 900, "Para hesabından ne zaman çıkar, hisse ne zaman gerçekten senin olur?", SANS, 50, SOLUK, 860, gir(t, 1.5), dy=30)
    g = gir(t, 4.2, 0.7)
    if g > 0:
        tv.kutu(140, 1180, 940, 1370, YESIL, r=36, o=g)
        tv.yazi(W / 2, 1245, "Cevap", SANS_K, 30, KREM_SOLUK, g)
        tv.yazi(W / 2, 1310, "Takas gününde", SERIF, 64, KREM, g)
    return tv


GUNLER = [("Pazartesi", "T", "İşlem"), ("Salı", "T+1", ""), ("Çarşamba", "T+2", "Takas")]


def s_zaman(t, T):
    tv = Tuval(ZEMIN)
    ust_bant(tv, "T+2 kuralı", T)
    tv.yazi(W / 2, 300, "T+2 kuralı", SERIF, 84, METIN, gir(t, 0.2), dy=30)
    tv.sar(W / 2, 400, "Pay Piyasası'nda alım satımın takası", SANS, 40, SOLUK, 900, gir(t, 0.5))

    xs = [200, 540, 880]
    y = 640
    cz = yumusak(t / 1.6)
    tv.cizgi(xs[0], y, xs[0] + (xs[2] - xs[0]) * cz, y, CIZGI, 10)
    for i, (gun, etiket, alt) in enumerate(GUNLER):
        p = gir(t, 1.0 + 0.6 * i, 0.5)
        if p <= 0:
            continue
        aktif = (i == 0 and t > 2.8) or (i == 2 and t > 8.2)
        renk = ALTIN if i == 2 and t > 8.2 else (YESIL if aktif else BEYAZ)
        r = 74 * (0.5 + 0.5 * p)
        tv.daire(xs[i], y, r, renk, cizgi=YESIL if not aktif else None, kalin=4)
        tv.yazi(xs[i], y, etiket, SANS_K, 40, KREM if aktif else YESIL, p)
        tv.yazi(xs[i], y + 120, gun, SANS_K, 36, METIN, p)
        if alt:
            tv.yazi(xs[i], y + 168, alt, SANS, 32, SOLUK, gir(t, 3.0 if i == 0 else 8.4))

    # işlem günü kartı
    g = gir(t, 3.0)
    if g > 0 and t < 8.6:
        o = g * (1 - gir(t, 7.8, 0.6))
        tv.kutu(90, 880, 990, 1040, BEYAZ, r=28, cizgi=CIZGI, o=o)
        tv.sar(W / 2, 960, "Pazartesi emrin gerçekleşti. Ama para ve hisse henüz el değiştirmedi.", SANS, 38, METIN, 820, o)

    # ilerleyen işaret
    m = yumusak((t - 5.0) / 3.0)
    if 5.0 < t:
        mx = xs[0] + (xs[2] - xs[0]) * m
        tv.daire(mx, y - 120, 22, ALTIN)
        tv.ok(mx, y - 92, mx, y - 82, ALTIN, 6)

    # takas şeması
    g = gir(t, 9.0)
    if g > 0:
        kutular = [(90, "Alıcı"), (395, "Takasbank"), (700, "Satıcı")]
        for x0, ad in kutular:
            renk = YESIL if ad == "Takasbank" else BEYAZ
            tv.kutu(x0, 1000, x0 + 290, 1150, renk, r=28, cizgi=None if ad == "Takasbank" else CIZGI, o=g)
            tv.yazi(x0 + 145, 1075, ad, SANS_K, 38, KREM if ad == "Takasbank" else METIN, g)
        p1 = yumusak((t - 10.0) / 1.6)
        p2 = yumusak((t - 11.4) / 1.6)
        tv.yazi(W / 2, 1220, "Para", SANS_K, 34, YESIL, gir(t, 10.0))
        tv.ok(235, 1270, 235 + (845 - 235) * p1, 1270, YESIL, 8, ilerleme=1 if p1 > 0 else 0)
        tv.yazi(W / 2, 1350, "Hisse", SANS_K, 34, ALTIN, gir(t, 11.4))
        tv.ok(845, 1400, 845 - (845 - 235) * p2, 1400, ALTIN, 8, ilerleme=1 if p2 > 0 else 0)

    tv.sar(W / 2, 1620, "Takas, işlem gününü izleyen ikinci iş günü yapılır.", SERIF, 54, METIN, 880, gir(t, 13.6), dy=30)
    return tv


HAFTA = [("Cuma", "T · işlem", "is"), ("Cumartesi", "Sayılmaz", "tatil"), ("Pazar", "Sayılmaz", "tatil"),
         ("Pazartesi", "T+1", "is"), ("Salı", "T+2 · takas", "takas")]


def s_hafta(t, T):
    tv = Tuval(ZEMIN)
    ust_bant(tv, "İş günleri", T)
    tv.sar(W / 2, 320, "Sadece iş günleri sayılır", SERIF, 72, METIN, 900, gir(t, 0.2), dy=30)
    tv.sar(W / 2, 440, "Hafta sonu ve piyasaların kapalı olduğu tatiller hesaba katılmaz.", SANS, 38, SOLUK, 860, gir(t, 0.6))
    for i, (gun, etiket, tur) in enumerate(HAFTA):
        p = gir(t, 1.2 + 0.9 * i, 0.5)
        if p <= 0:
            continue
        y0 = 580 + i * 170
        if tur == "takas":
            zemin, yazi, ek = ALTIN, YESIL_KOYU, YESIL_KOYU
        elif tur == "tatil":
            zemin, yazi, ek = CIZGI, SOLUK, SOLUK
        else:
            zemin, yazi, ek = BEYAZ, METIN, YESIL
        tv.kutu(120, y0, 960, y0 + 140, zemin, r=28, cizgi=None if tur != "is" else CIZGI, o=p)
        tv.yazi(170, y0 + 70, gun, SANS_K, 42, yazi, p, capa="lm")
        tv.yazi(910, y0 + 70, etiket, SANS_K, 36, ek, p, capa="rm")
        if tur == "tatil" and p > 0.6:
            tv.cizgi(170, y0 + 72, 170 + font(SANS_K, 42).getlength(gun) / K, y0 + 72, SOLUK, 4)
    tv.sar(W / 2, 1520, "Cuma günü aldığın hissenin takası Salı günü yapılır.", SERIF, 52, METIN, 880, gir(t, 6.4), dy=30)
    return tv


def s_yarim(t, T):
    tv = Tuval(ZEMIN)
    ust_bant(tv, "Yarım gün", T)
    tv.sar(W / 2, 300, "Yarım günde takas yapılmaz", SERIF, 70, METIN, 900, gir(t, 0.2), dy=30)
    tv.sar(W / 2, 500, "Arife gibi yarım günlerde piyasa kısa açıktır ve o gün takas işlemi yapılmaz.", SANS, 38, SOLUK, 860, gir(t, 0.6))
    satirlar = [("Salı", "Yarım gün · işlem", KIRMIZI_ZEMIN, KIRMIZI), ("Çarşamba", "1. iş günü", BEYAZ, METIN), ("Perşembe", "2. iş günü · takas", ALTIN, YESIL_KOYU)]
    for i, (gun, et, z, y) in enumerate(satirlar):
        p = gir(t, 1.6 + 1.0 * i, 0.5)
        y0 = 680 + i * 190
        tv.kutu(120, y0, 960, y0 + 150, z, r=28, cizgi=CIZGI if z == BEYAZ else None, o=p)
        tv.yazi(170, y0 + 75, gun, SANS_K, 42, y, p, capa="lm")
        tv.yazi(910, y0 + 75, et, SANS_K, 34, y, p, capa="rm")
    tv.sar(W / 2, 1360, "Yarım günde yapılan işlemlerin takası, yarım günü izleyen ikinci iş günü yapılır.", SERIF, 50, METIN, 880, gir(t, 5.0), dy=30)
    return tv


OZET = ["Takas: işlemden sonraki 2. iş günü (T+2)", "Hafta sonu ve tatiller sayılmaz", "Yarım günde takas yapılmaz", "Para ve hisse Takasbank üzerinden el değiştirir"]


def s_ozet(t, T):
    tv = Tuval(YESIL)
    tv.yazi(W / 2, 380, "AKILDA KALSIN", SANS_K, 34, ALTIN, gir(t, 0.2))
    tv.yazi(W / 2, 520, "T+2", SERIF, 150, KREM, gir(t, 0.5), dy=40)
    for i, m in enumerate(OZET):
        p = gir(t, 1.6 + 1.1 * i, 0.5)
        y0 = 760 + i * 200
        tv.kutu(100, y0, 980, y0 + 160, YESIL_KOYU, r=28, o=p)
        tv.daire(170, y0 + 80, 26 * p, ALTIN)
        tv.sar(220, y0 + 80, m, SANS_K, 38, KREM, 700, p, capa="lm")
    return tv


SECENEK = [("A", "Cuma"), ("B", "Pazartesi"), ("C", "Salı")]


def s_test(t, T):
    tv = Tuval(ZEMIN)
    ust_bant(tv, "Sıra sende", T)
    tv.yazi(W / 2, 320, "SIRA SENDE", SANS_K, 34, ALTIN, gir(t, 0.2))
    tv.sar(W / 2, 470, "Perşembe günü aldığın hissenin takası hangi gün yapılır?", SERIF, 62, METIN, 880, gir(t, 0.4), dy=30)
    acik = t > 5.2
    for i, (h, ad) in enumerate(SECENEK):
        p = gir(t, 1.2 + 0.4 * i, 0.4)
        y0 = 700 + i * 180
        dogru = i == 1
        z = ALTIN if acik and dogru else BEYAZ
        tv.kutu(120, y0, 960, y0 + 140, z, r=28, cizgi=None if acik and dogru else CIZGI, o=p)
        tv.daire(200, y0 + 70, 38, YESIL if acik and dogru else ZEMIN, cizgi=None if acik and dogru else CIZGI)
        tv.yazi(200, y0 + 70, h, SANS_K, 36, KREM if acik and dogru else YESIL, p)
        tv.yazi(270, y0 + 70, ad, SANS_K, 42, YESIL_KOYU if acik and dogru else METIN, p if not acik or dogru else p * 0.5, capa="lm")
        if acik and dogru:
            tv.yazi(910, y0 + 70, "Doğru", SANS_K, 34, YESIL_KOYU, gir(t, 5.2), capa="rm")
    # geri sayım
    if 2.2 < t < 5.2:
        k = (t - 2.2) / 3.0
        tv.kutu(120, 1270, 960, 1282, CIZGI, r=6)
        tv.kutu(120, 1270, 120 + 840 * (1 - k), 1282, YESIL, r=6)
        tv.yazi(W / 2, 1340, str(3 - int(k * 3)), SANS_K, 44, SOLUK)
    tv.sar(W / 2, 1400, "Perşembe işlem (T), Cuma T+1, hafta sonu sayılmaz, Pazartesi T+2.", SANS, 40, SOLUK, 860, gir(t, 5.6))
    return tv


def s_kapanis(t, T):
    tv = Tuval(YESIL)
    rozet(tv, W / 2, 760, 3.0 * (0.7 + 0.3 * gir(t, 0, 0.7)), 1.0)
    tv.yazi(W / 2, 1040, "Lisansla", SERIF, 88, KREM, gir(t, 0.4), dy=30)
    tv.sar(W / 2, 1180, "Bu konunun sorularını uygulamada çöz.", SANS, 42, KREM_SOLUK, 800, gir(t, 0.9))
    tv.yazi(W / 2, 1720, "Bağımsız bir çalışma aracıdır; SPL veya SPK ile bağlantılı değildir.", SANS, 24, KREM_SOLUK, gir(t, 1.2))
    return tv


SAHNELER = [(s_acilis, 5.0), (s_soru, 7.0), (s_zaman, 18.0), (s_hafta, 12.0), (s_yarim, 10.0), (s_ozet, 9.0), (s_test, 10.0), (s_kapanis, 4.0)]
SURE = sum(s for _, s in SAHNELER)
GECIS = 0.35


def kare(T):
    bas = 0.0
    for i, (f, s) in enumerate(SAHNELER):
        if T < bas + s or i == len(SAHNELER) - 1:
            a = f(T - bas, T).bitir()
            # sahne sonu: bir sonraki sahneyle kısa karışım
            kalan = bas + s - T
            if kalan < GECIS and i + 1 < len(SAHNELER):
                b = SAHNELER[i + 1][0](0.0, T).bitir()
                a = Image.blend(a, b, 1 - kalan / GECIS)
            return a
        bas += s


def main():
    if "--kare" in sys.argv:
        T = float(sys.argv[sys.argv.index("--kare") + 1])
        kare(T).save(f"kare_{T:05.1f}.png")
        return
    cikti = sys.argv[1] if len(sys.argv) > 1 else "takas_t2.mp4"
    ff = subprocess.Popen(
        ["ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
         "-c:v", "libx264", "-preset", "medium", "-crf", "20", "-pix_fmt", "yuv420p", "-movflags", "+faststart", cikti],
        stdin=subprocess.PIPE,
    )
    n = int(SURE * FPS)
    for i in range(n):
        ff.stdin.write(kare(i / FPS).tobytes())
        if i % 150 == 0:
            print(f"{i}/{n}", flush=True)
    ff.stdin.close()
    ff.wait()
    print("bitti", cikti, SURE, "sn")


if __name__ == "__main__":
    main()
