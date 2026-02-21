/*
 * Author: Swagat Subhash Kalita
 * Date: 2026-02-20
 */

package downloader

import java.io.File

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: ParallelFileDownloader <url> <output-file>")
        return
    }
    val url = args[0]
    val output = File(args[1])
    val downloader = ParallelFileDownloader()
    val result = downloader.download(url, output)
    if (result.success) {
        println("Downloaded ${result.bytesWritten} bytes to ${output.absolutePath}")
    } else {
        System.err.println("Download failed: ${result.message}")
    }
}
