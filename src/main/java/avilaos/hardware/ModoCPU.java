/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
 */
public enum ModoCPU {
    
     USUARIO("Programa de usuario"),
     SISTEMA_OPERATIVO("Sistema operativo");

     private final String descripcion;
    
     ModoCPU(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
     
}
