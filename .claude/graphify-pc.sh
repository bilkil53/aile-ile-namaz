#!/usr/bin/env bash
# Bilgisayarda (cloud dışı) Claude Code bu depoyu açınca: graphify yoksa kur, kod haritasını
# arka planda çıkar, kullanım kuralını oturuma ekle. Cloud'da bunları Default ortam kurulum
# betiği yapar; orada bu betik hiçbir şey yapmaz.
[ -n "$CLAUDE_CODE_REMOTE" ] && exit 0
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
PY=$(command -v python3 || command -v python) || exit 0
"$PY" -c 'import graphify' 2>/dev/null || "$PY" -m pip install -q --user 'graphifyy[sql]==0.9.115' >/dev/null 2>&1 || exit 0
nohup "$PY" -m graphify update . >/dev/null 2>&1 &
cat <<'TXT'
Kod haritası (graphify) bu bilgisayarda kurulu; harita graphify-out/ içinde çıkarılıyor (depoya girmez).
- Kod sorusunda ilk grep/okumadan önce `python -m graphify query "<soru>"` (ilişki: `path "A" "B"`, tek kavram: `explain "X"`). Ham dosyadan yalnız haritanın gösterdiği satırları oku.
- Kod değişince `python -m graphify update .`. Bağ bulunamazsa (ör. Dart) grep'e dön.
TXT
exit 0
