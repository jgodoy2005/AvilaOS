/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 */
public enum MotivoTerminacion {
    COMPLETADO("Completado"),
    DEADLINE("Deadline vencido"),
    INALCANZABLE("Deadline inalcanzable");
    
    private final String descripcion;
    
    MotivoTerminacion(String descripcion){
    this.descripcion = descripcion;
    }
    
    @Override
    public String toString(){
        return descripcion;
    }
    
    
}
