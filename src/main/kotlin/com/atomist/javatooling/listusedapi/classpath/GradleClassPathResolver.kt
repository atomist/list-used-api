package com.atomist.javatooling.listusedapi.classpath

import java.io.*

class GradleClassPathResolver : ClasspathResolver {
    private val INIT_SCRIPT = """
    allprojects {
        task listCompileClasspath {
            doLast {
                if(configurations.testCompileClasspath)
                println "classpath=${'$'}{configurations.testCompileClasspath.collect { File file -> file }.join(';')}"
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
        val output = ("gradle --init-script " + initGradle.absolutePath + " listCompileClasspath").runCommand(File(projectPath))
        val regex = Regex("classpath=(.*)")
        return regex.findAll(output!!)
                .flatMap { r -> r.groups[1]!!.value.splitToSequence(";") }
                .toSet()
    }
}
