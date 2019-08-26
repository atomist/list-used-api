package com.atomist.javatooling.listusedmethods;

import com.google.common.collect.Sets;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;

public class MavenClassPathResolver implements ClasspathResolver {
	@Override
	public Collection<String> resolveCompileClasspath(String projectPath) {
		return getDependencies(projectPath);
	}

	@Override
	public Collection<String> resolveTestCompileClasspath(String projectPath) {
		return getDependencies(projectPath);
	}

	private static Collection<String> getDependencies(String path) {
		try {
			File tempOutput = File.createTempFile("mvnClasspath", ".txt");
			String[] env = System.getenv().entrySet().stream()
					.map(e -> e.getKey() + "=" + e.getValue())
					.toArray(String[]::new);
			Process p = Runtime.getRuntime().exec("mvn dependency:build-classpath -Dmdep.outputFile=" + tempOutput.getAbsolutePath(), env, new File(path));
			p.waitFor();
			String dependencies = Files.readAllLines(tempOutput.toPath()).get(0);
			return Sets.newTreeSet(Arrays.asList(dependencies.split(File.pathSeparator)));
		} catch(Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
