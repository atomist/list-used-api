package com.atomist.javatooling.listusedapi.sourcepath

interface SourcePathDetector {
    fun getSourcePaths(path: String): Set<String>
}
