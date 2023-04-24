package org.craftinginterpreters.lox;

import java.util.List;

import static org.craftinginterpreters.lox.TokenType.*;
public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        Token tk = peek();
        // check for binary expression token at the start of an expression
        switch (tk.type) {
            case PLUS, SLASH, STAR, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, BANG_EQUAL, QUESTION_MARK, COMMA -> {
                throw error(tk, "Found binary operator at beginning of expression. Discarding...");
            }
        }
        return comma();
    }

    private Expr comma() {
        // evaluate first expression and discard
        Expr expr = ternary();

        while (match(COMMA)) {
            // ignore the ',' token
            // evaluate all expressions that come after, discarding each but the last one
            expr = ternary();
        }

        return expr;
    }

    private Expr ternary() {
        // Left hand side needs to be evaluated first
        Expr expr = equality();

        if (match(QUESTION_MARK)) {
            Token question_mark = previous();
            Expr middle = equality();
            if (match(COLON)) {
                Token colon = previous();
                Expr right = ternary(); // make a recursive call, because ?! is right associative
                expr = new Expr.Ternary(expr, question_mark, middle, colon, right);
            } else {
                throw error(peek(), "Expect ':' after '?' in ternary expression");
            }
        }

        return expr;
    }
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> { return; }
            }

            advance();
        }
    }
}
