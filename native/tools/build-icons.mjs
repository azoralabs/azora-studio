// Writes each icon resources/icons.azon names as a white-stroked SVG into the
// directory given, from the lucide-react package found beside this checkout.
// Usage: node build-icons.mjs <icons.azon> <lucide-react dir> <out dir>
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { pathToFileURL } from "node:url";
import { join } from "node:path";

const [listPath, lucide, out] = process.argv.slice(2);
// AZON as written here is JSON-compatible.
const list = JSON.parse(readFileSync(listPath, "utf8"));
mkdirSync(out, { recursive: true });
const attributes = (attrs) => Object.entries(attrs)
    .filter(([name]) => name !== "key")
    .map(([name, value]) => `${name.replace(/[A-Z]/g, (c) => "-" + c.toLowerCase())}="${value}"`)
    .join(" ");
for (const name of list.icons) {
    const module = await import(pathToFileURL(join(lucide, "dist/esm/icons", `${name}.mjs`)).href);
    const body = module.__iconNode.map(([tag, attrs]) => `<${tag} ${attributes(attrs)}/>`).join("");
    writeFileSync(join(out, `${name}.svg`),
        `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" ` +
        `stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${body}</svg>`);
}
console.log(`${list.icons.length} icons`);
