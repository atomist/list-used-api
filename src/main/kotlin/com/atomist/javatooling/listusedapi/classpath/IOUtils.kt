package com.atomist.javatooling.listusedapi.classpath

import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split(" ")
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .redirectErrorStream(true)
                .start();
        val output = CompletableFuture.supplyAsync{ proc.inputStream.bufferedReader().readText() }.join()
        proc.waitFor(5, TimeUnit.MINUTES)
        return output;
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}
