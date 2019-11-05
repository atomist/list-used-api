package com.atomist.javatooling.listusedapi.sourcepath

import com.atomist.javatooling.listusedapi.classpath.ModuleClasspath
import com.atomist.javatooling.listusedapi.classpath.runCommand
import java.io.File
import java.io.FileWriter
import java.lang.RuntimeException

class GradleSourcePathDetector: SourcePathDetector {
    private val INIT_SCRIPT = """
    allprojects {
        task printSourceSets {
            doLast {
                println "sourcepaths{${'$'}{project.name}}=" + project.sourceSets.collectMany {
                   it.getAllJava().getSrcDirTrees().collect { it.dir } 
                }.join(",")
            }
        }
    }
"""

    override fun getSourcePaths(path: String): Set<ModuleSourcePath> {
        val initGradle = File.createTempFile("init", ".gradle")
        FileWriter(initGradle).use { writer ->
            writer.append(INIT_SCRIPT)
            writer.flush()
        }
        val wrapperPath = getWrapperPath(path);
        val output = ("$wrapperPath --init-script " + initGradle.absolutePath + " printSourceSets").runCommand(File(path))
        val regex = Regex("sourcepaths\\{(.*)\\}[=:](.*)")
        return regex.findAll(output!!)
                .map { r -> ModuleSourcePath(r.groups[1]!!.value, r.groups[2]!!.value.splitToSequence(";").toSet()) }
                .toSet();
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
