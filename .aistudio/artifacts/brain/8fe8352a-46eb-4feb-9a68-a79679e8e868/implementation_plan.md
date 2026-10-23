# Aura Music — Minimalist Offline Music Player

Aura Music is a minimalist, high-craft mobile music player inspired by Apple Music and Spotify. It enables effortless offline audio playback by scanning device storage and importing audio files directly, while empowering listeners to organize tracks into custom Albums, Singers (Artists) folders, and a dedicated Liked Songs collection—complete with an interactive Up Next queue, Equalizer presets, and a Sleep Timer.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design decisions have been aligned based on your preferences and are ready for your review before implementation begins.

- **Confirmed Decision 1 — Visual Theme & Aesthetic**: Minimalist Apple Music editorial style featuring adaptive Light and Dark modes (with an in-app appearance selector), translucent elevated surfaces, bold display typography, and dynamic accent tinting derived from the active song's artwork palette.
- **Confirmed Decision 2 — Audio Import & Offline Discovery**: Hybrid offline ingestion combining automatic device `MediaStore` scanning (with runtime permission handling) and zero-permission Storage Access Framework (`OpenMultipleDocuments`) multi-file importing, complemented by an on-device WAV studio generator so fresh emulators can immediately test real audio playback, scrubbing, and equalizer effects.
- **Confirmed Decision 3 — Library Organization & Playback Tools**: Full support for creating, editing, and organizing custom **Albums** and **Singers** folders, curating a **Liked Songs** list, managing the **Up Next Queue** (with Shuffle and Repeat Off/All/One), and tuning audio via **Built-in Equalizer Presets** and a **Sleep Timer**.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a distraction-free offline listening experience where users can scan or import local audio files, group songs into custom or metadata-driven Albums and Singers folders, favorite tracks into Liked Songs, and control playback through a persistent floating mini-player and an expandable full-screen Now Playing studio.
- **Target Audience / Persona**: Music lovers and audiophiles who maintain local audio collections on their device and want the visual refinement of Apple Music/Spotify combined with flexible folder-like organization for Singers and Albums.
- **Key Value**: Bridges automatic ID3 metadata indexing with manual user curation—allowing users to create custom Singer folders or Album collections, assign cover palettes, and move or assign imported tracks seamlessly without leaving the app.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First Launch & Device Media Sync**:
   - Users land on the **Library Home** screen featuring quick-access hero cards (*Liked Songs*, *Recently Added*, *Scan Device*, and *Import Files*).
   - Tapping **Scan Device** requests Android's audio read permission (`READ_MEDIA_AUDIO` on Android 13+ or `READ_EXTERNAL_STORAGE` on older versions) and indexes all local audio tracks into the offline database.
   - Tapping **Import Audio Files** opens the system document picker (`OpenMultipleDocuments`) to import specific `.mp3`, `.wav`, `.flac`, or `.m4a` tracks with persistent URI permissions—requiring zero broad storage permissions.
   - For immediate testing on empty emulators, users can also tap **Load Studio Acoustic Samples** to generate real, multi-instrument offline WAV tracks directly on the device.

2. **Organizing Albums, Singers Folders & Liked Songs**:
   - **Albums Tab**: Displays an Apple Music-inspired 2-column editorial grid of Album folders. Users can tap `+ New Album` to create a custom album folder (setting title, singer, year, and cover theme color), edit existing album details, or add/remove songs.
   - **Singers Tab**: Displays curated Singer (Artist) folders with circular studio avatars, track counts, and album counts. Users can tap `+ New Singer` to create a custom Singer folder, rename or edit bio/accent tags, and assign tracks directly to that Singer.
   - **Liked Songs**: Every track row and the Now Playing screen features a one-tap heart toggle with spring physics. All favorited tracks are automatically aggregated in the dedicated **Liked Songs** view with instant Play All and Shuffle controls.

3. **Playback, Persistent Mini-Player & Full-Screen Now Playing**:
   - Selecting any song starts real offline audio playback and reveals a floating **Mini-Player** docked cleanly above the bottom navigation bar, displaying cover artwork, scrolling title/singer, heart toggle, play/pause, and a thin real-time progress bar.
   - Tapping the Mini-Player expands the full-screen **Now Playing** sheet featuring large elevated album artwork, interactive seek slider with elapsed/remaining timestamps, Shuffle/Repeat toggles, and bottom utility pills for **Up Next Queue**, **Equalizer**, and **Sleep Timer**.

4. **Queue, Equalizer & Sleep Timer**:
   - **Up Next Queue Sheet**: View currently playing context, jump to any upcoming track, reorder tracks (move up/down), or remove songs from the active queue.
   - **Equalizer & Audio FX Sheet**: Toggle real Android `Equalizer` / `BassBoost` audio session effects (with graceful acoustic fallback) across presets: *Flat*, *Acoustic*, *Bass Boost*, *Vocal Clarity*, *Late Night*, and *Electronic*, paired with interactive 5-band sliders.
   - **Sleep Timer Sheet**: Set a countdown timer (*15 min*, *30 min*, *45 min*, *60 min*, or *End of Track*) with a live remaining badge that gently pauses playback upon completion.

