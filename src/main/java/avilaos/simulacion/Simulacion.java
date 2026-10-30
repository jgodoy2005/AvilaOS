/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.simulacion;
import avilaos.cluster.Cluster;
import avilaos.cluster.Computador;
import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.ConfiguracionSimulacion.ConfigBuffer;
import avilaos.config.ConfiguracionSimulacion.ConfigComputador;
import avilaos.estructuras.Cola;
import avilaos.hardware.ObservadorReloj;
import avilaos.hardware.Reloj;
import avilaos.so.DatosProceso;
import avilaos.so.GeneradorId;
import avilaos.so.Kernel;
import avilaos.so.PCB;
import avilaos.so.Parametros;
import avilaos.so.RegistroEventos;
import avilaos.so.planificacion.TipoPolitica;
import avilaos.so.sincronizacion.BufferAcotado;
import avilaos.so.sincronizacion.SemaforoSO;
import avilaos.simulacion.EstrategiaAsignacion.CargaComputador;

/**
 *
 * @author juandiegogodoy
 */


public final class Simulacion implements ObservadorReloj {
    
    //lo necesario para armar la simulacion, parametros, registros, cluster de computadores, el reloj global 
    //y la estrategia para asignar procesos a computadores
    private final Parametros parametros;
    private final RegistroEventos registro;
    private final Cluster cluster;
    private final Reloj reloj;
    private final EstrategiaAsignacion asignacion = new AsignacionMenorCarga();

    private FotoSimulacion ultimaFoto;          // se usa con el candado del ciclo tomado
    private ObservadorSimulacion observador;    
    
    
    
    public Simulacion(ConfiguracionSimulacion config, boolean registroEnConsola) {
        config.validar();
        GeneradorId.reiniciar();
        parametros = new Parametros(config.quantum(), config.latenciaRed());
        registro = new RegistroEventos(registroEnConsola);
        cluster = new Cluster(parametros, registro);
        reloj = new Reloj(config.duracionCicloMs());

        for (ConfigComputador cc : config.computadores()) {
            Computador c = cluster.agregarComputador(cc.ramMB(), cc.politica());
            reloj.registrar(c, "Hilo-Computador-" + c.getNumero());  
        }
        reloj.setObservador(this);

        if (config.buffers() != null) {
            for (ConfigBuffer cb : config.buffers()) {
                cluster.crearBuffer(cb.capacidad(), cb.anfitrion(), 0);
            }
        }
        if (config.procesos() != null) {
            for (DatosProceso datos : config.procesos()) {
                crearProceso(datos);
            }
        }
        ultimaFoto = construirFoto(0);
    }

     //CONTROLAR EL RELOJ GLOBAL
    
     
    
    public void iniciar() {    //crea los hilos del reloj y de cada computador; arranca en pausa
        reloj.iniciar();
    }

    public void reanudar() {   //reanuda la marcha del reloj tras una pausa
        reloj.reanudar();
    }

    public void pausar() {     //congela el reloj y su paso
        reloj.pausar();
    }

    public void avanzarUnCiclo() {    //avanza manualmente un solo ciclo de reloj
        reloj.avanzarUnCiclo();
    }

    public void detener() {        //apaga o termina la simulacion
        reloj.detener();
    }

    //ejecuta n ciclos seguidos en un mismo hilo
    public void ejecutarCiclosSinHilos(int n) {
        reloj.ejecutarCiclosSinHilos(n);
    }

    
    //consulta si la simulacion esta en pausa
    public boolean isPausada() {
        return reloj.isPausado();
    }
    
    
    
    //cambios que se pueden pedir por interfaz
   
    //modificar la velocidad de cada ciclo de reloj
    public void setDuracionCicloMs(int ms) {
        reloj.setDuracionCicloMs(ms);  
    }

