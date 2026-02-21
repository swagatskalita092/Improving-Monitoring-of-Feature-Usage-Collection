# Problem Statement

## Improving Monitoring of Feature Usage Collection

### Context

Our reporting SDK is built in **Kotlin** and is responsible for **collecting, validating, anonymizing, and transmitting usage statistics**. Because this code runs on **client machines outside our control**, it is critical that we collect statistics *about* the statistics collection itself. The goal is to improve our monitoring by implementing additional statistics collectors and refactoring existing ones so they can be **reused across different products**.

---

### Task #1: Parallel File Downloader

Implement a **file downloader** (in **Java** or **Kotlin**) that can **download chunks of a file in parallel**. The parts are fetched from a web server by specifying a **URL**.

#### Server behaviour

You can run a local web server using:

```bash
docker run --rm -p 8080:80 -v /path/to/your/local/directory:/usr/local/apache2/htdocs/ httpd:latest
```

Files in that directory are then available at `http://localhost:8080/<filename>` (e.g. `http://localhost:8080/my-local-file.txt`).

The server is expected to behave as follows:

- **HEAD request** to the URL: the response includes the headers  
  - `Accept-Ranges: bytes`  
  - `Content-Length: <number of bytes>`
- **Chunk download:** send a **GET** request with a **Range** header, for example:  
  `Range: bytes=1024-2047`

The downloader must:

1. **Fetch chunks in parallel**
2. **Reassemble them** into the complete file

#### Deliverables

- Implementation of the file downloader
- **Unit tests** that verify the correctness of the downloader

---

### What you’ll learn

- Best practices in **feature usage collection**
- **SDK design** principles
- Understanding and working with **large codebases**

---

### Requirements

| | |
|---|---|
| **Must have** | Strong understanding of programming language concepts and paradigms |
| | Basic knowledge of HTTP and JSON APIs |
| | Good written and verbal communication skills |
| | Self-motivation and ability to work independently on research tasks |
| **Nice to have** | Experience with JVM-based languages (Java, Kotlin) |
