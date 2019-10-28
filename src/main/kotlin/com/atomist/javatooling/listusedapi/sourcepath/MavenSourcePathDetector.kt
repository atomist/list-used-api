package com.atomist.javatooling.listusedapi.sourcepath

import java.io.File

class MavenSourcePathDetector: SourcePathDetector {
    override fun getSourcePaths(path: String): Set<String> {
        return setOf(path + File.separator + "src/main/java",
                path + File.separator + "src/test/java");
    }
}
