package com.etsy.conjecture;

/**
 * @author Diane Hu
 */
public class GenericPair<F, S> implements java.io.Serializable {

    private static final long serialVersionUID = 123L;
    public F first;
    public S second;

    /**
     * Class constructor specifying the first and second number to create
     * 
     * @param first
     *            first number
     * @param second
     *            second number
     */

    public GenericPair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * The method gets first number
     * 
     * @return first number
     */
    public F getFirst() {
        return first;
    }

    /**
     * The method sets first number
     * 
     * @param fisrt
     *            first number
     */
    public void setFirst(F first) {
        this.first = first;
    }

    /**
     * The method gets second number
     * 
     * @return second number
     */
    public S getSecond() {
        return second;
    }

    /**
     * The method sets second number
     * 
     * @param second
     *            second number
     */
    public void setSecond(S second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return first + "," + second;
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (!(o instanceof GenericPair<?, ?>))
            return false;
        GenericPair<F, S> p = (GenericPair<F, S>)o;
        return (p.first).equals(first) && (p.second).equals(second);
    }

    public int hashCode() {
        return 17 + first.hashCode() * 31 + second.hashCode();
    }

}
