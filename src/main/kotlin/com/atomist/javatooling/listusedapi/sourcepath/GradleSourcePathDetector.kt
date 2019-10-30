package com.atomist.javatooling.listusedapi.sourcepath

import com.atomist.javatooling.listusedapi.classpath.runCommand
import java.io.File
import java.io.FileWriter

class GradleSourcePathDetector: SourcePathDetector {
    private val INIT_SCRIPT = """
allprojects {
    task printSourceSets {
        doLast {
            println "sourcepaths:" + project.sourceSets.collectMany {
               it.getAllJava().getSrcDirTrees().collect { it.dir } 
            }.join(",")
        }
    }
}
"""

    override fun getSourcePaths(path: String): Set<String> {
        val initGradle = File.createTempFile("init", ".gradle")
        FileWriter(initGradle).use { writer ->
            writer.append(INIT_SCRIPT)
            writer.flush()
        }
        val output = ("gradle --init-script " + initGradle.absolutePath + " printSourceSets").runCommand(File(path))
        val regex = Regex("sourcepaths[=:](.*)")
        return regex.findAll(output!!)
                .flatMap { r -> r.groups[1]!!.value.splitToSequence(",") }
                .toSet()
    }
}