### Visual Identity & Theme

- **Aesthetic Direction**: Minimalist Apple Music & Spotify hybrid—clean architectural grids, generous whitespace in Light Mode, deep obsidian surfaces in Dark Mode, and expressive album artwork containers.
- **Color Palette & Mood**:
  - *Signature Brand Accent*: **Crimson Pulse** (`#FA243C` in Light / `#FF375F` in Dark — Apple Music-inspired vibrant rose-crimson) paired with **Spotify Emerald** indicators (`#1DB954`) for active equalizer/shuffle states.
  - *Adaptive Light Posture*: Pure Alabaster background (`#FAFAFC`), Frosted Pearl elevated cards (`#FFFFFF`), and Deep Ink typography (`#111114`) with Muted Slate secondary labels (`#6E6E77`).
  - *Adaptive Dark Posture*: Obsidian Studio background (`#0A0A0E`), Elevated Charcoal cards (`#16161D`), Glass Surface containers (`#22222C`), and Crisp Snow text (`#F5F5F7`).
  - *Dynamic Artwork Ambient Tint*: Each Album and Singer folder stores a curated harmonic color seed so Album detail headers and the full-screen Now Playing backdrop render a soft radial gradient wash matching the track's artwork.
- **Typography & Hierarchy**:
  - *Display & Headings*: **Plus Jakarta Sans** (Bold / SemiBold) bundled locally for tight, modern editorial headers, album titles, and navigation headers.
  - *Body & Track Lists*: **Inter** (Regular / Medium) for crisp legibility across dense song lists, metadata subtitles, and tabular timestamps.
- **Component Styling & Layout**:
  - Standard Material 3 `NavigationBar` with four primary destinations: **Home (Songs)**, **Albums**, **Singers**, and **Liked**, respecting edge-to-edge system navigation bar insets.
  - Floating rounded Mini-Player (`16.dp` corner radius, subtle tonal border, and elevation) anchored above the navigation bar.
  - Responsive layout supporting both compact portrait screens and wider multi-column grids.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Local Persistence with Room Database (`androidx.room`)**
  - *Chosen Approach*: Store all indexed tracks, custom Album folders, custom Singer folders, Liked statuses, and playlist-folder mappings in a local Room SQLite database exposed via reactive Kotlin `Flow` streams.
  - *Why*: An offline music player requires instant zero-latency queries, relational grouping (Songs ↔ Albums ↔ Singers), and persistent custom folder organization that survives app restarts without requiring cloud sign-in.
  - *Alternatives Considered*: Querying `MediaStore` directly on every screen load was rejected because it does not allow users to create custom virtual Album/Singer folders, edit track assignments non-destructively, or persist custom imported SAF file URIs alongside Liked states.

- **Decision 2: Dual Audio Ingestion + Built-in Synthesizer Sample Generator**
  - *Chosen Approach*: Combine `MediaStore.Audio.Media` device scanning and Storage Access Framework (`ActivityResultContracts.OpenMultipleDocuments`) with an on-device PCM WAV audio generator that synthesizes real playable melodic tracks in app storage when requested or on first run.
  - *Why*: Gives users authentic offline file import on physical devices while guaranteeing that testing in a cloud emulator has genuine playable audio files with real durations, scrubbing, and equalizer session attachment.

- **Decision 3: Native `MediaPlayer` + `AudioEffect` Equalizer Engine**
  - *Chosen Approach*: Encapsulate Android's `MediaPlayer` and `android.media.audiofx.Equalizer` inside a lifecycle-aware controller managed by the `ViewModel` and repository layer, with a 250ms coroutine ticker for smooth seekbar updates and countdown sleep timer tracking.
  - *Why*: Provides reliable local URI playback (`content://` and `file://`), direct `audioSessionId` binding for hardware/software Equalizer presets, and accurate completion callbacks for Repeat One, Repeat All, and Shuffle queue progression.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        JETPACK COMPOSE UI LAYER                            │
