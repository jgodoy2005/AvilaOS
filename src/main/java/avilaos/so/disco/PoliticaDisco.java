/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.so.disco;

import avilaos.hardware.DecisionDisco;
import avilaos.hardware.DireccionCabezal;

/**
 *
 * @author juandiegogodoy
 * Contrato de toda política de planificación de disco. El planificador de disco solo usa
 * esta interfaz, así que agregar una política nueva no obliga a modificarlo.
 */
public interface PoliticaDisco {
    
    TipoPoliticaDisco getTipo();

    /**
     * Decide qué hace el disco ahora que está libre.
     * @param pendientes solicitudes en cola, en orden de llegada (nunca vacío)
     * @param cabezal posición actual del cabezal
     * @param direccion dirección actual del barrido
     * @param ultimoBloque último bloque del disco (incluye la zona de swap)
     */
    DecisionDisco decidir(Solicitud[] pendientes, int cabezal, DireccionCabezal direccion, int ultimoBloque);

    //posicion que usan las politicas para una solicitud: su primer bloque, o el cabezal si no toca bloques
    static int posicion(Solicitud s, int cabezal) {
        int p = s.primerBloque();
        return p < 0 ? cabezal : p;
    }
}
