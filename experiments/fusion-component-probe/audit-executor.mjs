import { execFileSync } from 'node:child_process';
import { existsSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';

const [executor, hostDirectory] = process.argv.slice(2).map(path => resolve(path));
if (!executor || !hostDirectory) throw new Error('Usage: node audit-executor.mjs executor.so staged-host-lib-directory');
const nativeRoot = '/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native';
const readelf = join(nativeRoot, 'llvm/bin/llvm-readelf');
const systemDirectory = join(nativeRoot, 'sysroot/usr/lib/aarch64-linux-ohos');
const cache = new Map();

function inspect(path) {
  if (cache.has(path)) return cache.get(path);
  const dynamic = execFileSync(readelf, ['-d', path], { encoding: 'utf8' });
  const table = execFileSync(readelf, ['--dyn-syms', path], { encoding: 'utf8' });
  const needed = [...dynamic.matchAll(/\(NEEDED\).*?\[([^\]]+)\]/g)].map(match => match[1]);
  const defined = new Set();
  const undefinedStrong = new Set();
  for (const line of table.split('\n')) {
    const match = line.match(/^\s*\d+:\s+\S+\s+\d+\s+\S+\s+(GLOBAL|WEAK)\s+DEFAULT\s+(\S+)\s+(\S+)/);
    if (!match) continue;
    const [, binding, index, name] = match;
    if (index !== 'UND') defined.add(name);
    else if (binding === 'GLOBAL') undefinedStrong.add(name);
  }
  const result = { path, needed, defined, undefinedStrong };
  cache.set(path, result);
  return result;
}

const component = inspect(executor);
const provided = new Set(component.defined);
const paths = new Set();
const queue = [executor];
while (queue.length) {
  const path = queue.shift();
  if (paths.has(path)) continue;
  paths.add(path);
  const library = inspect(path);
  for (const name of library.defined) provided.add(name);
  for (const name of library.needed) {
    const candidate = [join(dirname(path), name), join(hostDirectory, name), join(systemDirectory, name)].find(existsSync);
    if (candidate) queue.push(candidate);
  }
}
const missing = [...component.undefinedStrong].filter(name => !provided.has(name));
const extraProviders = [];
if (missing.length) {
  for (const name of readdirSync(systemDirectory).filter(name => name.endsWith('.so'))) {
    const path = join(systemDirectory, name);
    if (paths.has(path)) continue;
    const library = inspect(path);
    const matches = missing.filter(symbol => library.defined.has(symbol));
    if (matches.length) extraProviders.push({ library: name, symbols: matches });
  }
}
const exports = [...component.defined].sort();
const expected = ['glass_component_bounds', 'glass_component_close', 'glass_component_init',
  'glass_component_input', 'glass_component_render'];
const exportPass = JSON.stringify(exports) === JSON.stringify(expected);
console.log(JSON.stringify({
  passed: exportPass && missing.length === 0,
  scope: 'executor DT_NEEDED closure only; not device capability proof',
  exports, exportPass, libraries: paths.size, missing, extraProviders,
  hostBridgeImports: [...component.undefinedStrong].filter(name => name.startsWith('skikobridge_napi_') ||
    name === 'androidx_compose_ui_arkui_ArkUIViewController_onFrame'),
}, null, 2));
if (!exportPass || missing.length) process.exitCode = 1;
