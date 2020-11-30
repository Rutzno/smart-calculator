package calculator;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mack_TB
 * @version 1.0.7
 * @since 11/23/2020
 */

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Map<String, BigInteger> mapBig = new HashMap<>();

        label:
        while (true) {
            String numbers = scanner.nextLine();
            switch (numbers) {
                case "/exit":
                    System.out.println("Bye!");
                    break label;
                case "/help":
                    System.out.println("The program calculates the sum|sub|mul|div|pow of numbers");
                    break;
                default:
                    if (!numbers.isEmpty()) {
                        if (handleVar(mapBig, numbers)) break;

                        Pattern pattern = Pattern.compile("((\\d+|[a-zA-Z]+)\\s+(\\d+|[a-zA-Z]+))|\\*{2,}|/{2,}");
                        Matcher matcher = pattern.matcher(numbers);

                        if (numbers.matches("/.*")) {
                            System.out.println("Unknown command");
                            break;
                        } else if (matcher.find() || getNumParentheses(numbers) % 2 != 0) {
                            System.out.println("Invalid expression");
                            break;
                        }

                        numbers = cleanNumber(numbers);

                        StringBuilder postfix = infixToPostfix(numbers);
                        BigInteger result = compute(postfix, mapBig);
                        System.out.println(result);
                    }
                    break;
            }
        }
    }

    /**
     * Yields false if there is no variable handling
     * @param map
     * @param numbers
     * @return
     */
    private static boolean handleVar(Map<String, BigInteger> map, String numbers) {
        String varRegex = "[a-zA-Z]+";
        String assignRegex = "\\s*\\w+\\s*=\\s*.+";

        if (numbers.matches(assignRegex)) { // variable
            numbers = numbers.replaceAll("\\s+", "");
            String[] tab = numbers.split("=");
            if (!tab[0].matches(varRegex)) {
                System.out.println("Invalid identifier");
            } else if ((!tab[1].matches(varRegex) && !tab[1].matches("-?\\d+")) || tab.length > 2) {
                System.out.println("Invalid assignment");
            } else if (tab[1].matches(varRegex) && !map.containsKey(tab[1])) {
                System.out.println("Unknown variable");
            } else if (tab[1].matches(varRegex) && map.containsKey(tab[1])) {
                map.put(tab[0], map.get(tab[1]));
            } else {
                map.put(tab[0], new BigInteger(tab[1]));
            }
            return true;
        } else if (numbers.matches(varRegex) && map.containsKey(numbers)) {
            System.out.println(map.get(numbers));
            return true;
        } else if (numbers.matches(varRegex) && !map.containsKey(numbers)){
            System.out.println("Unknown variable");
            return true;
        }
        return false;
    }

    private static String cleanNumber(String numbers) {
        Pattern pattern = Pattern.compile("--+");
        Matcher matcher = pattern.matcher(numbers);
        while (matcher.find()) {
            int val = matcher.end() - matcher.start();
            numbers = val % 2 == 0 ?
                    numbers.replaceFirst("--+", "+") :
                    numbers.replaceFirst("--+", "-");
        }

        numbers = numbers.replaceAll("\\++\\s", "+");
        //numbers = numbers.replaceAll("-\\s+", "-");
        return numbers;
    }

    private static int sum(String... numbers) {
        int result = 0;
        for (String number : numbers) {
            result += Integer.parseInt(number);
        }
        return result;
    }

    /**
     * The reason to convert infix to postfix
     * expression is that we can compute the
     * answer of postfix expression easier by
     * using a stack.
     * @param number
     * @return
     */
    private static StringBuilder infixToPostfix(String number) {
        StringBuilder postfix = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();
        number = putSpace(number);
        String[] tab = number.split("\\s+");

        for (String str : tab) {
            if (str.matches("(-?\\d+)|[a-zA-Z]+")) {
                postfix.append(str).append(" ");
            } else if (str.matches("[+*/()^-]")) {
                char operator = str.charAt(0);
                if (!stack.isEmpty()) {
                    char op1 = operator;
                    char op2 = stack.peekLast();
                    if (op2 != '(' && !priority(op1, op2)) {
                        while (stack.peekLast() != null) {
                            operator = stack.pollLast();
                            if (priority(op1, operator) || operator == '(') break;
                            postfix.append(operator).append(" ");
                        }
                    }
                    if (op1 == ')') {
                        while (stack.peekLast() != null) {
                            operator = stack.pollLast();
                            if (operator == '(') break;
                            postfix.append(operator).append(" ");
                        }
                        continue;
                    }
                }
                operator = str.charAt(0);
                stack.offerLast(operator);
            }
        }
        while (stack.peekLast() != null) {
            char operator = stack.pollLast();
            postfix.append(operator).append(" ");
        }
        return postfix;
    }

    private static boolean priority(char op1, char op2) {
        boolean pr = false;
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-') || op1 == '/' && op2 == '*') {
            pr = true;
        } else if ((op1 == '(' || op1 == ')') && (op2 == '^' || op2 == '*' || op2 == '/' || op2 == '+' || op2 == '-')) {
            pr = true;
        } else if (op1 == '^' && (op2 == '*' || op2 == '/' || op2 == '+' || op2 == '-')) {
            pr = true;
        }

        return pr;
    }

    private static String putSpace(String numbers) {
        numbers = numbers.replaceAll("\\^", " ^ ");
        numbers = numbers.replaceAll("\\*", " * ");
        numbers = numbers.replaceAll("\\(", " ( ");
        numbers = numbers.replaceAll("\\)", " ) ");
        numbers = numbers.replaceAll("\\+", " + ");
        String[] tab = numbers.split("\\s+");
        for (String str : tab) {
            if (str.matches("(\\d+|[a-zA-Z]+)-(\\d+|[a-zA-Z]+)")) {
                String[] tab2 = str.split("-");
                String operand1 = tab2[0];
                String operand2 = tab2[1];
                numbers = numbers.replaceAll(str, operand1 + " - " + operand2);
            }
        }

        return numbers;
    }


    private static BigInteger compute(StringBuilder postfix, Map<String, BigInteger> map) {
        BigInteger result = BigInteger.ZERO;
        Deque<BigInteger> stack = new ArrayDeque<>();
        String[] tab = postfix.toString().trim().split("\\s+");
        for (String str : tab) {
            if (str.matches("-?\\d+")) {
                stack.offerLast(new BigInteger(str));
            } else if (str.matches("-?[a-zA-Z]")) {
                if (str.contains("-")) {
                    str = str.replaceFirst("-", "");
                    BigInteger value = map.get(str);
                    stack.offerLast(value.negate());
                } else {
                    BigInteger value = map.get(str);
                    stack.offerLast(value);
                }

            } else { // operator
                BigInteger operand2 = stack.pollLast();
                BigInteger operand1 = stack.pollLast();
                result = calculator(operand1, operand2, str);

                stack.offerLast(result);
            }
        }
        result = stack.peekLast() != null ? stack.peekLast() : result;
        return result;
    }

    /*private static int calculator(int operand1, int operand2, String operator) {
        int result = 0;
        switch (operator) {
            case "+":
                result = operand1 + operand2;;
                break;
            case "-":
                result = operand1 - operand2;
                break;
            case "*":
                result = operand1 * operand2;
                break;
            case "/":
                result = operand1 / operand2;
                break;
            case "^":
                result = (int) Math.pow(operand1, operand2);
                break;
        }
        return result;
    }*/

    private static BigInteger calculator(BigInteger operand1, BigInteger operand2, String operator) {
        BigInteger result = BigInteger.ZERO;
        switch (operator) {
            case "+":
                result = operand1.add(operand2);
                break;
            case "-":
                result = operand1.subtract(operand2);
                break;
            case "*":
                result = operand1.multiply(operand2);
                break;
            case "/":
                result = operand1.divide(operand2);
                break;
            case "^":
                int exponent = operand2.intValue();
                result = operand1.pow(exponent);
                break;
        }
        return result;
    }


    private static int getNumParentheses(String number) {
        String str = number;
        str = putSpace(str);
        String[] tab = str.split("\\s+");
        int count = 0;
        for (String str2 : tab) {
            if (("(").equals(str2) || ")".equals(str2)) {
                count++;
            }
        }
        return count;
    }
}
