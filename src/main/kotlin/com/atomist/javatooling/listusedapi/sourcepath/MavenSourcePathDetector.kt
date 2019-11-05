package com.atomist.javatooling.listusedapi.sourcepath

import java.io.File

class MavenSourcePathDetector: SourcePathDetector {
    override fun getSourcePaths(path: String): Set<ModuleSourcePath> {
        return setOf(ModuleSourcePath(path, setOf(path + File.separator + "src/main/java",
                path + File.separator + "src/test/java")))
    }
}
