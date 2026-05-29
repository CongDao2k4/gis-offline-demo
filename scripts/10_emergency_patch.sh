#!/bin/bash
set -e

if [ "$#" -ne 4 ]; then
  echo "Usage:"
  echo "  sh scripts/10_emergency_patch.sh <bbox> <patch_name> <minzoom> <maxzoom>"
  echo ""
  echo "Example:"
  echo "  sh scripts/10_emergency_patch.sh \"105.80,21.00,105.90,21.10\" hotfix_01 4 14"
  exit 1
fi

BBOX="$1"
PATCH_NAME="$2"
MINZOOM="$3"
MAXZOOM="$4"

echo "[INFO] Starting emergency patch for BBOX: $BBOX"
echo "[INFO] Sending request to local Spring Boot API..."

RESPONSE=$(curl -s -X POST http://localhost:8080/api/patch \
  -H "Content-Type: application/json" \
  -d '{
    "bbox": "'"$BBOX"'",
    "patchName": "'"$PATCH_NAME"'",
    "minZoom": '"$MINZOOM"',
    "maxZoom": '"$MAXZOOM"'
  }')

echo "[INFO] API Response: $RESPONSE"

STATUS=$(echo $RESPONSE | grep -o '"status":"ok"')

if [ "$STATUS" = '"status":"ok"' ]; then
  echo "[DONE] Emergency patch completed successfully!"
  echo "You can find the extracted PBF vector tiles in patches/$PATCH_NAME/export/"
else
  echo "[ERROR] Failed to execute patch. Check application logs."
  exit 1
fi
