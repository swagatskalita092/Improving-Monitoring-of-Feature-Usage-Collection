# Parallel File Downloader

**Author:** Swagat Subhash Kalita · **Date:** 2026-02-20

A Kotlin CLI that downloads files from a URL using **parallel HTTP Range requests**. When the server supports it (`Accept-Ranges: bytes` and `Content-Length`), the file is split into chunks and downloaded concurrently; otherwise the downloader falls back to a single GET. Output is written directly to disk at the correct offsets (no full-file buffering).

---

## Requirements

- **JDK 17**
- **Docker** (to run tests without installing Gradle), or **Gradle 8.x** for local build/test

---

## Quick Start

### Run the downloader

```bash
gradle run --args="<url> <output-file>"
```

Example with a local Apache server:

```bash
# Terminal 1: start server (serve files from a folder)
docker run --rm -p 8080:80 -v /path/to/your/files:/usr/local/apache2/htdocs/ httpd:latest

# Terminal 2: download
gradle run --args="http://localhost:8080/myfile.txt ./myfile.txt"
```

### Run tests (Docker — recommended)

No need to install JDK or Gradle on your machine:

```bash
docker build -t downloader-test .
docker run --rm downloader-test
```

Use the **`.`** at the end of the build command. Wait for `BUILD SUCCESSFUL` (about 1–2 minutes the first time).

### Run tests (local)

If you have Gradle installed:

```bash
gradle test
```

Or from your IDE: open `src/test/kotlin/downloader/ParallelFileDownloaderTest.kt` and run the test class.

---

## What the tests cover

Four deterministic unit tests use an in-process mock HTTP server (no real server or Docker needed for the tests themselves):

| Test | Description |
|------|-------------|
| **Parallel chunks** | 200 KB file, chunked download; content matches. |
| **Non-even boundaries** | File size not divisible by chunk size; correct bytes. |
| **Fallback (no Accept-Ranges)** | Server omits `Accept-Ranges`; single GET used; file correct. |
| **Retry** | One chunk returns 500 once then succeeds; retry works. |

---

## Design notes

- **Range:** Chunks use inclusive `bytes=start-end`; last chunk ends at `contentLength - 1`.
- **Fallback:** Single GET is used when `Content-Length` is missing or `Accept-Ranges` is not `bytes`.
- **Resources:** All OkHttp responses are closed with `.use { }`; no connection leaks.
- **Timeouts:** Connect 30s, read 60s, write 30s.
- **File writing:** `RandomAccessFile` with `seek(start)` per chunk inside `synchronized(raf)` for thread-safe concurrent writes.

---

## Project layout

```
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
├── src/main/kotlin/downloader/
│   ├── ParallelFileDownloader.kt   # Core download logic
│   └── Main.kt                     # CLI entry point
└── src/test/kotlin/downloader/
    └── ParallelFileDownloaderTest.kt
```
