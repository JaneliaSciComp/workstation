package org.janelia.jacs2.asyncservice.imageservices.align;

public class Matrix<E> {
    public Object[][] elems;

    public Matrix(int rows, int cols) {
        elems = new Object[rows][cols];
    }

    public E getElem(int row, int col) {
        return (E) elems[row][col];
    }

    public void setElem(int row, int col, E elem) {
        elems[row][col] = elem;
    }
}
