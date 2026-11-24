package avilaos.gui;

import avilaos.simulacion.FotoNodo;
import avilaos.so.archivos.TipoNodo;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Modelo del JTree hecho directamente sobre la foto del sistema de archivos (FotoNodo): cada
 * nodo del árbol es un FotoNodo y sus hijos son los de la foto. No usa colecciones de Java.
 * Es pública para que las pruebas (paquete pruebas) puedan usarla.
 */
public final class ModeloArbol implements TreeModel {

    private FotoNodo raiz;
    private final EventListenerList oyentes = new EventListenerList();

    //cambia la foto completa y avisa al JTree que todo el arbol cambio
    public void setRaiz(FotoNodo nueva) {
        raiz = nueva;
        TreeModelEvent evento = new TreeModelEvent(this, new Object[] {nueva});
        for (TreeModelListener o : oyentes.getListeners(TreeModelListener.class)) {
            o.treeStructureChanged(evento);
        }
    }

    //camino desde la raiz hasta el nodo con esa ruta (null si ya no existe)
    public TreePath camino(String ruta) {
        if (raiz == null || ruta == null) {
            return null;
        }
        TreePath camino = new TreePath(raiz);
        FotoNodo actual = raiz;
        while (!actual.ruta().equals(ruta)) {
            FotoNodo siguiente = null;
            for (FotoNodo h : actual.hijos()) {
                if (ruta.equals(h.ruta()) || ruta.startsWith(h.ruta() + "/")) {
                    siguiente = h;
                    break;
                }
            }
            if (siguiente == null) {
                return null;
            }
            actual = siguiente;
            camino = camino.pathByAddingChild(actual);
        }
        return camino;
    }

    @Override
    public Object getRoot() {
        return raiz;
    }

    @Override
    public Object getChild(Object padre, int indice) {
        return ((FotoNodo) padre).hijos()[indice];
    }

    @Override
    public int getChildCount(Object padre) {
        return ((FotoNodo) padre).hijos().length;
    }

    @Override
    public boolean isLeaf(Object nodo) {
        return ((FotoNodo) nodo).tipo() == TipoNodo.ARCHIVO;   //un directorio vacio se ve como carpeta
    }

    @Override
    public int getIndexOfChild(Object padre, Object hijo) {
        FotoNodo[] hijos = ((FotoNodo) padre).hijos();
        for (int i = 0; i < hijos.length; i++) {
            if (hijos[i] == hijo) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void valueForPathChanged(TreePath camino, Object valor) {
        //el arbol no se edita desde la interfaz: los cambios los hacen los procesos
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        oyentes.add(TreeModelListener.class, l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        oyentes.remove(TreeModelListener.class, l);
    }
}