│                                                                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐  │
│  │   Home / Songs   │  │   Albums Grid    │  │   Singers Folder Grid    │  │
│  │  Search, Filter, │  │  + Create/Edit   │  │   + Create/Edit Singer   │  │
│  │  Scan & Import   │  │  Album Detail    │  │   Singer Detail & Tracks │  │
│  └────────┬─────────┘  └────────┬─────────┘  └────────────┬─────────────┘  │
│           │                     │                         │                │
│           └─────────────────────┼─────────────────────────┘                │
│                                 ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │               Liked Songs Screen & Track Options Sheet               │  │
│  │       (Assign to Album, Assign to Singer, Edit Metadata, Like)       │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
│                                 ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │       Persistent Mini-Player Bar  ──►  Full-Screen Now Playing       │  │
│  │       ┌────────────────────┐ ┌───────────────────┐ ┌───────────────┐ │  │
│  │       │ Up Next Queue Sheet│ │ Equalizer Presets │ │  Sleep Timer  │ │  │
│  │       └────────────────────┘ └───────────────────┘ └───────────────┘ │  │
│  └──────────────────────────────┬───────────────────────────────────────┘  │
└─────────────────────────────────┼──────────────────────────────────────────┘
                                  │ StateFlow<MusicUiState> & UI Actions
                                  ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   VIEWMODEL & PLAYBACK CONTROLLER LAYER                    │
│                                                                            │
│  ┌───────────────────────────────────┐  ┌───────────────────────────────┐  │
│  │          MusicViewModel           │  │     AudioPlayerController     │  │
│  │ • Combines Room Flows             │◄─┤ • Android MediaPlayer Engine  │  │
│  │ • Search, Sort & Theme State      │  │ • Shuffle & Repeat Queue Logic│  │
│  │ • Folder & Track CRUD Actions     │  │ • Equalizer & BassBoost FX    │  │
│  │ • Sleep Timer Coroutine Job       │  │ • 250ms Progress Ticker Flow  │  │
│  └─────────────────┬─────────────────┘  └───────────────▲───────────────┘  │
└────────────────────┼────────────────────────────────────┼──────────────────┘
                     │                                    │
                     ▼                                    │ Content / File URI
┌─────────────────────────────────────────────────────────┼──────────────────┐
│                     DATA & DEVICE STORAGE LAYER         │                  │
│                                                         │                  │
│  ┌───────────────────────────┐   ┌──────────────────────┴───────────────┐  │
│  │   Room SQLite Database    │   │     Device Audio & Media Scanner     │  │
│  │ • SongEntity              │   │ • MediaStore.Audio.Media Query       │  │
│  │ • AlbumFolderEntity       │   │ • SAF OpenMultipleDocuments Importer │  │
│  │ • SingerFolderEntity      │   │ • MediaMetadataRetriever Parser      │  │
│  │ • MusicDao (Reactive Flow)│   │ • Studio WAV Sample Synthesizer      │  │
│  └───────────────────────────┘   └──────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

1. **`SongEntity` (Room Table `songs`)**:
   - `id` (PrimaryKey), `title`, `singerName`, `albumName`, `albumId`, `singerId`, `uriString`, `durationMs`, `isLiked`, `colorHex`, `dateAdded`, `trackNumber`, `isSample`.
2. **`AlbumFolderEntity` (Room Table `album_folders`)**:
   - `id` (PrimaryKey), `title`, `singerName`, `year`, `coverColorHex`, `description`, `createdAt`.
3. **`SingerFolderEntity` (Room Table `singer_folders`)**:
   - `id` (PrimaryKey), `name`, `genreTag`, `bio`, `avatarColorHex`, `createdAt`.
4. **`PlaybackState`**:
   - `currentSong`, `isPlaying`, `currentPositionMs`, `durationMs`, `queue` (`List<SongEntity>`), `currentQueueIndex`, `shuffleEnabled`, `repeatMode` (`OFF`, `ALL`, `ONE`), `equalizerPreset`, `bandLevels`, `bassBoostStrength`, `sleepTimerRemainingMs`.

### Interactive Component & State Mapping

- **Device Scan & SAF File Import**:
  - Requesting device scan invokes `rememberLauncherForActivityResult(RequestPermission)` for `READ_MEDIA_AUDIO` (API 33+) or `READ_EXTERNAL_STORAGE` (API <= 32), queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`, automatically groups discovered songs into corresponding `AlbumFolderEntity` and `SingerFolderEntity` records, and inserts them into Room.
  - Selecting files via `OpenMultipleDocuments` takes persistable URI permission (`takePersistableUriPermission`), extracts ID3 title, artist, album, and duration via `MediaMetadataRetriever`, and adds the songs into the user's library.
- **Custom Album & Singer Folder Management**:
  - Users can create, edit, or delete Album and Singer folders via modal dialogs with color palette pickers.
  - Tapping the three-dot menu on any song opens the **Track Organization Sheet**, allowing the user to move/assign the song to any Album folder or Singer folder, edit its title/artist/album metadata, toggle **Liked**, or add it to **Play Next** in the queue.
- **Now Playing, Queue & Audio FX Controls**:
  - Scrubbing the progress slider updates `AudioPlayerController.seekTo(positionMs)` immediately.
  - Reordering or removing items in the **Up Next Queue Sheet** mutates the active queue list while preserving the currently playing track's playback continuity.
  - Selecting an **Equalizer Preset** or adjusting the 5-band sliders applies real decibel adjustments to the active `Equalizer` instance (and persists settings in state) while displaying a live visual frequency curve.
