import { readFileSync } from 'node:fs';

const paths = process.argv.slice(2);
if (paths.length === 0) {
    throw new Error('Usage: node summarize-transfer.mjs <hilog-file> [<hilog-file> ...]');
}

function percentile(values, fraction) {
    const sorted = [...values].sort((left, right) => left - right);
    return sorted[Math.floor((sorted.length - 1) * fraction)];
}

function timings(rows) {
    const result = { samples: rows.length };
    for (const key of [
        'oldReadUs', 'directReadUs', 'oldRenderUs', 'directRenderUs',
        'oldImageUs', 'directImageUs', 'oldTotalUs', 'directTotalUs',
    ]) {
        const values = rows.map(row => row[key]);
        if (values.some(value => !Number.isFinite(value))) {
            throw new Error(`Missing timing field: ${key}`);
        }
        result[key.replace(/Us$/, 'Ms')] = {
            p50: percentile(values, 0.5) / 1000,
            p95: percentile(values, 0.95) / 1000,
            max: Math.max(...values) / 1000,
        };
    }
    return result;
}

const reports = paths.map(path => {
    const lines = readFileSync(path, 'utf8').split(/\r?\n/);
    const resultLines = lines.filter(line => /GlassMaterialProbe (?:RESULT|MATERIAL_RESULT|GPU_RESULT|TRANSFER_ALPHA_RESULT|TRANSFER_RESULT) /.test(line));
    if (resultLines.length !== 5 || resultLines.some(line => !line.includes('passed=true'))) {
        throw new Error(`Incomplete or failed gates: ${path}`);
    }
    const rows = lines.filter(line => line.includes('GlassMaterialProbe TRANSFER index='))
        .map(line => Object.fromEntries([...line.matchAll(/(\w+)=([^\s]+)/g)].map(([, key, value]) => [
            key, Number.isFinite(Number(value)) ? Number(value) : value,
        ])));
    if (rows.length !== 24 || rows.some((row, index) => row.index !== index ||
        row.sourceExact !== 'true' || row.outputExact !== 'true')) {
        throw new Error(`Incomplete or non-exact transfer frames: ${path}`);
    }
    if (!lines.some(line => line.includes('GlassMaterialProbe GPU_CLEANUP result=0')) ||
        !lines.some(line => line.includes('GPU_CLOSED') && line.includes('eglStateVerified=true'))) {
        throw new Error(`Missing GPU cleanup or EGL verification: ${path}`);
    }
    const warm = rows.slice(2);
    return {
        path,
        gatesPassed: true,
        discardedWarmupFrames: 2,
        all: timings(warm),
        normal: timings(warm.filter(row => row.mode === 1)),
        deep: timings(warm.filter(row => row.mode === 2)),
        oldFirst: timings(warm.filter(row => row.index % 4 < 2)),
        directFirst: timings(warm.filter(row => row.index % 4 >= 2)),
        limits: 'Synchronous pixel/material pipeline only; excludes layer export, validation, display upload and frame presentation.',
    };
});

process.stdout.write(`${JSON.stringify(reports, null, 2)}\n`);
