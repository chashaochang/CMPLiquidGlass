#!/bin/sh
set -eu

if [ "$#" -ne 3 ]; then
    printf 'Usage: sh run-device.sh probe-signed.hap normal-skia-signed.hap evidence-prefix\n       sh run-device.sh --restore normal-skia-signed.hap evidence-prefix\n' >&2
    exit 2
fi
PROBE_HAP=$1
NORMAL_HAP=$2
EVIDENCE=$3
RESTORE_AT_EXIT=${GLASS_PROBE_RESTORE_AT_EXIT:-false}
RESTORE_ONLY=false
if [ "$PROBE_HAP" = --restore ]; then
    RESTORE_ONLY=true
    RESTORE_AT_EXIT=true
    PROBE_HAP=$NORMAL_HAP
fi
HDC=${HDC:-/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/hdc}
BUNDLE=com.kyant.backdrop.catalog
LOGGER_PID=
REPLACED=false
test -f "$PROBE_HAP"
test -f "$NORMAL_HAP"

stop_logger() {
    if [ -n "$LOGGER_PID" ]; then
        kill "$LOGGER_PID" 2>/dev/null || true
        wait "$LOGGER_PID" 2>/dev/null || true
        LOGGER_PID=
    fi
}

capture_ui() {
    prefix=$1
    remote_prefix=$2
    "$HDC" shell uitest dumpLayout -b "$BUNDLE" -p "/data/local/tmp/$remote_prefix.json"
    "$HDC" file recv "/data/local/tmp/$remote_prefix.json" "$prefix-ui.json"
    "$HDC" shell uitest screenCap -p "/data/local/tmp/$remote_prefix.jpeg"
    "$HDC" file recv "/data/local/tmp/$remote_prefix.jpeg" "$prefix.jpeg"
}

restore() {
    result=$?
    trap - EXIT INT TERM
    stop_logger
    if [ "$REPLACED" = true ] && [ "$RESTORE_AT_EXIT" != true ]; then
        printf '\nProbe retained for this test batch; normal package has NOT been restored.\n'
        printf 'End batch: sh %s --restore %s %s\n' "$0" "$NORMAL_HAP" "$EVIDENCE"
        exit "$result"
    fi
    if [ "$REPLACED" = true ]; then
        printf '\nRestoring normal Skia package\n'
        "$HDC" shell aa force-stop "$BUNDLE" || true
        "$HDC" install -r "$NORMAL_HAP" > "$EVIDENCE-restore-install.log" 2>&1
        cat "$EVIDENCE-restore-install.log"
        if ! rg -q 'successfully' "$EVIDENCE-restore-install.log"; then
            printf 'RESTORE_FAILED: normal HAP installation not confirmed\n' >&2
            exit 3
        fi
        "$HDC" shell aa start -b "$BUNDLE" -a EntryAbility > "$EVIDENCE-restore-start.log" 2>&1
        cat "$EVIDENCE-restore-start.log"
        sleep 3
        restored_pid=$("$HDC" shell pidof "$BUNDLE" | tr -d '\r\n')
        if [ -z "$restored_pid" ]; then
            printf 'RESTORED_PACKAGE_BUT_START_FAILED\n' >&2
            exit 3
        fi
        "$HDC" shell hilog -x -P "$restored_pid" > "$EVIDENCE-restore.log" 2>&1
        capture_ui "$EVIDENCE-restored" glass-component-restored
        printf 'RESTORED_PID=%s\n' "$restored_pid"
    fi
    exit "$result"
}

# Keep incremental attempts installed; restore once when the batch ends or is interrupted.
trap restore EXIT
trap 'RESTORE_AT_EXIT=true; exit 130' INT
trap 'RESTORE_AT_EXIT=true; exit 143' TERM
REPLACED=true
if [ "$RESTORE_ONLY" = true ]; then
    exit 0
fi
"$HDC" install -r "$PROBE_HAP" > "$EVIDENCE-install.log" 2>&1
cat "$EVIDENCE-install.log"
rg -q 'successfully' "$EVIDENCE-install.log"
"$HDC" shell aa start -b "$BUNDLE" -a EntryAbility > "$EVIDENCE-start.log" 2>&1
cat "$EVIDENCE-start.log"
rg -q 'start ability successfully' "$EVIDENCE-start.log"
sleep 1
pid=$("$HDC" shell pidof "$BUNDLE" | tr -d '\r\n')
test -n "$pid"
printf 'PROBE_PID=%s\n' "$pid"
"$HDC" shell hilog -P "$pid" > "$EVIDENCE.log" 2>&1 &
LOGGER_PID=$!
count=0
while [ "$count" -lt 240 ]; do
    if rg -q 'GlassComponentProbe (RESULT|ERROR)' "$EVIDENCE.log"; then
        break
    fi
    current_pid=$("$HDC" shell pidof "$BUNDLE" | tr -d '\r\n')
    if [ "$current_pid" != "$pid" ]; then
        printf 'PROBE_PROCESS_LOST expected=%s current=%s\n' "$pid" "$current_pid" >&2
        break
    fi
    sleep 2
    count=$((count + 1))
done
sleep 2
stop_logger
rg 'GlassComponentProbe (PIXEL_RESULT|INPUT_RESULT|LIVE_RESULT|RESULT|ERROR|CLEANUP)|GlassComponentExecutor.*ERROR|GPU_CLOSED' "$EVIDENCE.log" || true
capture_ui "$EVIDENCE" glass-component-probe
if ! rg -q 'GlassComponentProbe RESULT passed=true' "$EVIDENCE.log"; then
    exit 1
fi
