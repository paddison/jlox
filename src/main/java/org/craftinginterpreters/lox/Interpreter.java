package org.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object> {
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        return switch (expr.operator.type) {
            case BANG -> !isTruthy(right);
            case MINUS -> -(double)right;
            default -> null; // unreachable
        };
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        return switch (expr.operator.type) {
            case GREATER -> (double)left > (double)right;
            case GREATER_EQUAL -> (double)left >= (double)right;
            case LESS -> (double)left < (double)right;
            case LESS_EQUAL -> (double)left <= (double)right;
            case MINUS -> (double)left - (double)right;
            case BANG_EQUAL -> !isEqual(left, right);
            case EQUAL_EQUAL -> isEqual(left, right);
            case PLUS ->
                    (left instanceof Double && right instanceof Double) ?
                        (double)left + (double)right :
                    (left instanceof String && right instanceof String) ?
                        (String)left + (String)right :
                    null;
            case SLASH -> (double)left / (double)right;
            case STAR -> (double)left * (double)right;
            default -> null; // unreachable
        };
    }

    // TODO check this separately
    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object cond = evaluate(expr.cond);

        if (isTruthy(cond)) {
            return evaluate(expr.left);
        } else {
            return evaluate(expr.right);
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b ==null) return true;
        if (a == null) return false;

        return a.equals(b);
    }
}
