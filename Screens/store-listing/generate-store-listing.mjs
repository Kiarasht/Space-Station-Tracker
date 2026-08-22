import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";
import sharp from "sharp";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const screensDir = path.resolve(scriptDir, "..");
const renderOutputDir = "/private/tmp/iss-tracker-store-listing";
const phoneOutputDir = path.join(renderOutputDir, "phone");
const tabletOutputDir = path.join(renderOutputDir, "tablet");

export const slides = [
    {
        slug: "track-the-iss-live",
        title: "Track the ISS Live",
        description: "Follow its position, speed, altitude, and orbital path in real time.",
        phone: "Phone 1.png",
        tablet: "Tablet 1.png",
        accent: "#62D8E8",
        accentAlt: "#FFEB3B",
        horizon: "#0B5778"
    },
    {
        slug: "find-visible-passes",
        title: "Find Visible Passes",
        description: "View upcoming sky paths, set reminders, and add passes to your calendar.",
        phone: "Phone 2.png",
        tablet: "Tablet 2.png",
        accent: "#FFEB3B",
        accentAlt: "#62D8E8",
        horizon: "#17336E"
    },
    {
        slug: "meet-the-crew",
        title: "Meet the Crew",
        description: "Explore the astronauts currently living and working in orbit.",
        phone: "Phone 3.png",
        tablet: "Tablet 3.png",
        accent: "#62D8E8",
        accentAlt: "#F5F7FF",
        horizon: "#123A67"
    },
    {
        slug: "discover-their-stories",
        title: "Discover Their Stories",
        description: "Read astronaut profiles, mission roles, launch details, and more.",
        phone: "Phone 4.png",
        tablet: "Tablet 4.png",
        accent: "#FFEB3B",
        accentAlt: "#62D8E8",
        horizon: "#203568"
    },
    {
        slug: "watch-earth-from-orbit",
        title: "Watch Earth From Orbit",
        description: "Open the live ISS stream and see our planet from space.",
        phone: "Phone 6.png",
        tablet: "Tablet 6.png",
        accent: "#FF5252",
        accentAlt: "#62D8E8",
        horizon: "#123E5D"
    },
    {
        slug: "built-for-day-or-night",
        title: "Built for Day or Night",
        description: "Switch between light and dark themes whenever you track.",
        phone: ["Phone 5.png", "Phone 4.png"],
        tablet: ["Tablet 4.png", "Tablet 5.png"],
        accent: "#FFEB3B",
        accentAlt: "#62D8E8",
        horizon: "#28456B",
        splitTheme: true
    }
];

export const formats = {
    phone: {
        width: 1080,
        height: 1920,
        outputDir: phoneOutputDir
    },
    tablet: {
        width: 1920,
        height: 1080,
        outputDir: tabletOutputDir
    }
};

const fontData = {
    heading: await fileDataUri(
        path.resolve(screensDir, "../app/src/main/res/font/orbitron_variable.ttf"),
        "font/ttf"
    ),
    body: await fileDataUri(
        path.resolve(screensDir, "../app/src/main/res/font/exo_variable.ttf"),
        "font/ttf"
    )
};

const copyManifest = {
    phone: {
        size: "1080x1920",
        files: slides.map((slide, index) =>
            `${String(index + 1).padStart(2, "0")}-${slide.slug}.png`
        )
    },
    tablet: {
        size: "1920x1080",
        files: slides.map((slide, index) =>
            `${String(index + 1).padStart(2, "0")}-${slide.slug}.png`
        )
    },
    slides: slides.map(({ slug, title, description }) => ({
        slug,
        title,
        description
    }))
};

export async function prepareHtmlPages() {
    await fs.mkdir(phoneOutputDir, { recursive: true });
    await fs.mkdir(tabletOutputDir, { recursive: true });

    const pages = [];
    for (const [formatName, format] of Object.entries(formats)) {
        for (const [index, slide] of slides.entries()) {
            const filename =
                `${String(index + 1).padStart(2, "0")}-${slide.slug}`;
            const htmlPath = path.join(
                renderOutputDir,
                `${formatName}-${filename}.html`
            );
            await fs.writeFile(
                htmlPath,
                await buildSlideHtml(formatName, format, slide)
            );
            pages.push({
                formatName,
                width: format.width,
                height: format.height,
                htmlPath,
                outputPath: path.join(format.outputDir, `${filename}.png`)
            });
        }
    }
    return pages;
}

