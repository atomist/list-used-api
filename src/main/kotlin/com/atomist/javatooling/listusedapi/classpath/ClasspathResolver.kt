package com.atomist.javatooling.listusedapi.classpath

interface ClasspathResolver {
    fun resolveCompileClasspath(projectPath: String): Set<String>
}
