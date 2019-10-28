#!/usr/bin/env kscript
@file:DependsOn("com.github.javaparser:javaparser-symbol-solver-core:3.14.11")
@file:DependsOn("com.github.ajalt:clikt:2.1.0")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit

fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split(" ")
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
        proc.waitFor(5, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}

interface ClasspathResolver {
    fun resolveCompileClasspath(projectPath: String): Set<String>
}

class GradleClassPathResolver : ClasspathResolver {
    private val INIT_SCRIPT = """allprojects {
	apply plugin: "java"
	task listCompilePath(dependsOn: configurations.compileClasspath) {
		doLast {
			println "classpath=${"$"}{configurations.testCompileClasspath.collect { File file -> file }.join(';')}"
		}
	}
}
"""

    override fun resolveCompileClasspath(projectPath: String): Set<String> {
        val initGradle = File.createTempFile("init", ".gradle")
        FileWriter(initGradle).use { writer ->
            writer.append(INIT_SCRIPT)
            writer.flush()
        }
        val output = ("gradle --init-script " + initGradle.absolutePath + " list").runCommand(File(projectPath))
        val regex = Regex("classpath=(.*)")
        return regex.findAll(output!!)
                .flatMap { r -> r.groups[1]!!.value.splitToSequence(";") }
                .toSet()
    }
}

class MavenClassPathResolver : ClasspathResolver {
    override fun resolveCompileClasspath(projectPath: String): Set<String> {
        val tempOutput = File.createTempFile("mvnClasspath", ".txt")
        "mvn dependency:build-classpath -Dmdep.outputFile=${tempOutput.absolutePath}".runCommand(File(projectPath))
        val dependencies = Files.readAllLines(tempOutput.toPath())[0]
        return dependencies.split(File.pathSeparator).toSet()
    }
}

class ExecuteTransformCommand(val transformer: (CompilationUnit, File) -> Unit) : CliktCommand() {
    val path: String by option(help = "Project root path").required()
    val srcFolder: String by option(help = "Sources path").default("src/main/java")
    val testSrcFolder: String by option(help = "Test sources path").default("src/test/java")
    val build: String by option(help = "Build system").choice("gradle", "maven").default("gradle")
    val languageLevel: String by option(help = "Language level").choice("8", "9", "10", "11", "12", "13").default("8")

    override fun run() {
        val srcFolders: List<String> = listOf(srcFolder, testSrcFolder)
        val parser: JavaParser = getJavaParser(languageLevel, path, srcFolders, build)
        srcFolders.map { path + File.separator + it }
                .flatMap { getJavaFiles(it) }
                .forEach { javaFile: String ->
                    val file = File(javaFile)
                    val parsed = parser.parse(File(javaFile))
                    parsed.ifSuccessful { cu -> transformer(cu, file) }
                }
    }

    fun getJavaParser(languageLevel: String, rootPath: String, sourcePaths: List<String>, build: String): JavaParser {
        val reflectionTypeSolver = ReflectionTypeSolver()
        reflectionTypeSolver.parent = reflectionTypeSolver
        val combinedSolver = CombinedTypeSolver()
        combinedSolver.add(reflectionTypeSolver)
        sourcePaths.map { JavaParserTypeSolver(rootPath + File.separator + it) }
                .forEach { combinedSolver.add(it) }
        val resolver: ClasspathResolver
        if ("gradle" == build) {
            resolver = GradleClassPathResolver()
        } else if ("maven" == build) {
            resolver = MavenClassPathResolver()
        } else {
            throw IllegalArgumentException("Unknown build system: " + build)
        }
        resolver.resolveCompileClasspath(rootPath)
                .filter { d -> d.endsWith(".jar") }
                .map(JarTypeSolver::getJarTypeSolver)
                .forEach { combinedSolver.add(it) }

        val configuration = ParserConfiguration()
        configuration.setSymbolResolver(JavaSymbolSolver(combinedSolver))
        configuration.languageLevel = ParserConfiguration.LanguageLevel.valueOf("JAVA_${languageLevel.replace(".", "_")}")
        configuration.isLexicalPreservationEnabled = true
        return JavaParser(configuration)
    }

    fun getJavaFiles(vararg paths: String): List<String> {
        val files = mutableListOf<String>()
        paths.forEach { file ->
            val walk = File(file).walkTopDown()
            walk.iterator().forEach { walkFile ->
                if(walkFile.isFile && walkFile.name.endsWith(".java")) {
                    files.add(walkFile.absolutePath)
                }
            }
        }
        return files;
    }
}
