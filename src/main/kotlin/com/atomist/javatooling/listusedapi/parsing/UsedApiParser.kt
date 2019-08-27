package com.atomist.javatooling.listusedapi.parsing

import com.atomist.javatooling.listusedapi.classpath.ClasspathResolver
import com.atomist.javatooling.listusedapi.classpath.GradleClassPathResolver
import com.atomist.javatooling.listusedapi.classpath.MavenClassPathResolver
import com.atomist.javatooling.listusedapi.toNullable
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import java.io.File
import java.io.IOException

class UsedApiParser(val path: String,
                    val sourcePath: String,
                    val testSourcePath: String,
                    val build: String,
                    val files: String?,
                    val languageLevel: String) {
    fun parse(): UsedApi {
        val javaParser = getJavaParser(languageLevel);
        val sourceFiles: List<String> = files?.split(",") ?: getJavaFiles(path + File.separator + sourcePath,
                    path + File.separator + testSourcePath)
        return sourceFiles
                .map { file ->
                    try {
                        val parseResult = javaParser.parse(File(file))
                        if (parseResult.isSuccessful) {
                            val usedMethods = getUsedMethods(parseResult.result.toNullable())
                            val usedClasses = getUsedClasses(parseResult.result.toNullable())
                            val usedAnnotations = getUsedAnnotations(parseResult.result.toNullable())
                            UsedApi(usedMethods, usedClasses, usedAnnotations)
                        } else {
                            UsedApi(setOf(), setOf(), setOf())
                        }
                    } catch (e: IOException) {
                        UsedApi(setOf(), setOf(), setOf())
                    }
                }
                .reduce({ a, b -> a.merge(b) })
    }

    private fun getJavaParser(languageLevel: String): JavaParser {
        val reflectionTypeSolver = ReflectionTypeSolver()
        reflectionTypeSolver.parent = reflectionTypeSolver
        val mainJavaParserTypeSolver = JavaParserTypeSolver(File(path + File.separator + sourcePath))
        val testJavaParserTypeSolver = JavaParserTypeSolver(File(path + File.separator + testSourcePath))

        val combinedSolver = CombinedTypeSolver()
        combinedSolver.add(reflectionTypeSolver)
        combinedSolver.add(mainJavaParserTypeSolver)
        combinedSolver.add(testJavaParserTypeSolver)
        val resolver: ClasspathResolver
        if ("gradle" == build) {
            resolver = GradleClassPathResolver()
        } else if ("maven" == build) {
            resolver = MavenClassPathResolver()
        } else {
            throw IllegalArgumentException("Unknown build system: " + build)
        }
        resolver.resolveCompileClasspath(path)
                .filter { d -> d.endsWith(".jar") }
                .map(JarTypeSolver::getJarTypeSolver)
                .forEach { combinedSolver.add(it) }

        val configuration = ParserConfiguration()
        configuration.setSymbolResolver(JavaSymbolSolver(combinedSolver))
        configuration.languageLevel = ParserConfiguration.LanguageLevel.valueOf("JAVA_${languageLevel.replace(".", "_")}")
        configuration.isLexicalPreservationEnabled = true
        return JavaParser(configuration)
    }

    private fun getUsedMethods(parseResult: CompilationUnit?) =
            parseResult?.findAll(MethodCallExpr::class.java)
                    ?.map {
                        try {
                            it.resolve().qualifiedSignature
                        } catch(e: Exception) {
                            null
                        }
                    }
                    ?.filterNotNull()
                    ?.toSet() ?: setOf()

    private fun getUsedAnnotations(parseResult: CompilationUnit?) =
            parseResult?.findAll(AnnotationExpr::class.java)
                    ?.map {
                        try {
                            it.resolve().qualifiedName
                        } catch(e: Exception) {
                            null
                        }
                    }
                    ?.filterNotNull()
                    ?.toSet() ?: setOf()

    private fun getUsedClasses(parseResult: CompilationUnit?) =
            parseResult?.findAll(ClassOrInterfaceType::class.java)
                    ?.map {
                        try {
                            it.resolve().qualifiedName
                        } catch(e: Exception) {
                            null
                        }
                    }
                    ?.filterNotNull()
                    ?.toSet() ?: setOf()

    private fun getJavaFiles(vararg paths: String): List<String> {
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
