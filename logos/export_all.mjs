import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const iconSvg = path.resolve('logos/export/icon.svg');
const logoSvg = path.resolve('logos/export/logo.svg');
const exportDir = path.resolve('logos/export');

if (!fs.existsSync(exportDir)) {
  fs.mkdirSync(exportDir, { recursive: true });
}

const iconSizes = [16, 32, 48, 64, 128, 192, 512, 1024, 2048];
const logoSizes = [512, 1024, 2048];

console.log('Exporting icon sizes...');
for (const size of iconSizes) {
  const outPath = path.join(exportDir, `icon-${size}.png`);
  execSync(`npx --yes @resvg/resvg-js-cli --fit-width ${size} "${iconSvg}" "${outPath}"`, { stdio: 'ignore' });
  console.log(`Generated: icon-${size}.png`);
}

console.log('Exporting logo sizes...');
for (const size of logoSizes) {
  const outPath = path.join(exportDir, `logo-${size}.png`);
  execSync(`npx --yes @resvg/resvg-js-cli --fit-width ${size} "${logoSvg}" "${outPath}"`, { stdio: 'ignore' });
  console.log(`Generated: logo-${size}.png`);
}

// Android mipmap targets
const mipmaps = [
  { dir: 'app/src/main/res/mipmap-mdpi', size: 48 },
  { dir: 'app/src/main/res/mipmap-hdpi', size: 72 },
  { dir: 'app/src/main/res/mipmap-xhdpi', size: 96 },
  { dir: 'app/src/main/res/mipmap-xxhdpi', size: 144 },
  { dir: 'app/src/main/res/mipmap-xxxhdpi', size: 192 },
];

console.log('Updating Android mipmap drawables...');
for (const m of mipmaps) {
  const targetDir = path.resolve(m.dir);
  if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
  }
  const icLauncher = path.join(targetDir, 'ic_launcher.png');
  const icLauncherRound = path.join(targetDir, 'ic_launcher_round.png');
  execSync(`npx --yes @resvg/resvg-js-cli --fit-width ${m.size} "${iconSvg}" "${icLauncher}"`, { stdio: 'ignore' });
  execSync(`npx --yes @resvg/resvg-js-cli --fit-width ${m.size} "${iconSvg}" "${icLauncherRound}"`, { stdio: 'ignore' });
  console.log(`Updated ${m.dir} (${m.size}px)`);
}

// Update meditech_avatar.png in app/src/main/res/drawable/
const avatarPath = path.resolve('app/src/main/res/drawable/meditech_avatar.png');
execSync(`npx --yes @resvg/resvg-js-cli --fit-width 512 "${iconSvg}" "${avatarPath}"`, { stdio: 'ignore' });
console.log('Updated app/src/main/res/drawable/meditech_avatar.png (512px)');
