#!/usr/bin/env bash
# Oturum açılışı: graphify yoksa kur, kod haritasını arka planda çıkar (yapay zekâ kullanmaz, ücretsiz).
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
command -v graphify >/dev/null 2>&1 || pip install -q 'graphifyy[sql]==0.9.115' >/dev/null 2>&1 || exit 0
nohup graphify update . >/dev/null 2>&1 &
exit 0
