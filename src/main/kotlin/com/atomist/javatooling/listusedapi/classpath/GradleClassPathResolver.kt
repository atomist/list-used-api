package com.atomist.javatooling.listusedapi.classpath

import java.io.*
import java.util.stream.Collectors.toSet

class GradleClassPathResolver : ClasspathResolver {
    private val INIT_SCRIPT = """
    allprojects {
        task listCompileClasspath {
            doLast {
                if(configurations.testCompileClasspath)
                println "classpath{${'$'}{project.name}}=${'$'}{configurations.testCompileClasspath.collect { File file -> file }.join(';')}"
            }
        }
    }
"""

    override fun resolveCompileClasspaths(projectPath: String): Set<ModuleClasspath> {
        val initGradle = File.createTempFile("init", ".gradle")
        FileWriter(initGradle).use { writer ->
            writer.append(INIT_SCRIPT)
            writer.flush()
        }
        val wrapperPath = getWrapperPath(projectPath);
        val output = ("$wrapperPath --init-script " + initGradle.absolutePath + " assemble listCompileClasspath").runCommand(File(projectPath))
        val regex = Regex("classpath\\{(.*)\\}[:=](.*)")

        return regex.findAll(output!!)
                .map { r -> ModuleClasspath(r.groups[1]!!.value, r.groups[2]!!.value.splitToSequence(";").toSet()) }
                .toSet()
    }

    fun getWrapperPath(path: String) : String {
        val wrapperPath = File("$path/gradlew")
        val parentPath = File(path).parent
        if(wrapperPath.exists()) {
            return wrapperPath.absolutePath
        } else if(File("$path/settings.gradle").exists()) {
            return "gradle"
        } else if(parentPath != null) {
            return getWrapperPath(File(path).parent)
        } else {
            return "gradle"
        }
    }
}
