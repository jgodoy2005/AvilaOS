/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package avilaos.simulacion;
import avilaos.so.disco.Solicitud;

/**
 *
 * @author juandiegogodoy
* Copia de solo lectura de una solicitud de disco, para la interfaz.
 */
public record FotoSolicitud(
    String proceso,
    String operacion,
    String descripcion,
    int primerBloque,
    int cantidadBloques,
    long cicloLlegada,
    int bloquesHechos) {

    //foto de una solicitud; 'bloquesHechos' es cuántos bloques de su cadena ya visitó el disco
    public static FotoSolicitud de(Solicitud s, int bloquesHechos) {
        return new FotoSolicitud(s.etiqueta(), s.getOperacion().toString(), s.descripcion(), s.primerBloque(),
                s.bloques().length, s.cicloLlegada(), bloquesHechos);
    }
}
