/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 */
public final class Parametros {
    
    private int quantum;
    private int latenciaRed;
    
    public Parametros(int quantum, int latenciaRed){
        setQuantum(quantum);
        setLatenciaRed(latenciaRed);
    }
    
    public int getQuantum(){
        return quantum;
    }
    
    public void setQuantum(int quantum){
        if (quantum<1){
            throw new IllegalArgumentException("El quantum debe ser al menos 1 ciclo");
        }
        this.quantum = quantum;
    }
    
    
    public int getLatenciaRed(){
        return latenciaRed;
    }
    
    public void setLatenciaRed(int latenciaRed){
        if (latenciaRed<0){
            throw new IllegalArgumentException("La latencia no puede ser negativa");
        }
        this.latenciaRed = latenciaRed;
    }
}
