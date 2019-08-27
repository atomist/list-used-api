package com.atomist.javatooling.listusedmethods;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

public class CliArguments extends OptionsBase {

	@Option(
			name = "path",
			abbrev = 'p',
			help = "Set the path",
			category = "parser",
			defaultValue = ""
	)
	public String path;

	@Option(
			name = "files",
			abbrev = 'f',
			help = "Set the specific sourcefiles",
			category = "parser",
			defaultValue = ""
	)
	public String files;

	@Option(
			name = "srcFolder",
			abbrev = 's',
			help = "Set the source subfolder",
			category = "parser",
			defaultValue = "src/main/java"
	)
	public String sourceSubfolder;

	@Option(
			name = "testFolder",
			abbrev = 't',
			help = "Set the test source subfolder",
			category = "parser",
			defaultValue = "src/test/java"
	)
	public String testSourceSubfolder;

	@Option(
			name = "build",
			abbrev = 'b',
			help = "Sets the build system (gradle or maven)",
			category = "parser",
			defaultValue = "gradle"
	)
	public String build;
}
