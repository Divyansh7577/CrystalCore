package com.crystalville.crystalcore.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /math <expression>
 * Evaluates a mathematical expression in-game: +, -, *, /, ^ (power),
 * parentheses, decimals, negative numbers. Open to everyone.
 * Example: /math (2+3)*4-1^2  ->  19
 *
 * Small hand-written recursive-descent parser/evaluator - no external
 * libraries or scripting engines, no code-injection risk from input.
 */
public class MathCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /math <expression>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /math (2+3)*4-1^2", NamedTextColor.GRAY));
            return true;
        }

        String expression = String.join(" ", args);

        try {
            double result = new ExpressionEvaluator(expression).evaluate();
            String formatted = formatResult(result);

            sender.sendMessage(Component.text(expression + " = ", NamedTextColor.GRAY)
                    .append(Component.text(formatted, NamedTextColor.GREEN)));
        } catch (ArithmeticException e) {
            sender.sendMessage(Component.text("Math error: " + e.getMessage(), NamedTextColor.RED));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid expression: " + e.getMessage(), NamedTextColor.RED));
        }

        return true;
    }

    private String formatResult(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /**
     * Grammar (standard precedence, ^ right-associative):
     *   expr    := term (('+' | '-') term)*
     *   term    := power (('*' | '/') power)*
     *   power   := unary ('^' power)?
     *   unary   := '-' unary | '+' unary | primary
     *   primary := NUMBER | '(' expr ')'
     */
    private static final class ExpressionEvaluator {
        private final String input;
        private int pos = 0;

        ExpressionEvaluator(String input) {
            this.input = input.replaceAll("\\s+", "");
            if (this.input.isEmpty()) {
                throw new IllegalArgumentException("empty expression");
            }
        }

        double evaluate() {
            double result = parseExpression();
            if (pos != input.length()) {
                throw new IllegalArgumentException("unexpected character at position " + pos);
            }
            return result;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (pos < input.length() && (peek() == '+' || peek() == '-')) {
                char op = next();
                double rhs = parseTerm();
                value = (op == '+') ? value + rhs : value - rhs;
            }
            return value;
        }

        private double parseTerm() {
            double value = parsePower();
            while (pos < input.length() && (peek() == '*' || peek() == '/')) {
                char op = next();
                double rhs = parsePower();
                if (op == '*') {
                    value = value * rhs;
                } else {
                    if (rhs == 0) {
                        throw new ArithmeticException("division by zero");
                    }
                    value = value / rhs;
                }
            }
            return value;
        }

        private double parsePower() {
            double base = parseUnary();
            if (pos < input.length() && peek() == '^') {
                next();
                double exponent = parsePower();
                return Math.pow(base, exponent);
            }
            return base;
        }

        private double parseUnary() {
            if (pos < input.length() && peek() == '-') {
                next();
                return -parseUnary();
            }
            if (pos < input.length() && peek() == '+') {
                next();
                return parseUnary();
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            if (pos >= input.length()) {
                throw new IllegalArgumentException("unexpected end of expression");
            }

            if (peek() == '(') {
                next();
                double value = parseExpression();
                if (pos >= input.length() || peek() != ')') {
                    throw new IllegalArgumentException("missing closing parenthesis");
                }
                next();
                return value;
            }

            int start = pos;
            if (peek() == '.') {
                throw new IllegalArgumentException("number cannot start with '.'");
            }
            while (pos < input.length() && (Character.isDigit(peek()) || peek() == '.')) {
                pos++;
            }

            if (pos == start) {
                throw new IllegalArgumentException("expected a number at position " + pos);
            }

            String numberStr = input.substring(start, pos);
            try {
                return Double.parseDouble(numberStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid number: " + numberStr);
            }
        }

        private char peek() {
            return input.charAt(pos);
        }

        private char next() {
            return input.charAt(pos++);
        }
    }
  }
