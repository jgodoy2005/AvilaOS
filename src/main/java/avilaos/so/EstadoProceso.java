/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 */
public enum EstadoProceso {
    NUEVO("Nuevo"),
    LISTO("Listo"),
    EJECUCION("Ejecucion"),
    BLOQUEADO("Bloqueado"),
    TERMINADO("Terminado");
            
    private final String nombre;
    
    EstadoProceso(String nombre){
        this.nombre = nombre;
    }
    
    @Override
    public String toString(){
        return nombre;
    }
    
}
    
    
    
    
    

