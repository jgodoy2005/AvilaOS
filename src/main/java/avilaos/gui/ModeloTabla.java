package avilaos.gui;

import javax.swing.table.AbstractTableModel;

public class ModeloTabla<T> extends AbstractTableModel {
    @FunctionalInterface
    public interface Columnas<T> {
        Object valor(T fila, int columna);
    }

    private final String[] titulos;
    private final Columnas<T> columnas;
    private T[] filas;

    public ModeloTabla(String[] titulos, Columnas<T> columnas) {
        this.titulos = titulos;
        this.columnas = columnas;
    }

    public void setFilas(T[] filas) {
        this.filas = filas;
        fireTableDataChanged();
    }

    public T getFila(int indice) {
        return filas[indice];
    }

    @Override
    public int getRowCount() {
        return filas == null ? 0 : filas.length;
    }

    @Override
    public int getColumnCount() {
        return titulos.length;
    }

    @Override
    public String getColumnName(int columna) {
        return titulos[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        return columnas.valor(filas[fila], columna);
    }
}