package com.atomist.javatooling.listusedapi.classpath

interface ClasspathResolver {
    fun resolveCompileClasspaths(projectPath: String): Set<ModuleClasspath>
}

data class ModuleClasspath(
        val module: String,
        val classpath: Set<String>
)
