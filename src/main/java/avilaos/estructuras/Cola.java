/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.estructuras;

/**
 *
 * @author erick
 */
public class Cola<T> {
    
    private final ListaEnlazada<T> elementos = new ListaEnlazada<>();
    

    public void encolar(T dato) {
        elementos.agregarAlFinal(dato);
    }

    public T desencolar() {
        return elementos.eliminarPrimero();
    }

    public T verPrimero() {
        return elementos.primero();
    }


    public boolean eliminar(T dato) {
        return elementos.eliminar(dato);
    }
    
    public boolean contiene(T dato) {
        return elementos.contiene(dato);
    }
    
    public T buscar(Condicion<T> condicion) {
        return elementos.buscar(condicion);
    }
    
    public void ordenar (Comparador<T> comparador) {
        elementos.ordenar(comparador);
    }
    
    public void paraCada(Accion<T> accion){
        elementos.paraCada(accion);
    }
    
    public T[] aArreglo(T[] destino) {
        return elementos.aArreglo(destino);
    }
    
    public int tamano() {
        return elementos.tamano();
    }
    
    public boolean estaVacia() {
        return elementos.estaVacia();
    }
}
