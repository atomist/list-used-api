package com.atomist.javatooling.listusedmethods;

import java.util.Collection;

public interface ClasspathResolver {
	Collection<String> resolveCompileClasspath(String projectPath);
	Collection<String> resolveTestCompileClasspath(String projectPath);
}
