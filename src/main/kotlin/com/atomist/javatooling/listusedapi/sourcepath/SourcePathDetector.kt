package com.atomist.javatooling.listusedapi.sourcepath

interface SourcePathDetector {
    fun getSourcePaths(path: String): List<String>
}
