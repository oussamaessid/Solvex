#!/bin/zsh
set -e

ROOT_DIR="${0:A:h:h}"
ASSETS="$ROOT_DIR/marketing/explainer_assets"
WORK_DIR="/private/tmp/solvex_ai_explainer"
OUTPUT="$ROOT_DIR/marketing/solvex_ai_explainer_fr_25s.mp4"
mkdir -p "$WORK_DIR"

make_scene() {
  local source_image="$1"
  local seconds="$2"
  local frames=$((seconds * 30))
  local output_file="$3"
  ffmpeg -y -loop 1 -i "$source_image" -t "$seconds" \
    -vf "scale=1080:1920,zoompan=z='min(zoom+0.00055,1.045)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=${frames}:s=1080x1920:fps=30,fade=t=in:st=0:d=0.2,fade=t=out:st=$(($seconds-1)).8:d=0.2,format=yuv420p" \
    -an -c:v libx264 -preset fast -crf 19 -r 30 "$output_file"
}

make_scene "$ASSETS/00_intro.jpg" 3 "$WORK_DIR/00.mp4"
make_scene "$ASSETS/01_clicks.jpg" 4 "$WORK_DIR/01.mp4"
make_scene "$ASSETS/02_balance.jpg" 5 "$WORK_DIR/02.mp4"
make_scene "$ASSETS/03_no_three.jpg" 4 "$WORK_DIR/03.mp4"
make_scene "$ASSETS/04_symbols.jpg" 5 "$WORK_DIR/04.mp4"
make_scene "$ASSETS/05_cta.jpg" 4 "$WORK_DIR/05.mp4"

printf "file '%s'\n" "$WORK_DIR/00.mp4" "$WORK_DIR/01.mp4" "$WORK_DIR/02.mp4" "$WORK_DIR/03.mp4" "$WORK_DIR/04.mp4" "$WORK_DIR/05.mp4" > "$WORK_DIR/list.txt"
ffmpeg -y -f concat -safe 0 -i "$WORK_DIR/list.txt" -c copy "$WORK_DIR/picture.mp4"

# Locally generated original music bed, mixed below the French narration.
ffmpeg -y \
  -f lavfi -i "sine=frequency=110:duration=25:sample_rate=48000" \
  -f lavfi -i "sine=frequency=440:duration=25:sample_rate=48000" \
  -f lavfi -i "sine=frequency=659.25:duration=25:sample_rate=48000" \
  -i "$ASSETS/voice_fr.aiff" \
  -filter_complex "[0:a]tremolo=f=4:d=0.88,volume=0.16[bass];[1:a]tremolo=f=8:d=0.93,volume=0.035[mid];[2:a]tremolo=f=2:d=0.96,volume=0.018[high];[bass][mid][high]amix=inputs=3:normalize=0,afade=t=in:st=0:d=0.25[music];[3:a]volume=1.4,highpass=f=90,lowpass=f=9000,apad[voice];[music][voice]amix=inputs=2:duration=first:weights='0.8 1.0':normalize=0,alimiter=limit=0.92,pan=stereo|c0=c0|c1=c0[a]" \
  -map "[a]" -t 25 -c:a aac -b:a 192k "$WORK_DIR/final_audio.m4a"

ffmpeg -y -i "$WORK_DIR/picture.mp4" -i "$WORK_DIR/final_audio.m4a" \
  -map 0:v -map 1:a -c:v copy -c:a aac -b:a 192k -t 25 -movflags +faststart "$OUTPUT"

echo "$OUTPUT"
