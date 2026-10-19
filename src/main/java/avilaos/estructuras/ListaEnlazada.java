/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.estructuras;

/**
 *
 * @author erick
 */
public class ListaEnlazada<T> {
    
    private Nodo<T> cabeza;
    private Nodo<T> ultimo;
    private int tamano;
    
    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            ultimo.siguiente = nuevo;
        }
        ultimo = nuevo;
        tamano++;
    }
    
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.siguiente = cabeza;
        cabeza = nuevo;
        if (ultimo == null){
            ultimo = nuevo;
        }
        tamano++;
    }
    
    public T primero(){
        return cabeza == null ? null : cabeza.dato;
    }
    
    public T eliminarPrimero() {
        if (cabeza == null) {
            return null;
    }
    T dato = cabeza.dato;
    cabeza = cabeza.siguiente;
    if (cabeza == null) {
        ultimo = null;
    }
    tamano--;
    return dato;
    }
    
    public boolean eliminar(T dato) {
        Nodo<T> anterior = null;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (actual.dato == dato) {
                if (anterior == null) {
                    cabeza = actual.siguiente; // era el primero
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                if (actual == ultimo) {
                    ultimo = anterior; // era el último
                }
                tamano--;
                return true;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return false;   
    }
    
    public T buscar(Condicion<T> condicion) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (condicion.cumple(n.dato)) {
                return n.dato;
            }
        }
        return null;
    }
    
    public boolean contiene(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (n.dato == dato) {
                return true;
            }
        }
        return false;
    }

    public T obtener (int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException ("Indice " + indice + " fuera de 0.." + (tamano - 1));
        }
        Nodo<T> n = cabeza;
        for (int i = 0; i < indice; i++) {
            n = n.siguiente;
        }
        return n.dato;
    }
    
    public void insertarOrdenado(T dato, Comparador<T> comparador) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null || comparador.comparar(dato, cabeza.dato) < 0) {
            nuevo.siguiente = cabeza;
            cabeza = nuevo;
            if (ultimo == null){
                ultimo = nuevo;
            }
        }else {
            Nodo<T> actual = cabeza;
            while (actual.siguiente != null && comparador.comparar(dato, actual.siguiente.dato) >= 0) {
                actual = actual.siguiente;
            }
            nuevo.siguiente = actual.siguiente;
            actual.siguiente = nuevo;
            if (nuevo.siguiente == null) {
                ultimo = nuevo;
            }
        }
        tamano++;
    }
    
    public void ordenar (Comparador<T> comparador) {
        Nodo<T> pendiente = cabeza;
        cabeza = null;
        ultimo = null;
        tamano = 0;
        while (pendiente != null) {
            insertarOrdenado(pendiente.dato, comparador);
            pendiente = pendiente.siguiente;
        }
    }
    
    public void paraCada(Accion<T> accion){
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            accion.ejecutar(n.dato);
        }
    }
/**
* Copia los elementos en el arreglo recibido, que debe tener al menos
* {@link #tamano()} posiciones, y lo devuelve. Uso típico:
* {@code PCB[] copia = lista.aArreglo(new PCB[lista.tamano()]);}
*
* Recorrer la copia permite eliminar elementos de la lista original sin
* romper el recorrido.
*/

    public T[] aArreglo(T[] destino) {
        if (destino.length < tamano) {
            throw new IllegalArgumentException(" El arreglo necesita " + tamano + "posiciones");
        }
        int i = 0;
        for (Nodo<T> n = cabeza; n !=null; n = n.siguiente) {
            destino[i++] = n.dato;
        }
        return destino;
    } 
    
    public int tamano() {
        return tamano;
    }
    
    public boolean estaVacia() {
        return tamano == 0;
    }
    
    public void vaciar() {
        cabeza = null;
        ultimo = null;
        tamano = 0;
    }
}    