    //modifica el quantum de tiempo de cada turno de round robin
    public void setQuantum(int quantum) {
        reloj.tomarCandadoCiclo();
        try {
            parametros.setQuantum(quantum);
            registro.registrar(reloj.getCiclo(), "El quantum de Round Robin cambia a " + quantum + " ciclos");
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //modifica la latencia de red para operaciones entre computadores diferentes del cluster
    public void setLatenciaRed(int latencia) {
        reloj.tomarCandadoCiclo();
        try {
            parametros.setLatenciaRed(latencia);
            registro.registrar(reloj.getCiclo(), "La latencia de red cambia a " + latencia + " ciclos");
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //cambia la politica de planificacion de un computador 
    public void cambiarPolitica(int numeroComputador, TipoPolitica politica) {
        reloj.tomarCandadoCiclo();
        try {
            cluster.getComputador(numeroComputador).getKernel().cambiarPolitica(politica);
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }
    
    
    //creacion de un proceso, la validacoion de sus datos y la asignacion a un computador del cluster
    public String crearProceso(DatosProceso datos) {
        datos.validar();
        reloj.tomarCandadoCiclo();   // si hay un ciclo corriendo, espera a que termine
        try {
            boolean automatico = datos.computador() == 0;
            int numero = automatico ? elegirComputador(datos.memoriaMB()) : datos.computador();
            if (numero < 1) {
                throw new IllegalArgumentException("Ningún computador tiene RAM suficiente para "
                        + datos.memoriaMB() + " MB");
            }
            Computador destino = cluster.getComputador(numero);
            int ram = destino.getMemoria().getCapacidadMB();
            if (datos.memoriaMB() > ram) {
                throw new IllegalArgumentException("El proceso pide " + datos.memoriaMB() + " MB y el "
                        + destino.getNombre() + " solo tiene " + ram + " MB de RAM en total");
            }
            BufferAcotado buffer = null;
            if (datos.tipo().usaBuffer()) {
                buffer = cluster.buscarBuffer(datos.buffer());
                if (buffer == null) {
                    throw new IllegalArgumentException("No existe el buffer " + datos.buffer());
                }
            }
            Kernel kernel = destino.getKernel();
            PCB pcb = new PCB(GeneradorId.nuevoId(), datos, kernel, buffer);
            registro.registrar(reloj.getCiclo(), "Se crea " + pcb.etiqueta() + " (" + datos.nombre() + ", "
                    + datos.tipo() + ") para el " + destino.getNombre()
                    + (automatico ? " por asignación automática" : ""));
            kernel.recibirProceso(pcb);
            return pcb.etiqueta() + " en el " + destino.getNombre();
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }
    
    // crea un buffer
    public String crearBuffer(int capacidad, int numeroAnfitrion) {
        reloj.tomarCandadoCiclo();
        try {
            return cluster.crearBuffer(capacidad, numeroAnfitrion, reloj.getCiclo()).getNombre();
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    
    //revisa la memoria y carga de cada computador para ver a quien asignar el proceso
    private int elegirComputador(int memoriaMB) {
        Computador[] computadores = cluster.getComputadores();
        CargaComputador[] cargas = new CargaComputador[computadores.length];
        for (int i = 0; i < computadores.length; i++) {
            Computador c = computadores[i];
            cargas[i] = new CargaComputador(c.getNumero(), c.getKernel().procesosActivos(),
                    c.getMemoria().getCapacidadMB(), c.getMemoria().getLibreMB());
        }
        return asignacion.elegir(cargas, memoriaMB);
    }

    //FIN DE CADA CICLO, TOMAR LAS FOTOS PARA LA INTERFAZ
    
    //metodo para avisar que un ciclo de reloj termino, llamar a la interfaz y tomar una foto para la interfaz
    @Override
    public void cicloTerminado(long ciclo) {
        FotoSimulacion foto;
        ObservadorSimulacion o;
        reloj.tomarCandadoCiclo();
        try {
            foto = construirFoto(ciclo);
            ultimaFoto = foto;
            o = observador;
        } finally {
            reloj.soltarCandadoCiclo();
        }
        if (o != null) {
            o.estadoActualizado(foto);
        }
    }
    
    //recompila los datos de computadores, buffers y metricas para una foto global
    private FotoSimulacion construirFoto(long ciclo) {
        Computador[] computadores = cluster.getComputadores();
        FotoComputador[] fotos = new FotoComputador[computadores.length];
        FotoMetricas[] metricas = new FotoMetricas[computadores.length];
        for (int i = 0; i < computadores.length; i++) {
            fotos[i] = fotografiar(computadores[i], ciclo);
            metricas[i] = fotos[i].metricas();
        }
        BufferAcotado[] buffers = cluster.getBuffers();
        FotoBuffer[] fotosBuffers = new FotoBuffer[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            fotosBuffers[i] = fotografiar(buffers[i], fotos);
        }
        return new FotoSimulacion(ciclo, reloj.isPausado(), fotos, fotosBuffers, FotoMetricas.combinar(metricas));
    }

    
    //extrae la foto individual de un computador
    private FotoComputador fotografiar(Computador c, long ciclo) {
        Kernel k = c.getKernel();
        PCB enEjecucion = k.getEnEjecucion();
        return new FotoComputador(
                c.getNumero(),
                c.getNombre(),
                k.getPolitica(),
                enEjecucion == null ? null : FotoPCB.de(enEjecucion, ciclo),
                c.getCpu().isOciosa(),
                c.getCpu().getModo(),
                c.getCpu().getDescripcion(),
                c.getMemoria().getUsadaMB(),
                c.getMemoria().getCapacidadMB(),
                c.getDispositivo().describir(),
                fotografiar(k.getNuevos(), ciclo),
                fotografiar(k.getListos(), ciclo),
                fotografiar(k.getBloqueados(), ciclo),
                fotografiar(k.getTerminados(), ciclo),
                FotoMetricas.de(k.getMetricas()));
    }

    //convierte una lista de procesos (los PCB de cada uno), a fotos de solo lectura
    private static FotoPCB[] fotografiar(PCB[] procesos, long ciclo) {
        FotoPCB[] fotos = new FotoPCB[procesos.length];
        for (int i = 0; i < procesos.length; i++) {
            fotos[i] = FotoPCB.de(procesos[i], ciclo);
        }
        return fotos;
    }

    //calcula el estado del buffer y el tiempo promedio de espera en sus semaforos
    private FotoBuffer fotografiar(BufferAcotado b, FotoComputador[] computadores) {
        int procesos = 0;
        long bloqueo = 0;
        for (FotoComputador c : computadores) {
            FotoPCB[][] grupos = {c.nuevos(), c.listos(), c.bloqueados(), c.terminados(),
                c.enEjecucion() == null ? new FotoPCB[0] : new FotoPCB[] {c.enEjecucion()}};
            for (FotoPCB[] grupo : grupos) {
                for (FotoPCB p : grupo) {
                    if (p.buffer().equals(b.getNombre())) {
                        procesos++;
                        bloqueo += p.ciclosBloqueadoSemaforo();
                    }
                }
            }
        }
        SemaforoSO s = b.getMutex(), n = b.getLlenos(), e = b.getVacios();
        return new FotoBuffer(b.getNombre(), b.getNumeroAnfitrion(), b.getCapacidad(), b.getCantidad(),
                b.contenido(), s.getValor(), n.getValor(), e.getValor(),
                s.procesosEnEspera(), n.procesosEnEspera(), e.procesosEnEspera(),
                b.getProducidos(), b.getConsumidos(), procesos,
                procesos == 0 ? Double.NaN : (double) bloqueo / procesos);
    }
    
    
    //PARA LA INTERFAZ
    
    //registra la interfaz grafica que recibira las fotos del estado
    public void setObservador(ObservadorSimulacion observador) {
        reloj.tomarCandadoCiclo();
        try {
            this.observador = observador;
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //obtiene la ultima foto capturada del sistema
    public FotoSimulacion getUltimaFoto() {
        reloj.tomarCandadoCiclo();
        try {
            return ultimaFoto;
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //extrae los eventos pendientes del historial
    public Cola<String> extraerEventos() {
        return registro.extraerPendientes();   // el registro tiene su propia cola sincronizada
    }


    public long getCiclo() {
        return reloj.getCiclo();
    }

    public int getDuracionCicloMs() {
        return reloj.getDuracionCicloMs();
    }

    public int getQuantum() {
        return parametros.getQuantum();
    }

    public int getLatenciaRed() {
        return parametros.getLatenciaRed();
    }

    public int cantidadComputadores() {
        return cluster.cantidadComputadores();
    }
}
