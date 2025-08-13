#!/usr/bin/env bash
# urplans-dbcore.sh - simple wrapper for urplans REST API
set -euo pipefail

BASE_URL="http://localhost:8080"
CMD=$(basename "$0")

usage() {
  cat <<EOF
Usage:
  $CMD -i "TITLE|DESCRIPTION|START>END|PRIORITY"
  $CMD -s <pattern_or_querystring>
  $CMD -d <id>
  $CMD -l
Notes:
  - Date: YYYY-MM-DD
  - Range: START>END  (END may be FOREVER or empty)
  - If RANGE omitted, uses today's date
  - PRIORITY defaults to NOT_URGENT_NOT_IMPORTANT
EOF
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "Install $1 (apt: sudo apt install $1)"; exit 2; }
}

if [[ $# -eq 0 ]]; then usage; exit 1; fi

# ensure basic tools
require_cmd curl
# jq optional but recommended
if ! command -v jq >/dev/null 2>&1; then
  JQ=0
else
  JQ=1
fi

while getopts ":i:s:d:lh" opt; do
  case $opt in
    i)
      IVAL="$OPTARG"
      # split into up to 4 parts by '|'
      IFS='|' read -r TITLE DESC RANGE PRIOR <<<"$IVAL"

      TITLE=${TITLE:-""}
      DESC=${DESC:-""}
      PRIOR=${PRIOR:-"NOT_URGENT_NOT_IMPORTANT"}

      # normalize RANGE
      if [[ -z "${RANGE:-}" ]]; then
        START="$(date +%F)"
        END=""
      elif [[ "$RANGE" == *'>'* ]]; then
        START="${RANGE%%>*}"
        END="${RANGE#*>}"
        # if END equals the original (no > present) then treat as single date
        [[ "$END" == "$RANGE" ]] && END=""
        [[ "$END" == "FOREVER" ]] && END=""
      else
        START="$RANGE"
        END=""
      fi

      # validate START (simple check)
      if ! [[ "$START" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
        echo "Bad start date: $START" >&2
        exit 3
      fi
      # validate END if present
      if [[ -n "$END" ]] && ! [[ "$END" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
        echo "Bad end date: $END" >&2
        exit 3
      fi

      # produce JSON; use plain string building to avoid dependency on jq
      if [[ -z "$END" ]]; then
        JSON=$(printf '{"title":"%s","description":"%s","startDate":"%s","endDate":null,"priority":"%s"}' \
          "$(printf '%s' "$TITLE" | sed 's/"/\\"/g')" \
          "$(printf '%s' "$DESC" | sed 's/"/\\"/g')" \
          "$START" \
          "$PRIOR")
      else
        JSON=$(printf '{"title":"%s","description":"%s","startDate":"%s","endDate":"%s","priority":"%s"}' \
          "$(printf '%s' "$TITLE" | sed 's/"/\\"/g')" \
          "$(printf '%s' "$DESC" | sed 's/"/\\"/g')" \
          "$START" "$END" "$PRIOR")
      fi

      RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/tasks" -H "Content-Type: application/json" -d "$JSON")
      BODY=$(echo "$RESP" | sed '$d')
      CODE=$(echo "$RESP" | tail -n1)
      if [[ "$CODE" =~ ^2 ]]; then
        if [[ $JQ -eq 1 ]]; then echo "$BODY" | jq .; else echo "$BODY"; fi
      else
        echo "Request failed HTTP $CODE" >&2
        echo "$BODY" >&2
        exit 4
      fi
      exit 0
      ;;
    s)
      SVAL="$OPTARG"
      # wildcard or querystring
      if [[ "$SVAL" == *"*"* ]]; then
        URL="${BASE_URL}/api/tasks/search?date=${SVAL}"
      elif [[ "$SVAL" == *"="* || "$SVAL" == *"&"* ]]; then
        URL="${BASE_URL}/api/tasks/search?${SVAL}"
      elif [[ "$SVAL" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
        URL="${BASE_URL}/api/tasks/search?date=${SVAL}"
      else
        URL="${BASE_URL}/api/tasks/search?title=${SVAL}"
      fi
      if [[ $JQ -eq 1 ]]; then curl -s "$URL" | jq .; else curl -s "$URL"; fi
      exit 0
      ;;
    d)
      ID="$OPTARG"
      curl -s -X DELETE "${BASE_URL}/api/tasks/${ID}" -w "\nHTTP:%{http_code}\n"
      exit 0
      ;;
    l)
      if [[ $JQ -eq 1 ]]; then curl -s "${BASE_URL}/api/tasks" | jq .; else curl -s "${BASE_URL}/api/tasks"; fi
      exit 0
      ;;
    h|\?)
      usage; exit 0
      ;;
  esac
done

usage
exit 1
