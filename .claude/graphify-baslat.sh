#!/usr/bin/env bash
# Oturum açılışı: graphify yoksa kur, kod haritasını arka planda çıkar (yapay zekâ kullanmaz, ücretsiz).
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
command -v graphify >/dev/null 2>&1 || pip install -q 'graphifyy[sql]==0.9.115' >/dev/null 2>&1 || exit 0
graphify hook status 2>/dev/null | grep -q 'post-commit: installed' || { graphify hook install >/dev/null 2>&1; grep -qx .gitattributes .git/info/exclude 2>/dev/null || echo .gitattributes >> .git/info/exclude; }  # commit sonrası haritayı güncelle
nohup graphify update . >/dev/null 2>&1 &
exit 0
