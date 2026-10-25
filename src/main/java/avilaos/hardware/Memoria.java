/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author erick
 */
public class Memoria {
    private final int capacidadMB;
    private int usadaMB;
    
    public Memoria(int capacidadMB) {
        if(capacidadMB <= 0) {
            throw new IllegalArgumentException("La RAM debe ser mayor que 0 MB");
        }
        this.capacidadMB = capacidadMB;
    }
    
    
    public boolean reservar(int mb) {
        if (usadaMB + mb > capacidadMB) {
            return false;
        }
        usadaMB += mb;
        return true;
    }
    
    public void liberar (int mb) {
        if (mb > usadaMB) {
            throw new IllegalStateException("Se intento liberar " + mb + " MB "
                    + "pero solo hay " + usadaMB + " usados");
        }
        usadaMB -= mb;
    }
    
    public int getCapacidadMB() {
        return capacidadMB;
    }
    
    public int getUsadaMB() {
        return usadaMB;
    }
    
    public int getLibreMB() {
        return capacidadMB - usadaMB;
    }
}


