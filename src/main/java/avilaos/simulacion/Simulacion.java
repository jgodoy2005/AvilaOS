/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.simulacion;

import avilaos.Limites;
import avilaos.cluster.Cluster;
import avilaos.cluster.Computador;
import avilaos.config.ArchivoSistemaArchivos;
import avilaos.config.ConfiguracionDisco;
import avilaos.config.DatosSistemaArchivos;
import avilaos.hardware.DireccionCabezal;
import avilaos.hardware.Disco;
import avilaos.hardware.TrabajoDisco;
import avilaos.so.TipoOperacionES;
import avilaos.so.archivos.EntradaFS;
import avilaos.so.archivos.SistemaArchivos;
import avilaos.so.disco.AreaIntercambio;
import avilaos.so.disco.MetricasDisco;
import avilaos.so.disco.PlanificadorDisco;
import avilaos.so.disco.Solicitud;
import avilaos.so.disco.TipoPoliticaDisco;
import java.io.IOException;
import java.nio.file.Path;
import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.ConfiguracionSimulacion.ConfigBuffer;
import avilaos.config.ConfiguracionSimulacion.ConfigComputador;
import avilaos.estructuras.Cola;
import avilaos.hardware.ObservadorReloj;
import avilaos.hardware.Reloj;
import avilaos.so.DatosProceso;
//import avilaos.so.GeneradorId;
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
    private int siguienteId = 1;
    private final EstrategiaAsignacion asignacion = new AsignacionMenorCarga();
    private final Disco disco;                      // disco local del unico computador
    private final PlanificadorDisco planificadorDisco;  // cola de E/S del disco y su politica
    private final SistemaArchivos sistemaArchivos;      // arbol de archivos sobre el disco

    private FotoSimulacion ultimaFoto;          // se usa con el candado del ciclo tomado
    private ObservadorSimulacion observador;

    public Simulacion(ConfiguracionSimulacion config, boolean registroEnConsola) {
        this(config, ConfiguracionDisco.porDefecto(), new EntradaFS[0], registroEnConsola);
    }

    public Simulacion(ConfiguracionSimulacion config, ConfiguracionDisco configDisco, boolean registroEnConsola) {
        this(config, configDisco, new EntradaFS[0], registroEnConsola);
    }

    //'archivos' es el contenido del JSON del sistema de archivos (o el estado guardado de otra ejecucion)
    public Simulacion(ConfiguracionSimulacion config, ConfiguracionDisco configDisco, EntradaFS[] archivos,
            boolean registroEnConsola) {
        config.validar();
        configDisco.validar();
        parametros = new Parametros(config.quantum(), config.latenciaRed());
        registro = new RegistroEventos(registroEnConsola);
        cluster = new Cluster(parametros, registro);
        reloj = new Reloj(config.duracionCicloMs());

        for (ConfigComputador cc : config.computadores()) {
            Computador c = cluster.agregarComputador(cc.ramMB(), cc.politica());
            reloj.registrar(c, "Hilo-Computador-" + c.getNumero());
        }
        //el disco local con su planificador y el sistema de archivos; el kernel del computador recibe
        //los avisos del disco. El disco se registra en el reloj con su propio hilo
        Computador computador = cluster.getComputador(1);
        Kernel kernel = computador.getKernel();
        disco = new Disco(configDisco.bloquesArchivos(), configDisco.bloquesSwap(), configDisco.cabezalInicial(),
                configDisco.direccionInicial(), configDisco.velocidad());
        planificadorDisco = new PlanificadorDisco(disco, kernel, configDisco.politicaInicial(), registro);
        sistemaArchivos = new SistemaArchivos(disco, configDisco.usuarios(), registro);
        sistemaArchivos.cargar(archivos);
        disco.conectar(planificadorDisco, kernel);
        kernel.conectarDisco(sistemaArchivos, planificadorDisco, new AreaIntercambio(disco));
        computador.conectarDisco(disco);
        reloj.registrar(disco, "Hilo-Disco");
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
            PCB pcb = new PCB(siguienteId++, datos, kernel, buffer);
            registro.registrar(reloj.getCiclo(), "Se crea " + pcb.etiqueta() + " (" + datos.nombre() + ", "
                    + datos.tipo() + ") para el " + destino.getNombre()
                    + (automatico ? " por asignación automática" : ""));
            kernel.recibirProceso(pcb);
            return pcb.etiqueta() + " en el " + destino.getNombre();
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //DISCO Y SISTEMA DE ARCHIVOS (desde la interfaz, con el candado del ciclo tomado)
    //el disco termina la solicitud en curso y no toma otra hasta reanudarlo
    public void detenerDisco() {
        reloj.tomarCandadoCiclo();
        try {
            disco.detener();
            registro.registrar(reloj.getCiclo(), disco.isDetenido() ? "Se detiene el disco"
                    : "Se pide detener el disco: termina la solicitud en curso y se detiene");
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    public void reanudarDisco() {
        reloj.tomarCandadoCiclo();
        try {
            disco.reanudar();
            registro.registrar(reloj.getCiclo(), "Se reanuda el disco");
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //solo con el disco detenido y libre; no cuenta como desplazamiento
    public void moverCabezal(int posicion, DireccionCabezal direccion) {
        reloj.tomarCandadoCiclo();
        try {
            disco.moverCabezal(posicion, direccion);
            registro.registrar(reloj.getCiclo(), "Se mueve el cabezal al bloque " + posicion + " con dirección " + direccion
                    + " (no cuenta como desplazamiento)");
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    public void setVelocidadCabezal(int velocidad) {
        if (velocidad < Limites.VELOCIDAD_CABEZAL_MIN || velocidad > Limites.VELOCIDAD_CABEZAL_MAX) {
            throw new IllegalArgumentException("La velocidad del cabezal debe estar entre " + Limites.VELOCIDAD_CABEZAL_MIN
                    + " y " + Limites.VELOCIDAD_CABEZAL_MAX);
        }
        reloj.tomarCandadoCiclo();
        try {
            disco.setVelocidad(velocidad);
            registro.registrar(reloj.getCiclo(), "La velocidad del cabezal cambia a " + velocidad + " posiciones por ciclo");
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //se aplica a partir del proximo ciclo
    public void cambiarPoliticaDisco(TipoPoliticaDisco politica) {
        reloj.tomarCandadoCiclo();
        try {
            planificadorDisco.cambiarPolitica(politica, reloj.getCiclo() + 1);
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //solicitudes en la cola del disco mas la que esta en curso (para confirmar si se baja la velocidad)
    public int solicitudesEnDisco() {
        reloj.tomarCandadoCiclo();
        try {
            return planificadorDisco.cantidadPendientes() + (disco.getActual() != null ? 1 : 0);
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    //modo usuario: revisa el permiso antes de crear el proceso (null si puede, o la razon);
    //para crear, la ruta es la del directorio donde se crea
    public String verificarPermiso(String usuario, TipoOperacionES operacion, String ruta) {
        return sistemaArchivos.puede(usuario, operacion, ruta);
    }

    //usuarios normales (user1..userN) para el selector de modo
    public String[] getUsuarios() {
        return sistemaArchivos.nombresUsuarios();
    }

    //estado actual del sistema de archivos, para guardarlo o para reiniciar con otros procesos
    public DatosSistemaArchivos exportarSistemaArchivos() {
        reloj.tomarCandadoCiclo();
        try {
            int total = disco.getBloquesArchivos();
            return new DatosSistemaArchivos(total, Math.min(disco.getCabezal(), total - 1), sistemaArchivos.exportar());
        } finally {
            reloj.soltarCandadoCiclo();
        }
    }

    public void guardarSistemaArchivos(Path archivo) throws IOException {
        DatosSistemaArchivos datos = exportarSistemaArchivos();
        ArchivoSistemaArchivos.guardar(archivo, datos.totalBloques(), datos.cabezalInicial(), datos.entradas());
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
        return new FotoSimulacion(ciclo, reloj.isPausado(), fotos, fotosBuffers, FotoMetricas.combinar(metricas),
                fotografiarDisco(), sistemaArchivos.fotografiar(), sistemaArchivos.tablaAsignacion());
    }

    //foto del disco: mapa de bloques, cabezal, solicitud en curso, cola y metricas
    private FotoDisco fotografiarDisco() {
        TrabajoDisco actual = disco.getActual();
        FotoSolicitud enCurso = actual == null ? null : FotoSolicitud.de((Solicitud) actual, disco.getBloquesHechos());
        Solicitud[] pendientes = planificadorDisco.pendientes();
        FotoSolicitud[] cola = new FotoSolicitud[pendientes.length];
        for (int i = 0; i < pendientes.length; i++) {
            cola[i] = FotoSolicitud.de(pendientes[i], 0);
        }
        MetricasDisco m = planificadorDisco.getMetricas();
        long[] politicas = m.serie(2);
        int[] seriePolitica = new int[politicas.length];
        for (int i = 0; i < politicas.length; i++) {
            seriePolitica[i] = (int) politicas[i];
        }
        return new FotoDisco(disco.getBloquesArchivos(), disco.getBloquesSwap(), disco.getCabezal(),
                disco.getDireccion(), disco.getVelocidad(), disco.isDetenido(), disco.isDeteniendo(),
                planificadorDisco.getTipoPolitica(), enCurso, cola, disco.ocupantes(), disco.colores(), disco.siguientes(),
                disco.getDesplazamiento(), m.getEsperaPromedio(), m.getAtendidasPorPolitica(),
                m.serie(0), m.serie(1), seriePolitica);
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
                c.enEjecucion() == null ? new FotoPCB[0] : new FotoPCB[]{c.enEjecucion()}};
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

    public Disco getDisco() {
        return disco;
    }

    public PlanificadorDisco getPlanificadorDisco() {
        return planificadorDisco;
    }

    public SistemaArchivos getSistemaArchivos() {
        return sistemaArchivos;
    }

    public int cantidadComputadores() {
        return cluster.cantidadComputadores();
    }
}