export async function finalizeAssets() {
    const manifestJson = `${JSON.stringify(copyManifest, null, 2)}\n`;
    await fs.writeFile(
        path.join(renderOutputDir, "screenshot-copy.json"),
        manifestJson
    );
    await fs.writeFile(
        path.join(phoneOutputDir, "screenshot-copy.json"),
        manifestJson
    );
    await fs.writeFile(
        path.join(tabletOutputDir, "screenshot-copy.json"),
        manifestJson
    );

    await makeContactSheet(
        phoneOutputDir,
        slides,
        path.join(phoneOutputDir, "contact-sheet-preview.png"),
        {
            columns: 3,
            thumbnailWidth: 270,
            thumbnailHeight: 480
        }
    );

    await makeContactSheet(
        tabletOutputDir,
        slides,
        path.join(tabletOutputDir, "contact-sheet-preview.png"),
        {
            columns: 3,
            thumbnailWidth: 480,
            thumbnailHeight: 270
        }
    );
}

export async function renderStoreListing() {
    const pages = await prepareHtmlPages();
    const browser = await chromium.launch({
        headless: true,
        executablePath:
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    });

    try {
        for (const pageSpec of pages) {
            const page = await browser.newPage({
                viewport: {
                    width: pageSpec.width,
                    height: pageSpec.height
                },
                deviceScaleFactor: 1
            });
            await page.goto(`file://${pageSpec.htmlPath}`, {
                waitUntil: "load"
            });
            await page.evaluate(() => document.fonts.ready);
            await page.waitForFunction(() =>
                Array.from(document.images).every((image) => image.complete)
            );

            const screenshot = await page.screenshot({ type: "png" });
            await sharp(screenshot)
                .flatten({ background: "#04071A" })
                .removeAlpha()
                .png()
                .toFile(pageSpec.outputPath);
            await page.close();
        }
    } finally {
        await browser.close();
    }

    await finalizeAssets();
}

export async function buildSlideHtml(formatName, format, slide) {
    const sourceFiles = Array.isArray(slide[formatName])
        ? slide[formatName]
        : [slide[formatName]];
    const images = await Promise.all(
        sourceFiles.map((filename) =>
            fileDataUri(path.join(screensDir, filename), "image/png")
        )
    );
    const stars = makeStars(format.width, format.height);
    const deviceMarkup = slide.splitTheme
        ? `
            <img class="device theme-device theme-device-light" src="${images[0]}" alt="">
            <img class="device theme-device theme-device-dark" src="${images[1]}" alt="">
        `
        : `<img class="device" src="${images[0]}" alt="">`;

    return `
        <!doctype html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                @font-face {
                    font-family: "OrbitronLocal";
                    src: url("${fontData.heading}") format("truetype");
                    font-style: normal;
                    font-weight: 400 900;
                }
                @font-face {
                    font-family: "ExoLocal";
                    src: url("${fontData.body}") format("truetype");
                    font-style: normal;
                    font-weight: 100 900;
                }
                * {
                    box-sizing: border-box;
                }
                html,
                body {
                    width: ${format.width}px;
                    height: ${format.height}px;
                    margin: 0;
                    overflow: hidden;
                    background: #04071A;
                }
                body {
                    position: relative;
                    color: #FFFFFF;
                    font-family: "ExoLocal", Arial, sans-serif;
                }
                .backdrop {
                    position: absolute;
                    inset: 0;
                    background:
                        radial-gradient(circle at 80% 16%, rgba(46, 86, 156, 0.35), transparent 34%),
                        linear-gradient(180deg, #050622 0%, #03051A 56%, #07132A 100%);
                }
                .stars {
                    position: absolute;
                    inset: 0;
                    opacity: 0.72;
                }
                .star {
                    position: absolute;
                    border-radius: 50%;
                    background: #FFFFFF;
                    box-shadow: 0 0 8px rgba(255, 255, 255, 0.6);
                }
                .horizon {
                    position: absolute;
                    left: 50%;
                    background: ${slide.horizon};
                    border-top: 4px solid ${slide.accent};
                    box-shadow: 0 -24px 90px color-mix(in srgb, ${slide.accent} 24%, transparent);
                }
                .horizon::after {
                    content: "";
                    position: absolute;
                    inset: 12% 4% auto;
                    height: 40%;
                    border-radius: 50%;
                    background: rgba(98, 216, 232, 0.09);
                    filter: blur(24px);
                }
                .copy {
                    position: absolute;
                    z-index: 5;
                }
                h1 {
                    margin: 0;
                    font-family: "OrbitronLocal", Arial, sans-serif;
                    font-weight: 720;
                    letter-spacing: 0;
                    text-wrap: balance;
                }
                p {
                    margin: 20px 0 0;
                    color: #DDE5F7;
                    font-weight: 480;
                    letter-spacing: 0;
                }
                .device {
                    position: absolute;
                    z-index: 4;
                    display: block;
                    object-fit: contain;
                    filter: drop-shadow(0 38px 45px rgba(0, 0, 0, 0.52));
                }
                .theme-device {
                    filter: drop-shadow(0 38px 45px rgba(0, 0, 0, 0.52));
                }
                ${formatName === "phone" ? phoneCss() : tabletCss()}
            </style>
        </head>
        <body>
            <div class="backdrop"></div>
            <div class="stars">${stars}</div>
            <div class="horizon"></div>
            <section class="copy">
                <h1>${slide.title}</h1>
                <p>${slide.description}</p>
            </section>
            ${deviceMarkup}
        </body>
        </html>
    `;
}

