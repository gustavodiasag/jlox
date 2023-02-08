package jlox;

/**
 * Unlike the Java cast exception, the class tracks
 * the token that identifies where in the user's
 * code the runtime error came from. As with static
 * errors, this helps the user know where to fix
 */
class RuntimeError extends RuntimeException {
	final Token token;
	
	RuntimeError(Token token, String message) {
		super(message);
		this.token = token;
	}
}
