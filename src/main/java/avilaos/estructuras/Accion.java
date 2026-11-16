/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.estructuras;

/**
 *
 * @author erick
 */
@FunctionalInterface
public interface Accion<T> {
    void ejecutar(T elemento);
    
}
