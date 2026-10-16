/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 */
public enum MotivoBloqueo {
    ES("Esperando E/S"),
    BUFFER_LLENO("Buffer lleno: semWait(e)"),
    BUFFER_VACIO("Buffer vacio: semWait(n)"),
    REGION_CRITICA("Region critica ocupada: semWait(s)"),
    RED("Latencia de red");
    
    private final String descripcion;
    
    MotivoBloqueo(String descripcion){
        this.descripcion = descripcion;
    }
    
    public boolean esDeSemaforo() {
        return this == BUFFER_LLENO || this == BUFFER_VACIO || this == REGION_CRITICA;
    }
    
    
    @Override
    public String toString(){
        return descripcion;
    }
    
}
