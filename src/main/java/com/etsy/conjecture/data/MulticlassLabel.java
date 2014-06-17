package com.etsy.conjecture.data;

/**
 * representing a 100% probability of membership in a particular class
 */
public class MulticlassLabel extends Label {

    private static final long serialVersionUID = 1L;

    protected String label;

    public MulticlassLabel() {
        this(null);
    }

    public MulticlassLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }

    public BinaryLabel toBinaryLabel(String className) {
        return new BinaryLabel(className.equals(label) ? 1.0 : 0.0);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MulticlassLabel other = (MulticlassLabel)obj;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        return true;
    }
}
