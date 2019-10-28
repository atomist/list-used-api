package com.atomist.javatooling.listusedapi.sourcepath

import java.io.File

class GradleSourcePathDetector: SourcePathDetector {
    private val INIT_SCRIPT = """allprojects {
	apply plugin: "java"
	tasks.register("printSourceSets") {
		doLast {
			this.sourceSets.forEach{
                println it.getAllJava().getSrcDirTrees()
            }
		}
	}
}
"""

    override fun getSourcePaths(path: String): List<String> {
        return listOf(path + File.separator + "src/main/java",
                path + File.separator + "src/test/java");
    }
}
