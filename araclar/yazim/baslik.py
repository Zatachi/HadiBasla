import re,sys,json
BAGLAC={'ve','ile','veya','ya','da','de','ki','mi','mı','mu','mü','vs'}
SORU_EKI=re.compile(r'^m[ıiuü](s[ıiuü]n|s[ıiuü]n[ıiuü]z|y[ıiuü]m|y[ıiuü]z|d[ıiuü]r|yd[ıiuü])?$')
KORU={'sn','dk','XP','vs','·','→','+','–','-','/','&'}
def ust(c): return {'i':'İ','ı':'I'}.get(c,c.upper())
def baslik(t):
    if '\\' in t:
        ps=re.split(r'(\\.)',t)
        return ''.join(p if p.startswith('\\') else _baslik(p, i==0) for i,p in enumerate(ps))
    return _baslik(t)
def _baslik(t, ilk_bas=True):
    # ${...} ve $isim kısımlarını koru
    parcalar=re.split(r'(\$\{[^}]*\}|\$[A-Za-z_][A-Za-z_0-9]*)',t)
    sonuc=[];ilk=ilk_bas
    for p in parcalar:
        if p.startswith('$'): sonuc.append(p); ilk=False; continue
        kel=re.split(r'(\s+)',p)
        for i,w in enumerate(kel):
            if not w or w.isspace(): continue
            if w in ('·','→','|'): ilk=True; continue
            m=re.search(r'[^\W\d_]',w)
            if not m or w in KORU: ilk=False; continue
            cekirdek=re.sub(r'[^\w]','',w.lower())
            if '://' in w or '@' in w: ilk=False; continue
            if (cekirdek in BAGLAC or SORU_EKI.match(cekirdek)) and not ilk: ilk=False; continue
            j=m.start()
            if re.search(r"[0-9A-Za-zÇĞİÖŞÜçğıöşü'\\]", w[:j]) : ilk=False; continue
            if w[j].islower(): kel[i]=w[:j]+ust(w[j])+w[j+1:]
            ilk=False
        sonuc.append(''.join(kel))
    return ''.join(sonuc)
if __name__=='__main__':
    for s in sys.argv[1:]: print(baslik(s))
