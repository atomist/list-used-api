package com.atomist.javatooling.listusedapi;

import java.util.Collection;

public interface ClasspathResolver {
	Collection<String> resolveCompileClasspath(String projectPath);
}
