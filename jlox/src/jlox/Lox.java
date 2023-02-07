package jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
	static boolean hadError = false;

	public static void main(String[] args) throws IOException {
		
		/**
		 * Lox is a scripting language, which means it executes directly
		 * from source
  		 *
		 * The interpreter supports two ways of running code
		 */
		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64);

		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}

	/**
	 * If you start jlox from the command line and give it a path
	 * to a file, it reads the file and executes it
	 */
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));

		run(new String(bytes, Charset.defaultCharset()));
		
		// Indicate an error in the exit code
		if (hadError)
			System.exit(65);
	}

	/**
	 * Using jlox without any arguments results in a prompt where
	 * you can enter and execute code one line at a time
	 */
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		/**
		 * To kill the command-line application, type Ctrl+D, doing so
		 * signals and "end-of-file" condition to the program
       	 *
		 * When that happens, readLine() returns null, exiting the loop 
		 */
		for (;;) {
			System.out.print("> ");
	
			String line = reader.readLine();

			if (line == null)
				break;

			run(line);

			hadError = false;
		}
	}

	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();

		for (Token token : tokens)
			System.out.println(token);
	}

	static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err.println(
			"[line " + line + "] Error" + where + ": " + message);

		hadError = true;
	}
}