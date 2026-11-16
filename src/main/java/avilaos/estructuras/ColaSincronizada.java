/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.estructuras;
import java.util.concurrent.Semaphore;
/**
 *
 * @author erick
 */
public class ColaSincronizada<T> {
    
    private Cola<T> cola = new Cola<>();
    private final Semaphore mutex = new Semaphore(1, true);
    
    public void encolar(T dato) {
        mutex.acquireUninterruptibly();
        try {
            cola.encolar(dato);
        } finally {
            mutex.release();
        }
    }
    
    public T desencolar() {
        mutex.acquireUninterruptibly();
        try {
            return cola.desencolar();
        } finally {
            mutex.release();
        }
    }
    
    public Cola<T> extraerTodos() {
        mutex.acquireUninterruptibly();
        try{
            Cola<T> todos = cola;
            cola = new Cola<>();
            return todos;
        }finally {
            mutex.release();
        }
    }
    
    public int tamano() {
        mutex.acquireUninterruptibly();
        try {
            return cola.tamano();
        } finally {
            mutex.release();
        }
    }
}
