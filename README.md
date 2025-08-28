# GameworldJEM 🕹️✨
**A tiny top‑down world renderer for Jetpack Compose** — loads maps from **Tiled** and draws them using Compose’s `Canvas`, including multi‑tileset support, a walking hero sprite, camera follow, and simple collisions. Fast to drop into your app for prototypes, demos, or retro‑style games.

> **Module coordinates**: `com.jem.gameworldjem:gameworldjem:0.1.0`

---

## 🚀 Features
- 🎯 **Jetpack Compose** canvas renderer (no OpenGL needed)
- 🗺️ **Tiled** loader: reads `.tmj` maps + `.tsj/.tsx` tilesets (JSON or XML)
- 🧱 Multiple tilesets with **firstgid** handling
- 👣 Plug‑in **hero sprite** (4×4 walking animation) with camera follow
- 🧊 Simple AABB **collisions** from a `collision` object layer
- 🔍 **HD tilesets** support with scale factor auto‑computed
- 🧩 Pure Kotlin, tiny API, easy to extend

---

## 📦 Install (GitHub Packages)

Add the GitHub Packages repo once in your **`settings.gradle`**:

```kotlin
// settings.gradle(.kts)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // GameworldJEM on GitHub Packages
        maven {
            url = uri("https://maven.pkg.github.com/joelmora9618/GameworldJEM")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: settings.providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN")
                    ?: settings.providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
```

> 💡 Put your token in **`~/.gradle/gradle.properties`** (or CI secrets), *not* in the repo:
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PAT   # repo:read + packages:read (and write for publishing)
```

Add the dependency to your **app module**:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.jem.gameworldjem:gameworldjem:0.1.0")
}
```

---

## 🧰 Quick start

1) Place your assets (maps & sprites) under your app module at:
```
app/src/main/assets/tiled/
├─ demo_map.tmj
├─ tileset_office.tsj / .tsx  (and PNG referenced inside)
└─ hero_spritesheet.png
```

2) Drop the screen anywhere in your Compose tree:
```kotlin
@Composable
fun Demo() {
    GameWorldScreen(
        title = "GameWorld Demo",
        basePath = "tiled",
        mapFile = "demo_map.tmj",
        heroSheetPath = "tiled/hero_spritesheet.png",
        desiredTilesTall = 1.6f // zoom hero (≈ tiles of height on screen)
    )
}
```

That’s it—run the app 🎉

---

## 🗺️ Creating maps with **Tiled** (🧡)

- **Orientation**: `orthogonal`
- **Tile size**: any (e.g., 32×32, 64×64, 128×128). The renderer scales to your map.
- **Layers**:
  - `ground` (tile layer) — or name any tile layers you like
  - `collision` (object layer) — add rectangles where the player shouldn’t walk
- **Tilesets**:
  - External `.tsj` (JSON) or `.tsx` (XML)
  - Use `margin`/`spacing` if your atlas has padding
  - Can mix multiple tilesets; **firstgid** is handled automatically

> ✅ Tip: Keep a clear grid, e.g., 128×128 office tiles. If sprites get “cut”, increase **spacing/margin** in your atlas or in Tiled’s tileset settings.

---

## 🧍 Hero spritesheet spec

- **Grid**: 4×4 (16 frames total)
- **Rows** (top→bottom): **Down, Left, Right, Up**
- **Columns**: Frame 0 is idle; frames 1–3 animate walk
- **Padding**: Use generous **margin**/**spacing** to avoid cropping
- Example naming: `tiled/hero_spritesheet.png`

The engine assumes a 4×4 layout and computes sub‑frames like this:
```
sx = margin + frameIdx * (frameW + spacing)
sy = margin + row      * (frameH + spacing)
```
Where `row` is based on direction (Down/Left/Right/Up).

---

## ⚙️ API surface (mini)

```kotlin
@Composable
fun GameWorldScreen(
    modifier: Modifier = Modifier,
    title: String = "Top-Down Demo",
    onBack: (() -> Unit)? = null,
    basePath: String = "tiled",
    mapFile: String = "demo_map.tmj",
    heroSheetPath: String = "tiled/hero_spritesheet.png",
    desiredTilesTall: Float = 1.6f // hero zoom
)
```

- **`desiredTilesTall`** controls hero size on screen (e.g., `1.0f` = ~1 tile tall).
- Camera auto‑centers on the hero.
- Collisions read from an object layer named `collision`.

---

## 🧱 Multi‑tileset support (how it’s resolved)
When drawing a `gid`, the engine finds the tileset where `gid >= firstgid` (largest match), then computes local tile index and samples the atlas using either supplied `columns` or derived from the PNG width/height + `margin/spacing`. This makes mixed tilesets “just work”.

---

## 🧪 Sample assets
Bring your own PNGs or grab free sets, then match their tile size in Tiled. Place everything under `assets/tiled`. The loader normalizes image paths (e.g., strips folders inside `.tsx`) and reads from `assets/tiled/`.

---

## 🩹 Troubleshooting

- **“Sprites cut off”** → Increase `spacing/margin` in the tileset, or enlarge spacing in the hero sheet.  
- **“Black tiles / wrong atlas offset”** → Check `firstgid` and that the atlas PNG size matches `tilewidth/height`, `margin`, `spacing`.  
- **“Nothing draws”** → Verify files exist under `app/src/main/assets/tiled/` and names match `mapFile` / `heroSheetPath`.  
- **AGP warning about `compileSdk`** → Update Android Gradle Plugin or add `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` temporarily.  
- **GitHub Packages 401** → Ensure `gpr.user` / `gpr.key` or `GITHUB_ACTOR` / `GITHUB_TOKEN` are set (read permission is enough for consumers).  

---

## 🛣️ Roadmap
- Layers ordering/z-index helpers
- Animated tiles support
- NPC entities & triggers
- Pathfinding helpers
- Offscreen culling improvements

---

## 🤝 Contributing
PRs welcome! Please keep the module small and dependency‑light. Add demos where helpful.

---

## 📄 License
MIT — do what you love. Attribution appreciated but not required.

---

Made with 💙 by **@joelmora9618** — if this helped you, drop a ⭐ on the repo!