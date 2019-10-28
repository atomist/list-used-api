package com.atomist.javatooling.listusedapi

import com.atomist.javatooling.listusedapi.parsing.UsedApiLocator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import java.io.File

import java.util.*

fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null)

private val logger = LoggerFactory.getLogger("com.atomist.javatooling.listusedapi")

class ListUsedApi : CliktCommand() {
    val path: String by option(help = "Project root path").required()
    val build: String by option(help = "Build system").choice("gradle", "maven").default("gradle")
    val languageLevel: String by option(help = "Language level").choice("8", "9", "10", "11", "12", "13").default("8")
    val definitions: String by option(help = "JSON file with definition to look for").required()
    val outputFile: String? by option(help = "Output file name")

    override fun run() {
        val gson = GsonBuilder().setPrettyPrinting().create()
        if(definitions.isNotEmpty()) {
            val usedApiLocator = UsedApiLocator(path, build, languageLevel, definitions)
            val api = usedApiLocator.locate();
            if (outputFile != null && outputFile!!.isNotEmpty()) {
                val output = File(outputFile!!);
                if (!output.exists()) {
                    output.createNewFile();
                } else {
                    output.delete();
                    output.createNewFile();
                }
                output.writeText(gson.toJson(api))
            } else {
                logger.info(gson.toJson(api))
            }
        }
    }
}

fun main(args: Array<String>) {
    ListUsedApi().main(args);
}
