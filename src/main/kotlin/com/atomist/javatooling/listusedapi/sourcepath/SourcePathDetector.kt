package com.atomist.javatooling.listusedapi.sourcepath

interface SourcePathDetector {
    fun getSourcePaths(path: String): Set<ModuleSourcePath>
}

data class ModuleSourcePath(
        val module: String,
        val sourcePaths: Set<String>
)
