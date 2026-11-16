#!/usr/bin/env bash
# Claude Code durum satırı: bağlam (token) kullanım göstergesi.
input=$(cat)
model=$(echo "$input" | jq -r '.model.display_name // "Claude"')
size=$(echo "$input" | jq -r '.context_window.context_window_size // 200000')
used=$(echo "$input" | jq -r '
  (.context_window.current_usage // {}) as $u
  | (($u.input_tokens // 0) + ($u.cache_creation_input_tokens // 0) + ($u.cache_read_input_tokens // 0))')
pct=$(echo "$input" | jq -r '.context_window.used_percentage // empty')
[ -z "$pct" ] && pct=$(( size > 0 ? used * 100 / size : 0 ))
pct=${pct%.*}
cost=$(echo "$input" | jq -r '.cost.total_cost_usd // empty')

# 10 hücreli çubuk
filled=$(( pct / 10 )); [ $filled -gt 10 ] && filled=10
bar=""; for i in $(seq 1 10); do [ $i -le $filled ] && bar+="█" || bar+="░"; done

# Renk: <50 yeşil, <80 sarı, üstü kırmızı
if [ $pct -lt 50 ]; then c=32; elif [ $pct -lt 80 ]; then c=33; else c=31; fi

k() { awk -v n="$1" 'BEGIN{ if(n>=1000) printf "%.1fk", n/1000; else printf "%d", n }'; }
line=$(printf "%s │ \033[%sm%s %d%%\033[0m │ %s / %s token" "$model" "$c" "$bar" "$pct" "$(k "$used")" "$(k "$size")")
[ -n "$cost" ] && line+=$(printf " │ \$%.2f" "$cost")
printf "%b" "$line"
