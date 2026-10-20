"""Ekran metinlerini kurumsal başlık düzenine göre denetler.

Kullanım:  python3 araclar/yazim/denetle.py          (sadece rapor)
           python3 araclar/yazim/denetle.py --yaz    (düzeltir)
Kural: Her Kelime Büyük; ve, ile, veya, ya da, de/da, ki, mi bağlaçları küçük.
Soru metinleri, seslendirme, KVKK metni ve paylaşım mesajları cümle düzeninde kalır.
"""
import re,sys,os
sys.path.insert(0,os.path.dirname(__file__))
from baslik import baslik
KOK=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','..','app/src/main/java/com/lisanshazirlik/app')
ATLA_DOSYA={'data/SoruBankasi.kt','data/Olcum.kt','ui/Tema.kt'}
ATLA_SATIR2=re.compile(r'collection\(|document\(|Log\.|whereEqualTo|http|\.json|optString|optBoolean|putExtra|NotificationChannel\(|speak\(|ACTION_SEND|EXTRA_TEXT|val metin = "Sermaye')
ATLA_SATIR=re.compile(r'==|!=|\.contains\(|startsWith\(|endsWith\(|equals\(|Regex\(|\.split\(|\.replace\(|collection\(|document\(|olay\(|Log\.|getString\(|whereEqualTo|Locale|http|\.json|optString|optBoolean|putExtra|Intent\(|setChannel|NotificationChannel|konus\(|speak\(|tts')
def literaller(src):
    """(bas,son) üst düzey ve iç içe string içerik aralıkları; yorumları atlar."""
    out=[];i=0;n=len(src)
    def oku_str(i):  # i: açılış " sonrası
        parca=[];bas=i
        while i<n:
            c=src[i]
            if c=='\\': i+=2; continue
            if c=='"': out.append((bas,i)); return i+1
            if c=='$' and i+1<n and src[i+1]=='{':
                j=i+2;d=1
                while j<n and d:
                    if src[j]=='"':
                        if src[j:j+3]=='"""':
                            k=src.index('"""',j+3); j=k+3; continue
                        j=oku_str(j+1); continue
                    if src[j]=='{': d+=1
                    elif src[j]=='}': d-=1
                    j+=1
                i=j; continue
            i+=1
        return i
    while i<n:
        if src.startswith('//',i): i=src.find('\n',i); i=n if i<0 else i; continue
        if src.startswith('/*',i): i=src.find('*/',i)+2; continue
        if src.startswith('"""',i): i=src.index('"""',i+3)+3; continue
        if src[i]=="'": 
            i+= 4 if src[i+1]=='\\' else 3; continue
        if src[i]=='"': i=oku_str(i+1); continue
        i+=1
    return out
def uygun(src,b,e):
    t=src[b:e]
    duz=re.sub(r'\$\{[^}]*\}|\$[A-Za-z_]\w*','',t)
    if not re.search(r'[a-zçğıöşü]',duz): return False
    on_=src[src.rfind('\n',0,b)+1:b-1]; arka=src[e+1:src.find('\n',e)]
    satir=on_+arka
    if ATLA_SATIR2.search(satir): return False
    if re.search(r'(==|!=|contains\(|startsWith\(|endsWith\(|equals\(|split\(|replace\(|Regex\(|olay\(|getString\()\s*$',on_) or re.match(r'\s*(==|!=|\)\s*==)',arka): return False
    if re.fullmatch(r'[dMyHhmsE .:/-]+',t): return False
    # Arkadaşa gönderilen paylaşım mesajları sohbet dilinde kalır.
    if any(x in t for x in ('davet ediyorum','Sermaye piyasası lisans','Kayıt olurken davet kodumu')): return False
    if re.search(r'\s',duz.strip()) : return True
    # tek kelime: sadece Text("...") ilk argümanı
    on=src[max(0,b-6):b-1]
    return on.endswith('Text(')
degisen=0;rapor=[]
yaz='--yaz' in sys.argv
for kok,_,dosyalar in os.walk(KOK):
    for f in dosyalar:
        if not f.endswith('.kt'): continue
        p=os.path.join(kok,f); rel=os.path.relpath(p,KOK)
        if rel in ATLA_DOSYA: continue
        src=open(p).read()
        araliklar=sorted(literaller(src),reverse=True)
        yeni=src
        for b,e in araliklar:
            if not uygun(src,b,e): continue
            t=src[b:e]
            if '{' in re.sub(r'\$\{','',t).replace('}',''): pass
            y=baslik(t)
            if y!=t:
                rapor.append(f'{rel}: {t!r} -> {y!r}'); degisen+=1
                yeni=yeni[:b]+y+yeni[e:]
        if yaz and yeni!=src: open(p,'w').write(yeni)
print('\n'.join(rapor)); print('TOPLAM',degisen)
