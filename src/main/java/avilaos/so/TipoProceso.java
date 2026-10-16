/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 */
public enum TipoProceso {
    CPU_BOUND("CPU bound"), IO_BOUND("I/O bound"), PRODUCTOR("Productor"), CONSUMIDOR("consumidor");
    
    private final String nombre;
    
    TipoProceso(String nombre){
        this.nombre = nombre;
    }
    
    public Boolean UsaBuffer(){
        return this == PRODUCTOR || this == CONSUMIDOR;  
    }
    
    @Override
    public String toString(){
        return nombre;
    }
}
