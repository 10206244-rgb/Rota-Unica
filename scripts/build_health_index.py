#!/usr/bin/env python3
"""Create compact, public-place-only national health index from the official CNES snapshot."""
import sys, sqlite3, zipfile, unicodedata, re, os, math, json
from pathlib import Path
import ijson

def norm(s):
    return " ".join(re.findall(r"[a-z0-9]+", unicodedata.normalize("NFKD",str(s or "")).encode("ascii","ignore").decode().lower()))
def first(r,*keys):
    for k in keys:
        v=r.get(k)
        if v is not None and str(v).strip(): return str(v).strip()
    return ""
def rows(z):
    for name in z.namelist():
        if not name.lower().endswith(".json"): continue
        for prefix in ("estabelecimentos.item","item"):
            found=False
            with z.open(name) as f:
                for r in ijson.items(f,prefix):
                    if not isinstance(r,dict): continue
                    found=True
                    yield r
            if found: break
def build(src,dst,municipios):
    target=Path(dst);target.parent.mkdir(parents=True,exist_ok=True)
    tmp=target.with_suffix(".tmp");tmp.unlink(missing_ok=True)
    cities={}
    for m in json.load(open(municipios,encoding="utf-8")):
        try:
            k=str(m["id"])[:6]
            # New and legacy IBGE schemas both occur.
            uf=(m.get("regiao-imediata") or {}).get("regiao-intermediaria",{}).get("UF",{})
            if not uf: uf=(m.get("microrregiao") or {}).get("mesorregiao",{}).get("UF",{})
            cities[k]=m["nome"]+" "+uf.get("sigla","")
        except (KeyError,TypeError,AttributeError): pass
    db=sqlite3.connect(tmp)
    db.executescript("PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF; CREATE TABLE places(id TEXT UNIQUE,name TEXT,detail TEXT,category TEXT,lat REAL,lon REAL,cell INTEGER); CREATE VIRTUAL TABLE names USING fts4(name,detail,tokenize=unicode61);")
    seen=set();batch=[];total=0
    def flush():
        nonlocal total
        for id,name,detail,category,lat,lon,cell in batch:
            cur=db.execute("INSERT OR IGNORE INTO places VALUES(?,?,?,?,?,?,?)",(id,name,detail,category,lat,lon,cell))
            if cur.rowcount:
                db.execute("INSERT INTO names(docid,name,detail) VALUES(?,?,?)",(cur.lastrowid,norm(name),norm(detail)));total+=1
        batch.clear()
    with zipfile.ZipFile(src) as z:
        for r in rows(z):
            id=first(r,"codigo_cnes","CO_CNES","cnes")
            if not id or id in seen: continue
            reason=first(r,"codigo_motivo_desabilitacao_estabelecimento","CO_MOTIVO_DESAB")
            if reason and reason not in ("0","00","000"): continue
            name=first(r,"nome_fantasia","NO_FANTASIA","nome","nome_razao_social","NO_RAZAO_SOCIAL")
            if not name: continue
            try:
                lat=float(first(r,"latitude","latitude_estabelecimento_decimo_grau","NU_LATITUDE").replace(",","."))
                lon=float(first(r,"longitude","longitude_estabelecimento_decimo_grau","NU_LONGITUDE").replace(",","."))
            except ValueError: continue
            if not (-35<=lat<=6 and -75<=lon<=-32): continue
            seen.add(id)
            city=cities.get(first(r,"codigo_municipio","CO_IBGE","CO_MUNICIPIO","codigo_municipio_ibge")[:6],"")
            addr=" ".join(filter(None,[first(r,"endereco_estabelecimento","NO_LOGRADOURO"),first(r,"numero_estabelecimento","NU_ENDERECO"),first(r,"bairro_estabelecimento","NO_BAIRRO")]))
            detail=" · ".join(filter(None,[addr,city,"CNES / Ministério da Saúde"]))
            n=norm(name)
            category="hospital" if re.search(r"\bhospital\b",n) else "health"
            cell=math.floor((lat+90)*4)*1440+math.floor((lon+180)*4)
            batch.append((id,name,detail,category,lat,lon,cell))
            if len(batch)>=5000: flush()
    flush()
    if total<10000: raise RuntimeError(f"Unexpectedly small CNES index: {total}")
    db.execute("CREATE INDEX location ON places(cell)");db.commit();db.close();os.replace(tmp,target)
    print("CNES georeferenced public establishment records:",total)
if __name__=="__main__": build(*sys.argv[1:4])
