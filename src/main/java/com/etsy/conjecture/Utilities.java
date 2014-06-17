package com.etsy.conjecture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import com.google.common.hash.*;
import com.google.common.collect.Lists;

/**
 * class of static data science utility methods
 * 
 * @author jattenberg
 * 
 */
public class Utilities {

    public static final double SMALL = 1e-10;
    public static final HashFunction HASHER = Hashing.md5();
    public static final double ROOT2 = Math.sqrt(2d);
    public static final double LOG2 = Math.log(2.);

    private Utilities() {
    }

    public static String cleanLine(String line) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c < 128 && Character.isLetter(c)) {
                buffer.append(c);
            } else {
                buffer.append(' ');
            }
        }
        return buffer.toString().toLowerCase();
    }

    public static String cleanLineRobust(String input, String separator,
            boolean ignoreNumbers) {
        StringBuilder buff = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(input,
                " +.,~\\<>\\$?!:;(){}|" + "\b\t\n\f\r\"\'\\\\/\\=\\&\\%\\_");

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            token = token.replaceAll("-{2,}", "-");
            token = token.replaceAll("^-", "");
            token = token.replaceAll("-$", "");
            if (token.length() < 2
                    || (ignoreNumbers && StringUtils.containsAny(token,
                            "0123456789")))
                continue;
            buff.append(token + separator);
        }
        int index = buff.lastIndexOf(separator);
        if (index >= 0)
            buff.delete(index, buff.length());
        return buff.toString();
    }

    public static String checkNotBlank(String s) {
        if (StringUtils.isBlank(s)) {
            throw new IllegalArgumentException("Argument cannot be blank");
        }
        return s;
    }

    public static List<String> checkNotBlank(List<String> S) {
        for (String s : S)
            checkNotBlank(s);
        return S;
    }

    public static String[] checkNotBlank(String[] S) {
        for (String s : S)
            checkNotBlank(s);
        return S;
    }

    public static double stringInnerProduct(Map<String, Double> coefficients,
            Collection<String> input) {
        double output = 0;
        for (String token : input)
            output += coefficients.containsKey(token) ? coefficients.get(token)
                    : 0;
        return output;
    }

    public static double sigmoid(double operand) {
        return 1. / (1. + Math.exp(-operand));
    }

    /**
     * derivative of the sigmoid function
     */
    public static double dsigmoid(double operand) {
        return Math.exp(operand) / Math.pow(1. + Math.exp(operand), 2.);
    }

    /**
     * returns the strings in input in sorted order
     * 
     * @param input
     * @return
     */
    public static String sortTerms(String input) {
        return sortTerms(input, "\\s+");
    }

    public static String sortTerms(String input, String delim) {
        String[] terms = input.split(delim);
        Arrays.sort(terms);
        return StringUtils.join(terms, delim);
    }

    public final static String cleanText(String tmp, int maxlen) {

        StringTokenizer tok = new StringTokenizer(tmp,
                " +.,~\\<>\\$?!:;(){}|-0123456789\b\t\n\f\r\"\'\\\\/\\=\\&\\%\\_");
        StringBuilder buff = new StringBuilder();
        while (tok.hasMoreTokens()) {
            String out = tok.nextToken();
            if (out.length() < 2 || out.length() > maxlen)
                continue;
            buff.append(out + " ");
        }
        return buff.toString();
    }

    public final static List<String> grams(String input, int[] gramSizes,
            String separator) {
        List<String> out = Lists.newArrayList();
        StringBuilder buff = new StringBuilder();
        String[] tokens = StringUtils.split(input);

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            for (int len : gramSizes) {
                if (len > i + 1)
                    continue;
                if (len == 1) {
                    out.add(token);
                    continue;
                }
                buff.setLength(0);

                for (int k = len - 1; k > 0; k--)
                    buff.append(tokens[i - k] + separator);
                buff.append(token);
                out.add(buff.toString());
            }
        }
        return out;
    }

    public static final boolean floatingPointEquals(double a, double b) {
        return (a - b < SMALL) && (b - a < SMALL);
    }

    public static int doubleHash(double d) {
        long t = Double.doubleToLongBits(d);
        return (int)(t ^ (t >>> 32));
    }

    public static double logistic(double x) {
        return 1d / (1 + Math.exp(-x));
    }

    static class ValueComparator<K, V extends Comparable<? super V>> implements
            Comparator<Map.Entry<K, V>> {
        boolean reverse;

        public ValueComparator(boolean reverse) {
            this.reverse = reverse;
        }

        public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
            int res = a.getValue().compareTo(b.getValue());
            return reverse ? -res : res;
        }
    }

    public static <K, V extends Comparable<? super V>> ArrayList<K> orderKeysByValue(
            Map<K, V> map) {
        return orderKeysByValue(map, false);
    }

    public static <K, V extends Comparable<? super V>> ArrayList<K> orderKeysByValue(
            Map<K, V> map, boolean reverse) {
        ArrayList<Map.Entry<K, V>> keys = new ArrayList<Map.Entry<K, V>>();
        keys.addAll(map.entrySet());
        Collections.sort(keys, new ValueComparator<K, V>(reverse));
        ArrayList<K> res = new ArrayList<K>();
        for (int i = 0; i < keys.size(); i++) {
            res.add(keys.get(i).getKey());
        }
        return res;
    }

    public static <K, V extends Comparable<? super V>> List<K> topKeysByValue(
            Map<K, V> map, int n) {
        ArrayList<K> keys = orderKeysByValue(map, true);
        ArrayList<K> res = new ArrayList<K>(n);
        for (int i = 0; i < n && i < keys.size(); i++) {
            res.add(keys.get(i));
        }
        return res;
    }
}
