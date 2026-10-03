#!/bin/zsh
set -e

ROOT_DIR="${0:A:h:h}"
SOURCE_VIDEO="/Users/apple/Downloads/WhatsApp Video 2026-09-07 at 18.26.20.mp4"
ASSET_DIR="$ROOT_DIR/marketing/reel_assets"
WORK_DIR="/private/tmp/solvex_reel_build"
OUTPUT_VIDEO="$ROOT_DIR/marketing/solvex_social_ad_15s.mp4"

mkdir -p "$WORK_DIR"

make_clip() {
  local start_time="$1"
  local duration="$2"
  local overlay_file="$3"
  local output_file="$4"
  ffmpeg -y -ss "$start_time" -t "$duration" -i "$SOURCE_VIDEO" -i "$overlay_file" \
    -filter_complex "[0:v]fps=30,split=2[front][back];[back]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,gblur=sigma=45[bg];[front]scale=900:1920:force_original_aspect_ratio=decrease[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2[scene];[scene][1:v]overlay=0:0,fade=t=in:st=0:d=0.18,fade=t=out:st=2.82:d=0.18,format=yuv420p[v]" \
    -map "[v]" -an -c:v libx264 -preset fast -crf 19 "$output_file"
}

make_clip 25 3 "$ASSET_DIR/hook.png" "$WORK_DIR/01.mp4"
make_clip 38 3 "$ASSET_DIR/tap.png" "$WORK_DIR/02.mp4"
make_clip 53 3 "$ASSET_DIR/rules.png" "$WORK_DIR/03.mp4"
make_clip 68 3 "$ASSET_DIR/win.png" "$WORK_DIR/04.mp4"

ffmpeg -y -loop 1 -t 3 -i "$ASSET_DIR/end_card.jpg" \
  -vf "scale=1080:1920,fade=t=in:st=0:d=0.25,fade=t=out:st=2.75:d=0.25,format=yuv420p" \
  -an -r 30 -c:v libx264 -preset fast -crf 19 "$WORK_DIR/05.mp4"

printf "file '%s'\n" "$WORK_DIR/01.mp4" "$WORK_DIR/02.mp4" "$WORK_DIR/03.mp4" "$WORK_DIR/04.mp4" "$WORK_DIR/05.mp4" > "$WORK_DIR/list.txt"
ffmpeg -y -f concat -safe 0 -i "$WORK_DIR/list.txt" -c copy "$WORK_DIR/video.mp4"

# Original, royalty-free electronic pulse generated locally for this ad.
ffmpeg -y \
  -f lavfi -i "sine=frequency=110:duration=15:sample_rate=48000" \
  -f lavfi -i "sine=frequency=440:duration=15:sample_rate=48000" \
  -f lavfi -i "sine=frequency=660:duration=15:sample_rate=48000" \
  -filter_complex "[0:a]tremolo=f=4:d=0.85,volume=0.34[bass];[1:a]tremolo=f=8:d=0.92,volume=0.075[mid];[2:a]tremolo=f=2:d=0.96,volume=0.035[high];[bass][mid][high]amix=inputs=3:normalize=0,afade=t=in:st=0:d=0.3,afade=t=out:st=14.2:d=0.8[music]" \
  -map "[music]" -c:a aac -b:a 192k "$WORK_DIR/music.m4a"

ffmpeg -y -i "$WORK_DIR/video.mp4" -i "$WORK_DIR/music.m4a" \
  -map 0:v -map 1:a -c:v copy -c:a aac -b:a 192k -shortest -movflags +faststart "$OUTPUT_VIDEO"

echo "$OUTPUT_VIDEO"
