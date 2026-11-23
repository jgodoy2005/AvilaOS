/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;

import avilaos.estructuras.Comparador;
import avilaos.estructuras.ListaEnlazada;

/**
 *
 * @author juandiegogodoy
 */

// Si un proceso nuevo no cabe en RAM, el planificador elige qué procesos bloqueados por E/S suspender.
// Prioriza los de menor prioridad (número mayor) y, en caso de empate, los que llevan más tiempo bloqueados.
// Solo propone una selección si cabe en el swap y libera suficiente RAM. El kernel aplica la decisión.

public class PlanificadorMedianoPlazo {
    //orden de eleccion: prioridad mas baja (numero mayor) primero; empate: el que se bloqueo antes
    static final Comparador<PCB> CRITERIO = (a, b) -> a.getPrioridad() != b.getPrioridad()
            ? Integer.compare(b.getPrioridad(), a.getPrioridad())
            : Long.compare(a.getCicloBloqueo(), b.getCicloBloqueo());

    //si un proceso se puede suspender
    static boolean esCandidato(PCB p) {
        return p.getEstado() == EstadoProceso.BLOQUEADO
                && (p.getMotivoBloqueo() == MotivoBloqueo.ES || p.getMotivoBloqueo() == MotivoBloqueo.ES_DISCO)
                && p.isMemoriaAsignada()
                && p.getTerminacionPendiente() == null;
    }

    //Devuelve las víctimas en orden de preferencia. Devuelve null si ninguna combinación alcanza.
    public PCB[] elegirVictimas(PCB[] bloqueados, int falta, int swapLibre) {
        if (falta <= 0) {
            return new PCB[0];
        }
        if (falta > swapLibre) {
            return null;
        }

        ListaEnlazada<PCB> posibles = new ListaEnlazada<>();
        for (PCB p : bloqueados) {
            if (esCandidato(p) && p.getMemoriaMB() <= swapLibre) {
                posibles.agregarAlFinal(p);
            }
        }
        posibles.ordenar(CRITERIO);
        PCB[] candidatos = posibles.aArreglo(new PCB[posibles.tamano()]);
        int[] retenidosPorTamano = new int[swapLibre + 1];
        int cantidad = 0;
        for (PCB p : candidatos) {
            int memoria = p.getMemoriaMB();
            //De un mismo tamaño, solo pueden entrar los primeros que cabrían juntos.
            if (retenidosPorTamano[memoria] < swapLibre / memoria) {
                candidatos[cantidad++] = p;
                retenidosPorTamano[memoria]++;
            }
        }

        //liberable[i][espacio] indica cuánta RAM se puede liberar con los candidatos i en adelante.
        int[][] liberable = new int[cantidad + 1][swapLibre + 1];
        for (int i = cantidad - 1; i >= 0; i--) {
            int memoria = candidatos[i].getMemoriaMB();
            for (int espacio = 0; espacio <= swapLibre; espacio++) {
                int sinVictima = liberable[i + 1][espacio];
                liberable[i][espacio] = memoria <= espacio
                        ? Math.max(sinVictima, memoria + liberable[i + 1][espacio - memoria])
                        : sinVictima;
            }
        }

        if (liberable[0][swapLibre] < falta) {
            return null;
        }

        ListaEnlazada<PCB> elegidas = new ListaEnlazada<>();
        for (int i = 0; i < cantidad && falta > 0; i++) {
            PCB p = candidatos[i];
            int memoria = p.getMemoriaMB();
            //Conserva la preferencia si incluir este proceso todavía permite completar la selección.
            if (memoria <= swapLibre && memoria + liberable[i + 1][swapLibre - memoria] >= falta) {
                elegidas.agregarAlFinal(p);
                falta -= memoria;
                swapLibre -= memoria;
            }
        }
        return elegidas.aArreglo(new PCB[elegidas.tamano()]);
    }
}
