package com.atomist.javatooling.listusedapi

import com.atomist.javatooling.listusedapi.parsing.UsedApiParser
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory

import java.util.*

fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null)

private val logger = LoggerFactory.getLogger("com.atomist.javatooling.listusedapi")

class ListUsedApi : CliktCommand() {
    val path: String by option(help = "Project root path").required()
    val srcFolder: String by option(help = "Sources path").default("src/main/java")
    val testSourceFolder: String by option(help = "Test sources path").default("src/test/java")
    val build: String by option(help = "Build system").choice("gradle", "maven").default("gradle")
    val files: String? by option(help = "Specific files")
    val languageLevel: String by option(help = "Language level").choice("8", "9", "10", "11", "12", "13").default("8")

    override fun run() {
        val usedApiParser = UsedApiParser(path, srcFolder, testSourceFolder, build, files, languageLevel)
        val usedApi = usedApiParser.parse();
        val gson = GsonBuilder().setPrettyPrinting().create()
        logger.info(gson.toJson(usedApi))
    }
}

fun main(args: Array<String>) {
    ListUsedApi().main(args);
}
