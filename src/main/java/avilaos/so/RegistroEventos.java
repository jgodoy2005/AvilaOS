/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;
import avilaos.estructuras.Cola;
import avilaos.estructuras.ColaSincronizada;
/**
 *
 * @author juandiegogodoy
 */
public class RegistroEventos {
    private final ColaSincronizada<String> pendientes = new ColaSincronizada<>();
    private final boolean imprimirEnConsola;
    
    public RegistroEventos(boolean imprimirEnConsola) {
        this.imprimirEnConsola = imprimirEnConsola;
    }

    public void registrar(long ciclo, String mensaje) {
        String linea = "[ciclo " + ciclo + "] " + mensaje;
        pendientes.encolar(linea);
        if (imprimirEnConsola) {
            System.out.println(linea);
        }
    }
    
    //devuelve los mensajes que se guardaron y los elimina de la cola
    public Cola<String> extraerPendientes() {
        return pendientes.extraerTodos();
    }
}
