/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.sincronizacion;
import avilaos.so.MotivoBloqueo;
import avilaos.so.PCB;
import avilaos.so.TipoInstruccion;

/**
 *
 * @author juandiegogodoy
 */


/*
ESQUELETO PARA LOS BUFFERS, referenciarlos y si alguien lo pide, devolver excepcion de que aun no esta listo
*/
public class BufferAcotado {
    public final String nombre;
    public BufferAcotado(String nombre, int capacidad, int numeroAnfitrion) { this.nombre = nombre; }
    public String getNombre() { return nombre; }
    public int getNumeroAnfitrion() { return 0; }
    public int getCapacidad() { return 0; }
    public int getCantidad() { return 0; }
    public MotivoBloqueo ejecutarOperacion(PCB proceso, TipoInstruccion operacion) {
        throw new UnsupportedOperationException("El buffer todavía no está implementado");
    }
    public String liberarRecursos(PCB proceso) { return ""; }
}
