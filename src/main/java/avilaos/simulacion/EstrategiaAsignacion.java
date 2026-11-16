/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.simulacion;

/**
 *
 * @author juandiegogodoy
 */
public interface EstrategiaAsignacion {
    //el numero del computador seleccionado
    int elegir(CargaComputador[] computadores, int memoriaMB);

    //lo que se sabe de cada computador al elegir
    record CargaComputador(int numero, int procesosActivos, int memoriaTotalMB, int memoriaLibreMB) {
    }
}
