#!/bin/zsh
set -e

ROOT_DIR="${0:A:h:h}"
SOURCE="/Users/apple/Downloads/WhatsApp Video 2026-09-07 at 18.26.20.mp4"
ASSETS="$ROOT_DIR/marketing/gameplay_ad_assets"
WORK="/private/tmp/solvex_gameplay_ad"
OUTPUT="$ROOT_DIR/marketing/solvex_gameplay_ad_fr_18s.mp4"
mkdir -p "$WORK"

make_clip() {
  local start="$1"
  local overlay="$2"
  local out="$3"
  ffmpeg -y -ss "$start" -t 3 -i "$SOURCE" -i "$overlay" \
    -filter_complex "[0:v]fps=30,split=2[front][back];[back]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,gblur=sigma=45[bg];[front]scale=900:1920:force_original_aspect_ratio=decrease[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2[scene];[scene][1:v]overlay=0:0,fade=t=in:st=0:d=0.12,fade=t=out:st=2.88:d=0.12,format=yuv420p[v]" \
    -map "[v]" -an -c:v libx264 -preset fast -crf 19 "$out"
}

# Only the clean Level 1 run is used; clips with broken-heart errors are excluded.
make_clip 25 "$ASSETS/01_start.png" "$WORK/01.mp4"
make_clip 33 "$ASSETS/02_click.png" "$WORK/02.mp4"
make_clip 43 "$ASSETS/03_balance.png" "$WORK/03.mp4"
make_clip 53 "$ASSETS/04_think.png" "$WORK/04.mp4"
make_clip 61 "$ASSETS/05_finish.png" "$WORK/05.mp4"
make_clip 68 "$ASSETS/06_win.png" "$WORK/06.mp4"

printf "file '%s'\n" "$WORK/01.mp4" "$WORK/02.mp4" "$WORK/03.mp4" "$WORK/04.mp4" "$WORK/05.mp4" "$WORK/06.mp4" > "$WORK/list.txt"
ffmpeg -y -f concat -safe 0 -i "$WORK/list.txt" -c copy "$WORK/picture.mp4"

ffmpeg -y \
  -f lavfi -i "sine=frequency=110:duration=18:sample_rate=48000" \
  -f lavfi -i "sine=frequency=440:duration=18:sample_rate=48000" \
  -i "$ASSETS/voice_fr.aiff" \
  -filter_complex "[0:a]tremolo=f=4:d=0.88,volume=0.15[b];[1:a]tremolo=f=8:d=0.94,volume=0.035[m];[b][m]amix=inputs=2:normalize=0,afade=t=in:st=0:d=0.2[music];[2:a]volume=1.45,highpass=f=90,lowpass=f=9000,apad[voice];[music][voice]amix=inputs=2:duration=first:weights='0.75 1':normalize=0,alimiter=limit=0.92,pan=stereo|c0=c0|c1=c0[a]" \
  -map "[a]" -t 18 -c:a aac -b:a 192k "$WORK/audio.m4a"

ffmpeg -y -i "$WORK/picture.mp4" -i "$WORK/audio.m4a" \
  -map 0:v -map 1:a -c:v copy -c:a aac -b:a 192k -t 18 -movflags +faststart "$OUTPUT"
echo "$OUTPUT"
