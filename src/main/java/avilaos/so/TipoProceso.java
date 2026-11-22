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
    CPU_BOUND("CPU bound"), IO_BOUND("I/O bound"), PRODUCTOR("Productor"), CONSUMIDOR("Consumidor"),
    ES_DISCO("E/S de disco");
    
    private final String nombre;
    
    TipoProceso(String nombre){
        this.nombre = nombre;
    }
    
    //tipos que se pueden crear desde el dialogo de procesos: los de disco salen del JTree y
    //productor/consumidor quedan desactivados en el Proyecto 2
    public static TipoProceso[] disponiblesEnDialogo(){
        return new TipoProceso[] {CPU_BOUND, IO_BOUND};
    }
    
    public Boolean usaBuffer(){
        return this == PRODUCTOR || this == CONSUMIDOR;  
    }
    
    @Override
    public String toString(){
        return nombre;
    }
}
