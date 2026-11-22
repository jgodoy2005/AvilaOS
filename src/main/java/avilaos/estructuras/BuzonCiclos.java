/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.estructuras;

import java.util.concurrent.Semaphore;
/**
 *
 * @author juandiegogodoy
 * Buzón protegido con semáforo para comunicar componentes que corren en hilos distintos.
 * Cada mensaje lleva el ciclo en que se envió, y quien recibe solo toma los mensajes de
 * ciclos anteriores al actual (regla del ciclo anterior). Así el resultado no depende de
 * qué hilo termina primero dentro de un mismo ciclo.
 */
public class BuzonCiclos<T> {
    //un mensaje y el ciclo en que se envio
    private static final class Mensaje<T> {
        final T dato;
        final long ciclo;

        Mensaje(T dato, long ciclo) {
            this.dato = dato;
            this.ciclo = ciclo;
        }
    }

    private final ListaEnlazada<Mensaje<T>> mensajes = new ListaEnlazada<>();
    private final Semaphore mutex = new Semaphore(1, true);

    //deja un mensaje marcado con el ciclo actual de quien lo envia
    public void enviar(T dato, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            mensajes.agregarAlFinal(new Mensaje<>(dato, ciclo));
        } finally {
            mutex.release();
        }
    }

    //saca, en orden de llegada, los mensajes enviados antes del ciclo indicado; los demas se quedan
    public Cola<T> recibirAnteriores(long cicloActual) {
        Cola<T> salida = new Cola<>();
        mutex.acquireUninterruptibly();
        try {
            ListaEnlazada<Mensaje<T>> quedan = new ListaEnlazada<>();
            Mensaje<T> m;
            while ((m = mensajes.eliminarPrimero()) != null) {
                if (m.ciclo < cicloActual) {
                    salida.encolar(m.dato);
                } else {
                    quedan.agregarAlFinal(m);
                }
            }
            quedan.paraCada(mensajes::agregarAlFinal);
        } finally {
            mutex.release();
        }
        return salida;
    }

    //cantidad de mensajes que esperan (de cualquier ciclo)
    public int tamano() {
        mutex.acquireUninterruptibly();
        try {
            return mensajes.tamano();
        } finally {
            mutex.release();
        }
    }
}
