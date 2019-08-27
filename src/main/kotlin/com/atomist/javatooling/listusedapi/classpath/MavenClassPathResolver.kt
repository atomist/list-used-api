package com.atomist.javatooling.listusedapi.classpath

import java.io.File
import java.nio.file.Files

class MavenClassPathResolver : ClasspathResolver {
    override fun resolveCompileClasspath(projectPath: String): Set<String> {
        val tempOutput = File.createTempFile("mvnClasspath", ".txt")
        "mvn dependency:build-classpath -Dmdep.outputFile=${tempOutput.absolutePath}".runCommand(File(projectPath))
        val dependencies = Files.readAllLines(tempOutput.toPath())[0]
        return dependencies.split(File.pathSeparator).toSet()
    }
}
