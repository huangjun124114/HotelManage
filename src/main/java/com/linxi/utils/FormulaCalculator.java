package com.linxi.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公式计算器
 * 支持 + - * / 四则运算，变量从 values Map 中获取
 */
@Slf4j
public class FormulaCalculator {

    private static final Pattern VAR_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)");

    /**
     * 计算公式
     * @param formula 公式表达式，如 "room_nights / own_room_count"
     * @param values 变量值映射
     * @return 计算结果
     */
    public static BigDecimal calculate(String formula, Map<String, BigDecimal> values) {
        if (formula == null || formula.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            String expr = formula.trim();
            Matcher matcher = VAR_PATTERN.matcher(expr);
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;

            while (matcher.find()) {
                String varName = matcher.group(1);
                // 检查是否为关键字
                if (isKeyword(varName)) {
                    continue;
                }
                sb.append(expr, lastEnd, matcher.start());
                BigDecimal value = values.getOrDefault(varName, BigDecimal.ZERO);
                sb.append(value.toPlainString());
                lastEnd = matcher.end();
            }
            sb.append(expr, lastEnd, expr.length());

            String resolvedExpr = sb.toString();
            return evaluate(resolvedExpr);
        } catch (Exception e) {
            log.error("公式计算失败: formula={}, values={}", formula, values, e);
            return BigDecimal.ZERO;
        }
    }

    private static boolean isKeyword(String word) {
        return word.equals("if") || word.equals("else") || word.equals("min") || word.equals("max");
    }

    /**
     * 简单四则运算表达式求值
     * 支持 + - * / 和括号
     */
    private static BigDecimal evaluate(String expr) {
        // 去除所有空格
        expr = expr.replaceAll("\\s+", "");

        // 先处理乘除
        expr = processOperator(expr, '*', '/');
        // 再处理加减
        expr = processOperator(expr, '+', '-');

        try {
            return new BigDecimal(expr);
        } catch (NumberFormatException e) {
            log.error("表达式求值失败: {}", expr);
            return BigDecimal.ZERO;
        }
    }

    private static String processOperator(String expr, char op1, char op2) {
        StringBuilder sb = new StringBuilder(expr);
        int i = 0;
        while (i < sb.length()) {
            char c = sb.charAt(i);
            if (c == op1 || c == op2) {
                // 找左边的数字
                int leftStart = i - 1;
                while (leftStart >= 0 && (Character.isDigit(sb.charAt(leftStart)) || sb.charAt(leftStart) == '.' || sb.charAt(leftStart) == '-')) {
                    if (sb.charAt(leftStart) == '-' && leftStart > 0 && Character.isDigit(sb.charAt(leftStart - 1))) {
                        break;
                    }
                    leftStart--;
                }
                leftStart++;
                String leftStr = sb.substring(leftStart, i);
                BigDecimal left = new BigDecimal(leftStr);

                // 找右边的数字
                int rightEnd = i + 1;
                if (rightEnd < sb.length() && sb.charAt(rightEnd) == '-') {
                    rightEnd++;
                }
                while (rightEnd < sb.length() && (Character.isDigit(sb.charAt(rightEnd)) || sb.charAt(rightEnd) == '.')) {
                    rightEnd++;
                }
                String rightStr = sb.substring(i + 1, rightEnd);
                BigDecimal right = new BigDecimal(rightStr);

                BigDecimal result;
                if (c == '*') {
                    result = left.multiply(right);
                } else if (c == '/') {
                    if (right.compareTo(BigDecimal.ZERO) == 0) {
                        result = BigDecimal.ZERO;
                    } else {
                        result = left.divide(right, 10, RoundingMode.HALF_UP);
                    }
                } else if (c == '+') {
                    result = left.add(right);
                } else {
                    result = left.subtract(right);
                }

                String resultStr = result.stripTrailingZeros().toPlainString();
                sb.replace(leftStart, rightEnd, resultStr);
                i = leftStart + resultStr.length() - 1;
            }
            i++;
        }
        return sb.toString();
    }
}
