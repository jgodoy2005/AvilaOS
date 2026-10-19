package avilaos.gui;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

public class ModeloLista<E> extends AbstractListModel<E> implements ComboBoxModel<E> {
    private E[] elementos;
    private Object seleccionado;

    public ModeloLista(E[] elementos) {
        setElementos(elementos);
    }

    public void setElementos(E[] nuevos) {
        int anteriores = elementos == null ? 0 : elementos.length;
        elementos = nuevos;
        boolean sigue = false;
        for (E e : elementos) {
            sigue |= e.equals(seleccionado);
        }
        if (!sigue) {
            seleccionado = elementos.length > 0 ? elementos[0] : null;
        }
        fireContentsChanged(this, 0, Math.max(anteriores, elementos.length) - 1);
    }

    @Override
    public int getSize() {
        return elementos == null ? 0 : elementos.length;
    }

    @Override
    public E getElementAt(int indice) {
        return elementos[indice];
    }

    @Override
    public void setSelectedItem(Object item) {
        seleccionado = item;
        fireContentsChanged(this, -1, -1);
    }

    @Override
    public Object getSelectedItem() {
        return seleccionado;
    }

    public int indiceSeleccionado() {
        for (int i = 0; i < elementos.length; i++) {
            if (elementos[i].equals(seleccionado)) {
                return i;
            }
        }
        return -1;
    }
}