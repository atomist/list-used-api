package com.atomist.javatooling.listusedmethods;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.collect.Lists;
import com.google.devtools.common.options.OptionsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger("com.atomist.javatooling.listusedmethods");

	private static class ListUsedJavaMethodsException extends RuntimeException {
		ListUsedJavaMethodsException(Throwable cause) {
			super(cause);
		}
	}

	public static void main(String[] args) {
		OptionsParser parser = OptionsParser.newOptionsParser(CliArguments.class);
		parser.parseAndExitUponError(args);
		CliArguments arguments = parser.getOptions(CliArguments.class);
		if (Objects.requireNonNull(arguments).path.isEmpty() || arguments.build.isEmpty() || arguments.sourceSubfolder.isEmpty()) {
			printUsage(parser);
		} else {
			configureStaticJavaParser(arguments);
			getJavaFiles(arguments.path + File.separator + arguments.sourceSubfolder)
					.stream()
					.flatMap(file -> getUsedMethods(file).stream())
					.collect(Collectors.toCollection(TreeSet::new))
					.forEach(logger::info);
		}
	}

	private static void printUsage(OptionsParser parser) {
		if(logger.isErrorEnabled()) {
			logger.error("Usage: java -jar list-used-methods.jar OPTIONS");
			logger.error(parser.describeOptions(Collections.emptyMap(),
					OptionsParser.HelpVerbosity.LONG));
		}
	}


	private static void configureStaticJavaParser(CliArguments arguments) {
		TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
		reflectionTypeSolver.setParent(reflectionTypeSolver);
		TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(arguments.path + File.separator + arguments.sourceSubfolder));

		CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
		combinedSolver.add(reflectionTypeSolver);
		combinedSolver.add(javaParserTypeSolver);
		ClasspathResolver resolver;
		if ("gradle".equals(arguments.build)) {
			resolver = new GradleClassPathResolver();
		} else if("maven".equals(arguments.build)) {
			resolver = new MavenClassPathResolver();
		} else {
			throw new IllegalArgumentException("Unknown build system: " + arguments.build);
		}
		if("compile".equals(arguments.scope)) {
			resolver.resolveCompileClasspath(arguments.path).stream()
					.filter(d -> d.endsWith(".jar"))
					.map(Main::createJarTypeSolver)
					.forEach(combinedSolver::add);
		} else if ("test".equals(arguments.scope)) {
			resolver.resolveTestCompileClasspath(arguments.path).stream()
					.filter(d -> d.endsWith(".jar"))
					.map(Main::createJarTypeSolver)
					.forEach(combinedSolver::add);
		} else {
			throw new IllegalArgumentException("Unknown scope: " + arguments.scope);
		}

		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);
		StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
	}

	private static JarTypeSolver createJarTypeSolver(String j) {
		try {
			return new JarTypeSolver(j);
		} catch (IOException e) {
			logger.error(j);
			throw new ListUsedJavaMethodsException(e);
		}
	}

	private static Collection<String> getUsedMethods(String file) {
		try {
			CompilationUnit cu = StaticJavaParser.parse(new File(file));
			return cu.findAll(MethodCallExpr.class).stream()
					.map(m -> {
						try {
							return m.resolve().getQualifiedSignature();
						} catch (RuntimeException ignored) {
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		} catch(IOException e) {
			return Lists.newArrayList();
		}

	}

	private static List<String> getJavaFiles(String path) {
		List<String> files = Lists.newArrayList();
		try {
			Path startPath = Paths.get(path);
			Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir,
				                                         BasicFileAttributes attrs) {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if(file.getFileName().toString().endsWith(".java")) {
						files.add(file.toAbsolutePath().toString());
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException e) {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new IllegalArgumentException("Error getting Java files from path", e);
		}
		return files;
	}
}
