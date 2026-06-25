package dev.zxilly.gradle

fun String.exec(): String {
    val parts = trim().split(Regex("\\s+"))
    val process = ProcessBuilder(parts)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
    val exitCode = process.waitFor()
    check(exitCode == 0) {
        "Command failed with exit code $exitCode: $this\n$output"
    }
    return output
}
