package org.janelia.jacs2.asyncservice.imageservices;

public class FijiColor {
    private char code;
    private int divisor;

    FijiColor(char code, int divisor) {
        this.code = code;
        this.divisor = divisor;
    }

    public char getCode() {
        return code;
    }

    public int getDivisor() {
        return divisor;
    }
}
