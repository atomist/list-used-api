package com.atomist.javatooling.listusedmethods;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

class CliArguments extends OptionsBase {
	@Option(
			name = "path",
			abbrev = 'p',
			help = "Set the path",
			category = "parser",
			defaultValue = ""
	)
	String path;

	@Option(
			name = "srcFolder",
			abbrev = 's',
			help = "Set the source subfolder",
			category = "parser",
			defaultValue = "src/main/java"
	)
	String sourceSubfolder;

	@Option(
			name = "build",
			abbrev = 'b',
			help = "Sets the build system (gradle or maven)",
			category = "parser",
			defaultValue = "gradle"
	)
	String build;

	@Option(
			name = "scope",
			abbrev = 'p',
			help = "Set the classpath scope of the scan (test or compile)",
			category = "parser",
			defaultValue = "compile"
	)
	String scope;
}
