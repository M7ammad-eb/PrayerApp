// Regenerates pre-Android-8 launcher PNGs from the supplied Salati 1.svg artwork.
const fs = require("fs");
const path = require("path");
const sharp = require("sharp");

const [sourcePath, resPath] = process.argv.slice(2);
if (!sourcePath || !resPath) {
  throw new Error("Usage: node render_salati_icons.js <Salati 1.svg> <app/src/main/res>");
}

const source = fs.readFileSync(sourcePath, "utf8");
const colored = source.replace(
  "</svg>",
  `<style>
    #Layer_1 > path { fill: #165B33; }
    #Layer_1 > g:not(#Layer_3) { fill: #C59B27; }
    #Layer_3 { fill: #165B33; }
  </style></svg>`
);

const sizes = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };

async function render() {
  for (const [density, size] of Object.entries(sizes)) {
    const markSize = Math.round(size * 0.82);
    const mark = await sharp(Buffer.from(colored)).resize(markSize, markSize).png().toBuffer();
    const offset = Math.floor((size - markSize) / 2);
    const directory = path.join(resPath, `mipmap-${density}`);

    await sharp({ create: { width: size, height: size, channels: 4, background: "#FFFFFF" } })
      .composite([{ input: mark, left: offset, top: offset }])
      .png()
      .toFile(path.join(directory, "salati_launcher.png"));

    const circle = Buffer.from(
      `<svg width="${size}" height="${size}"><circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}" fill="#FFFFFF"/></svg>`
    );
    await sharp({ create: { width: size, height: size, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
      .composite([{ input: circle }, { input: mark, left: offset, top: offset }])
      .png()
      .toFile(path.join(directory, "salati_launcher_round.png"));
  }
}

render().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
