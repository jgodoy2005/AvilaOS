/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author erick
 */
/**
 * Un bloque del disco simulado. Su número es su posición. Guarda quién lo ocupa
 * (un archivo o un proceso suspendido), el color con que se pinta y cuál es el
 * siguiente bloque de su cadena (asignación encadenada). El contenido no se simula.
 */
public final class Bloque {

    public static final int FIN = -1;   //marca de fin de cadena

    private final int numero;
    private final boolean zonaSwap;   //si pertenece a la zona de intercambio
    private String ocupante;          //nombre del archivo o proceso; null si esta libre
    private int color = -1;           //indice de color del ocupante; -1 si no tiene
    private int siguiente = FIN;      //siguiente bloque de la cadena

    Bloque(int numero, boolean zonaSwap) {
        this.numero = numero;
        this.zonaSwap = zonaSwap;
    }

    void ocupar(String ocupante, int color, int siguiente) {
        this.ocupante = ocupante;
        this.color = color;
        this.siguiente = siguiente;
    }

    void liberar() {
        ocupante = null;
        color = -1;
        siguiente = FIN;
    }

    public int getNumero() { return numero; }
    public boolean isZonaSwap() { return zonaSwap; }
    public boolean isLibre() { return ocupante == null; }
    public String getOcupante() { return ocupante; }
    public int getColor() { return color; }
    public int getSiguiente() { return siguiente; }
}

