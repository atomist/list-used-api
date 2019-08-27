package com.atomist.javatooling.listusedmethods;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GradleClassPathResolver implements ClasspathResolver {
	private String getInitScript(String configuration) {
		return "allprojects {\n" +
				"\tapply plugin: \"java\"\n" +
				"\ttask listCompilePath(dependsOn: configurations.compileClasspath) {\n" +
				"\t\tdoLast {\n" +
				"\t\t\tprintln \"classpath=${configurations." + configuration + ".collect { File file -> file }.join(';')}\"\n" +
				"\t\t}\n" +
				"\t}\n" +
				"}\n";
	}

	@Override
	public Collection<String> resolveCompileClasspath(String projectPath) {
		return getDependencies(projectPath, "testCompileClasspath");
	}

	private Collection<String> getDependencies(String path, String configuration) {
		try {
			File initGradle = File.createTempFile("init", ".gradle");
			try(FileWriter writer = new FileWriter(initGradle)) {
				writer.append(getInitScript(configuration));
				writer.flush();
			}
			String[] env = System.getenv().entrySet().stream()
					.map(e -> e.getKey() + "=" + e.getValue())
					.toArray(String[]::new);
			Process p = Runtime.getRuntime().exec("gradle --init-script " + initGradle.getAbsolutePath() + " list", env, new File(path));
			StringBuilder builder = new StringBuilder();
			try(InputStream is = p.getInputStream()) {
				try (InputStreamReader reader = new InputStreamReader(is)) {
					try (BufferedReader buffered = new BufferedReader(reader)) {
						String line = buffered.readLine();
						while (line != null) {
							builder.append(line).append("\n");
							line = buffered.readLine();
						}
					}
				}
			}
			String output = builder.toString();
			String regex = "classpath=(.*)";
			return Lists.newArrayList(Splitter.on("\n").split(output))
					.stream()
					.filter(l -> l.matches(regex))
					.flatMap(l -> {
						Matcher m = Pattern.compile(regex).matcher(l);
						if(m.matches()) {
							String classpath = m.group(1);
							return Arrays.stream(classpath.split(";"));
						} else {
							return Arrays.stream(new String[0]);
						}
					})
					.collect(Collectors.toSet());
		} catch(Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
