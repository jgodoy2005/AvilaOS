/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.estructuras;

/**
 *
 * @author erick
 */
class Nodo<T> {
    T dato;
    Nodo<T> siguiente;
    
    Nodo(T dato) {
        this.dato = dato;
    }
}
