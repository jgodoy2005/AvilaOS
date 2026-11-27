/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.disco;

import avilaos.estructuras.BuzonCiclos;
import avilaos.estructuras.Cola;
import avilaos.estructuras.ListaEnlazada;
import avilaos.hardware.ControladorDisco;
import avilaos.hardware.DecisionDisco;
import avilaos.hardware.DireccionCabezal;
import avilaos.hardware.Disco;
import avilaos.hardware.ManejadorDisco;
import avilaos.so.PCB;
import avilaos.so.RegistroEventos;
import java.util.concurrent.Semaphore;

/**
 *
 * @author erick
 *
 * Planificador de disco (parte del sistema operativo). Tiene la única cola de
 * E/S del disco y, cada vez que el disco queda libre, le pregunta a la política
 * activa qué hacer.
 *
 * El kernel le manda solicitudes y cancelaciones por buzones con ciclo; el
 * disco, desde su hilo, las recibe al empezar cada ciclo (solo las de ciclos
 * anteriores).
 */
public class PlanificadorDisco implements ControladorDisco {

    private final Disco disco;
    private final ManejadorDisco manejador;     //kernel: recibe los avisos de cancelacion
    private final RegistroEventos registro;
    private final ListaEnlazada<Solicitud> cola = new ListaEnlazada<>();
    private final Semaphore mutex = new Semaphore(1, true);   //protege la cola de E/S del disco
    private final BuzonCiclos<Solicitud> entrada = new BuzonCiclos<>();
    private final BuzonCiclos<PCB> cancelaciones = new BuzonCiclos<>();
    private final MetricasDisco metricas = new MetricasDisco();
    private PoliticaDisco politica;

    public PlanificadorDisco(Disco disco, ManejadorDisco manejador, TipoPoliticaDisco inicial, RegistroEventos registro) {
        this.disco = disco;
        this.manejador = manejador;
        this.politica = inicial.crear();
        metricas.registrarPunto(0, disco.getDesplazamiento(), inicial);
        this.registro = registro;
    }

    //DESDE EL HILO DEL KERNEL
    //manda una solicitud a la cola del disco; el disco la ve desde el ciclo siguiente
    public void enviar(Solicitud s, long ciclo) {
        //s.marcarLlegada(ciclo);
        entrada.enviar(s, ciclo);
    }

    //pide sacar de la cola las solicitudes de un proceso (las que estan en curso se terminan)
    public void cancelar(PCB proceso, long ciclo) {
        cancelaciones.enviar(proceso, ciclo);
    }

    //DESDE EL HILO DEL DISCO
    @Override
    public void recibir(long ciclo) {
        Cola<Solicitud> nuevas = entrada.recibirAnteriores(ciclo);
        Cola<PCB> canceladas = cancelaciones.recibirAnteriores(ciclo);
        mutex.acquireUninterruptibly();
        try {
            while (!nuevas.estaVacia()) {
                Solicitud s = nuevas.desencolar();
                s.marcarLlegada(ciclo);
                cola.agregarAlFinal(s);
                registro.registrar(ciclo, "Llega a la cola del disco " + texto(s));
            }
            while (!canceladas.estaVacia()) {
                PCB p = canceladas.desencolar();
                Solicitud s;
                while ((s = cola.buscar(x -> x.getProceso() == p)) != null) {
                    cola.eliminar(s);
                    registro.registrar(ciclo, "Se saca de la cola del disco " + texto(s));
                    manejador.trabajoCancelado(s, ciclo);
                }
            }
        } finally {
            mutex.release();
        }
    }

    public DecisionDisco siguiente(int cabezal, DireccionCabezal direccion, int ultimoBloque, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (cola.estaVacia()) {
                return DecisionDisco.esperar();
            }
            DecisionDisco d = politica.decidir(cola.aArreglo(new Solicitud[cola.tamano()]), cabezal, direccion, ultimoBloque);
            switch (d.getTipo()) {
                case ATENDER -> {
                    Solicitud s = (Solicitud) d.getTrabajo();
                    cola.eliminar(s);
                    metricas.registrarAtencion(politica.getTipo(), ciclo - s.cicloLlegada());
                    String donde = s.primerBloque() < 0 ? " (sin bloques)" : " en el bloque " + s.primerBloque();
                    registro.registrar(ciclo, "El disco atiende la solicitud de " + s.etiqueta() + donde
                            + " (" + politica.getTipo() + ")");
                    if (d.getDireccion() != direccion && politica.getTipo() == TipoPoliticaDisco.SCAN) {
                        registro.registrar(ciclo, "El cabezal llegó al extremo (bloque " + cabezal + ") y cambia de dirección");
                    }
                }
                case SALTAR_AL_INICIO ->
                    registro.registrar(ciclo, "El cabezal llegó al bloque " + cabezal
                            + " y salta al 0 (C-SCAN)");
                default -> {
                }
            }
            return d;
        } finally {
            mutex.release();
        }
    }

    @Override
    public void finalizarCiclo(long ciclo) {
        metricas.registrarPunto(ciclo, disco.getDesplazamiento(),
                politica.getTipo());
    }

    //"la solicitud de P3-H1: Leer /sistema/boot.bin" (o solo la descripcion si no tiene proceso)
    private static String texto(Solicitud s) {
        return s.getProceso() != null ? "la solicitud de " + s.etiqueta() + ": " + s.descripcion() : s.descripcion();
    }

    //DESDE LA INTERFAZ (con el candado del ciclo tomado)
    public void cambiarPolitica(TipoPoliticaDisco tipo, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (tipo == politica.getTipo()) {
                return;
            }
            politica = tipo.crear();
        } finally {
            mutex.release();
        }
        registro.registrar(ciclo, "La política de disco cambia a " + tipo);
    }

    //copia de la cola en orden de llegada
    public Solicitud[] pendientes() {
        mutex.acquireUninterruptibly();
        try {
            return cola.aArreglo(new Solicitud[cola.tamano()]);
        } finally {
            mutex.release();
        }
    }

    public int cantidadPendientes() {
        mutex.acquireUninterruptibly();
        try {
            return cola.tamano() + entrada.tamano();
        } finally {
            mutex.release();
        }
    }

    public TipoPoliticaDisco getTipoPolitica() {
        return politica.getTipo();
    }

    public MetricasDisco getMetricas() {
        return metricas;
    }
}
