package jlox;

import java.util.ArrayList;
import java.util.List;

import static jlox.TokenType.*;

class Parser {
	/**
	 * Sentinel class used to unwind the parser
	 * 
	 * Some parse errors occur in places where the parser
	 * isn't likely to get into a weird state and we don't
	 * need to synchronize
	 *
	 * In those places, we simply report the error
	 */
	private static class ParseError extends RuntimeException {}
	
	private final List<Token> tokens;
	private int current = 0;
	
	/**
	 * Like the scanner, the parser consumes a flat input
	 * sequence, only now we're reading tokens instead of
	 * characters
	 */
	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}
	
	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		
		while (!isAtEnd())
			statements.add(statement());
		
		return statements;
	}
	
	/**
	 * Each method for parsing a grammar rule produces a
	 * syntax tree for that rule and returns it to the
	 * caller
	 *
	 * When the body of rule contains a nonterminal (a
	 * reference to another rule) we call that other
	 * rule's method
	 */
	
	/*
	 *  statement:			exprStmt
	 *  					| printStmt
	 */
	private Stmt statement() {
		if (match(PRINT))
			return printStatement();
		
		return expressionStatement();
	}
	
	// printStmt:			"print" expression ";"
	private Stmt printStatement() {
		Expr value = expression();
		
		/**
		 * Since the "print" token has already been matched,
		 * we just parse the subsequent expression, consume
		 * the terminating semicolon and emit the syntax tree
		 */
		consume(SEMICOLON, "Expect ';' after value.");
		
		return new Stmt.Print(value);
	}
	
	// exprStmt:			expression ";"
	private Stmt expressionStatement() {
		Expr expr = expression();
		
		consume(SEMICOLON, "Expect ';' after expression.");
		
		return new Stmt.Expression(expr);
	}

	// expression:			equality;
	private Expr expression() {

		// The first rule simply expand to the equality rule
		return equality();
	}
	
	// equality:			comparison (("!=" | "==") comparison)*
	private Expr equality() {
		Expr expr = comparison();

		/**
		 * The "()*" loop in the rule maps to a while loop
		 *
		 * We can see that inside the rule, we must first find
		 * either a "!=" or "==" token, so, if we don't see one
		 * of those, we must be done with the sequence of
		 * equality operators
		 */
		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();

			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	// comparison:			term ((">" | ">=' | "<" | "<=") term)*
	private Expr comparison() {
		Expr expr = term();

		/**
		 * The process for matching binary operations remains the
		 * same for all the nested binary rules
		 */
		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();

			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}
	
	// term:				factor (("+" | "-") factor)*
	private Expr term() {
		Expr expr = factor();

		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = factor();

			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	// factor:				unary (("/" | "*") unary)*
	private Expr factor() {
		Expr expr = unary();

		while(match(SLASH, STAR)) {
			Token operator = previous();
			Expr right = unary();

			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	// unary:				("!" | "-") unary
	private Expr unary() {
		if (match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();

			return new Expr.Unary(operator, right);
		}

		return primary();
	}

	/**
	 * - primary:			NUMBER
	 *   					| STRING
	 *   					| "true"
	 *   					| "false"
	 *   					| "nil"
	 *   					| "(" expression ")"
	 */
	private Expr primary() {
		if (match(FALSE))
			return new Expr.Literal(false);

		if (match(TRUE))
			return new Expr.Literal(true);

		if (match(NIL))
			return new Expr.Literal(null);

		if (match(NUMBER, STRING)) 
			return new Expr.Literal(previous().literal);

		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			
			consume(RIGHT_PAREN, "Expect ')' after expression.");

			return new Expr.Grouping(expr);
		}
		
		throw error(peek(), "Expect expression.");
	}
	
	// Checks to see if the current token has any of the given types
	private boolean match(TokenType ... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();

				return true;
			}
		}

		return false;
	}
	
	/**
	 * Similar to match. If some other token is there,
	 * an error is reached
	 */
	private Token consume(TokenType type, String message) {
		if (check(type))
			return advance();
		
		throw error(peek(), message);
	}
	
	// Similar to match, but never consumes the token
	private boolean check(TokenType type) {
		if (isAtEnd())
			return false;

		return peek().type == type;
	}
	
	// Consumes the current token and returns it
	private Token advance() {
		if (!isAtEnd())
			current++;

		return previous();
	}

	// Checks if we've run out of tokens to parse
	private boolean isAtEnd() {
		return peek().type == EOF;
	}

	// Returns the current token yet to be consumed
	private Token peek() {
		return tokens.get(current);
	}

	// Returns the most recently consumed token
	private Token previous() {
		return tokens.get(current - 1);
	}
	
	// Handles mismatch of expected tokens
	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		
		return new ParseError();
	}
	
	private void synchronize() {
		advance();
		
		while (!isAtEnd()) {
			if (previous().type == SEMICOLON)
				return;
			
			switch(peek().type) {
				case CLASS:
					
				case FUN:
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case PRINT:
				case RETURN:
					return;
			}
		}
		
		advance();
	}
}