function phoneCss() {
    return `
        .copy {
            top: 84px;
            left: 72px;
            right: 72px;
        }
        h1 {
            max-width: 940px;
            font-size: 72px;
            line-height: 1.03;
        }
        p {
            max-width: 900px;
            font-size: 35px;
            line-height: 1.28;
        }
        .horizon {
            bottom: -360px;
            width: 1540px;
            height: 890px;
            transform: translateX(-50%);
            border-radius: 50% 50% 0 0;
        }
        .device {
            width: 850px;
            height: auto;
            left: 115px;
            bottom: -245px;
        }
        .theme-device {
            width: 650px;
            height: auto;
            bottom: -80px;
        }
        .theme-device-light {
            left: -20px;
            right: auto;
            z-index: 3;
            transform: rotate(-4deg);
        }
        .theme-device-dark {
            left: auto;
            right: -20px;
            z-index: 4;
            transform: rotate(4deg);
        }
    `;
}

function tabletCss() {
    return `
        .copy {
            top: 105px;
            left: 92px;
            width: 570px;
        }
        h1 {
            max-width: 570px;
            font-size: 70px;
            line-height: 1.02;
        }
        p {
            max-width: 540px;
            font-size: 30px;
            line-height: 1.28;
        }
        .horizon {
            bottom: -610px;
            width: 2200px;
            height: 1060px;
            transform: translateX(-28%);
            border-radius: 50% 50% 0 0;
        }
        .device {
            width: 1320px;
            height: auto;
            right: -90px;
            top: -155px;
        }
        .theme-device {
            width: 900px;
            height: auto;
            top: 110px;
        }
        .theme-device-light {
            left: 600px;
            right: auto;
            z-index: 3;
            transform: rotate(-3deg);
        }
        .theme-device-dark {
            left: auto;
            right: -20px;
            z-index: 4;
            transform: rotate(3deg);
        }
    `;
}

function makeStars(width, height) {
    return Array.from({ length: 72 }, (_, index) => {
        const x = (index * 197 + 41) % width;
        const y = (index * 263 + 79) % height;
        const size = index % 11 === 0 ? 4 : index % 4 === 0 ? 3 : 2;
        const opacity = 0.28 + ((index * 17) % 48) / 100;
        return `<span class="star" style="left:${x}px;top:${y}px;width:${size}px;height:${size}px;opacity:${opacity}"></span>`;
    }).join("");
}

async function fileDataUri(filePath, mimeType) {
    const contents = await fs.readFile(filePath);
    return `data:${mimeType};base64,${contents.toString("base64")}`;
}

async function makeContactSheet(outputDir, slideData, outputPath, options) {
    const gap = 28;
    const margin = 36;
    const labelHeight = 58;
    const rows = Math.ceil(slideData.length / options.columns);
    const width =
        margin * 2 +
        options.columns * options.thumbnailWidth +
        (options.columns - 1) * gap;
    const height =
        margin * 2 +
        rows * (options.thumbnailHeight + labelHeight) +
        (rows - 1) * gap;
    const composites = [];

    for (const [index, slide] of slideData.entries()) {
        const filePath = path.join(
            outputDir,
            `${String(index + 1).padStart(2, "0")}-${slide.slug}.png`
        );
        const left =
            margin +
            (index % options.columns) * (options.thumbnailWidth + gap);
        const top =
            margin +
            Math.floor(index / options.columns) *
                (options.thumbnailHeight + labelHeight + gap);
        const thumbnail = await sharp(filePath)
            .resize(options.thumbnailWidth, options.thumbnailHeight, {
                fit: "cover"
            })
            .png()
            .toBuffer();
        const escapedTitle = escapeXml(`${index + 1}. ${slide.title}`);
        const label = Buffer.from(`
            <svg width="${options.thumbnailWidth}" height="${labelHeight}">
                <rect width="100%" height="100%" fill="#111832"/>
                <text
                    x="${options.thumbnailWidth / 2}"
                    y="38"
                    fill="#FFFFFF"
                    font-size="22"
                    font-family="Arial, sans-serif"
                    text-anchor="middle"
                >${escapedTitle}</text>
            </svg>
        `);

        composites.push({ input: thumbnail, left, top });
        composites.push({
            input: label,
            left,
            top: top + options.thumbnailHeight
        });
    }

    await sharp({
        create: {
            width,
            height,
            channels: 3,
            background: "#EAF0F5"
        }
    })
        .composite(composites)
        .png()
        .toFile(outputPath);
}

function escapeXml(value) {
    return value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&apos;");
}
