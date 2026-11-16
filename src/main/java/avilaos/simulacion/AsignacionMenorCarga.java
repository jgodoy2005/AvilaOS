/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.simulacion;

/**
 *
 * @author juandiegogodoy
 */
public class AsignacionMenorCarga implements EstrategiaAsignacion{
    
    //selecciona el computador con menor cantidad de procesos activos, si empatan al que tiene mas memoria libre
    //si el proceso no puede entrar a ningun computador (ninguno tiene ram suficiente), devuelve -1
    @Override
    public int elegir(CargaComputador[] computadores, int memoriaMB) {
        int elegido = -1;
        int menorCarga = Integer.MAX_VALUE;
        int mayorLibre = -1;
        for (CargaComputador c : computadores) {
            if (c.memoriaTotalMB() < memoriaMB) {
                continue;
            }
            boolean mejor = c.procesosActivos() < menorCarga
                    || (c.procesosActivos() == menorCarga && c.memoriaLibreMB() > mayorLibre);
            if (mejor) {
                elegido = c.numero();
                menorCarga = c.procesosActivos();
                mayorLibre = c.memoriaLibreMB();
            }
        }
        return elegido;
    }
}
