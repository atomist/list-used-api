package com.atomist.javatooling.listusedapi.parsing

import com.atomist.javatooling.listusedapi.classpath.ClasspathResolver
import com.atomist.javatooling.listusedapi.classpath.GradleClassPathResolver
import com.atomist.javatooling.listusedapi.classpath.MavenClassPathResolver
import com.atomist.javatooling.listusedapi.toNullable
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.google.gson.Gson
import java.io.File
import java.io.IOException

class UsedApiLocator(val path: String,
                     val sourcePaths: List<String>,
                     val build: String,
                     val files: String?,
                     val languageLevel: String,
                     val definitionsFile: String) {
    fun locate(): Set<String> {
        val javaParser = getJavaParser(languageLevel);
        val sourceFiles = sourcePaths
                .filter {
                    File(it).exists()
                }
                .flatMap {
                    files?.split(",") ?: getJavaFiles(it)
                }
        val definitionsFile = File(definitionsFile)
        val definitions = Gson().fromJson<ApiDefinition>(definitionsFile.readText(), ApiDefinition::class.java)
        return sourceFiles
                .map { file ->
                    try {
                        val parseResult = javaParser.parse(File(file))
                        return@map if (parseResult.isSuccessful) {
                            val relativeFileName = file.substring(path.length + 1)
                            val usedMethods = findMethodUsage(definitions.methods, parseResult.result.toNullable(), relativeFileName)
                            val usedClasses = findClassUsage(definitions.classes, parseResult.result.toNullable(), relativeFileName)
                            val usedAnnotations = findAnnotationUsage(definitions.annotations, parseResult.result.toNullable(), relativeFileName)
                            val usedFields = findFieldsUsage(definitions.fields, parseResult.result.toNullable(), relativeFileName)
                            usedMethods.union(usedClasses).union(usedAnnotations)
                        } else {
                            setOf()
                        }
                    } catch (e: IOException) {
                        setOf<String>()
                    }
                }
                .reduce { a, b -> a.union(b) }
    }

    private fun getJavaParser(languageLevel: String): JavaParser {
        val reflectionTypeSolver = ReflectionTypeSolver()
        reflectionTypeSolver.parent = reflectionTypeSolver

        val combinedSolver = CombinedTypeSolver()
        combinedSolver.add(reflectionTypeSolver)
        sourcePaths
                .filter {
                    File(it).exists()
                }
                .forEach {
                    combinedSolver.add(JavaParserTypeSolver(File(it)))
                }
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

    private fun findMethodUsage(methods: Set<String>, parseResult: CompilationUnit?, relativeFileName: String): Set<String> {
        val m = parseResult?.findAll(MethodCallExpr::class.java) ?: listOf()
        return m.filter { try {  methods.contains(it.resolve().qualifiedSignature) } catch (e: Exception) { false } }
                .map {
                    "$relativeFileName:${it.range.get().begin.line}"
                }
                .toSet()
    }

    private fun findAnnotationUsage(annotations: Set<String>, parseResult: CompilationUnit?, relativeFileName: String): Set<String> {
        val a = parseResult?.findAll(AnnotationExpr::class.java) ?: listOf()
        return a.filter { try {  annotations.contains(it.resolve().qualifiedName) } catch (e: Exception) { false } }
                .map {
                    "$relativeFileName:${it.range.get().begin.line}"
                }
                .toSet()
    }

    private fun findClassUsage(classes: Set<String>, parseResult: CompilationUnit?, relativeFileName: String): Set<String> {
        val a = parseResult?.findAll(ClassOrInterfaceType::class.java) ?: listOf()
        return a.filter { try {  classes.contains(it.resolve().qualifiedName) } catch (e: Exception) { false } }
                .map {
                    "$relativeFileName:${it.range.get().begin.line}"
                }
                .toSet()
    }

    private fun findFieldsUsage(fields: Set<String>, parseResult: CompilationUnit?, relativeFileName: String): Set<String> {
        val a = parseResult?.findAll(FieldAccessExpr::class.java) ?: listOf()
        return a.filter { try {  fields.contains(it.resolve().name) } catch (e: Exception) { false } }
                .map {
                    "$relativeFileName:${it.range.get().begin.line}"
                }
                .toSet()
    }

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
