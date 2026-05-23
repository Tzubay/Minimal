package interpreter;

public class MatrixValue {
    public final int rows;
    public final int cols;
    public final int[] data;

    public MatrixValue(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new RuntimeException("Las dimensiones de la matriz deben ser mayores que 0.");
        }

        this.rows = rows;
        this.cols = cols;
        this.data = new int[rows * cols];
    }

    public int index(int row, int col) {
        if (row < 0 || row >= rows) {
            throw new RuntimeException("Fila fuera de rango: " + row);
        }

        if (col < 0 || col >= cols) {
            throw new RuntimeException("Columna fuera de rango: " + col);
        }

        return row * cols + col;
    }

    public int get(int row, int col) {
        return data[index(row, col)];
    }

    public void set(int row, int col, int value) {
        data[index(row, col)] = value;
    }
}