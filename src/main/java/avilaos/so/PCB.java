/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;
import avilaos.hardware.ContextoEjecucion;
import avilaos.hardware.ModoCPU;
import avilaos.so.sincronizacion.BufferAcotado;
import avilaos.so.sincronizacion.SemaforoSO;
/**
 *
 * @author juandiegogodoy
 */


/*
    quedan pendientes:
        kernel
        bufferacotado
        semaforo
        tipos de politicas
*/


public class PCB implements ContextoEjecucion {
    
    // Variables para identificar quien
    private final int id;                    
    private final String nombre;
    private final Kernel kernel;             // pendiente

    
    
    
    // verificar el estado del procesador (donde lo dejo)
    private int pc;                          // program counter, dirección de la próxima instrucción
    private int mar;                         // memory addres register, última dirección leída

    
    
    
    // manipular a los procesos
    
    private EstadoProceso estado = EstadoProceso.NUEVO;
    private final TipoProceso tipo;
    private final int prioridad;            
    private final int memoriaMB;
    private boolean memoriaAsignada;
    private final int deadline;              // ciclos que tiene para terminar, contados desde que se crea
    private long cicloLimite;                // ciclo en que vence: creación + deadline (fijo)
    private final int totalInstrucciones;

    private final int ciclosEntreES;         // para procesos de cpu o E/S: cada cuántas instrucciones pide E/S (0 = nunca)
    private final int duracionES;            // ciclos que tarda cada E/S

    private final BufferAcotado buffer;      // productor/consumidor; null en los demás
    private final int ciclosPorElemento;     // Cuanta tarda en crear/consumir un elemento
    private final int elementosRequeridos;   // Elementos totales
    private int elementosHechos;

    
    
    private MotivoBloqueo motivoBloqueo;
    private SemaforoSO esperandoEn;          // semáforo en cuya cola espera
    private TipoInstruccion operacionRemota; // operación que viaja por la red
    private int ciclosRedRestantes;
    private MotivoTerminacion motivoTerminacion;
    private long ordenLlegadaListos;         // para desempatar: quién entró antes a listos
    private int ciclosEsperando;             // ciclos seguidos en listos desde que entró (columna Espera)

    
    // Metricas
    private long cicloCreacion = -1;
    private long cicloPrimeraEjecucion = -1;
    private long cicloFin = -1;
    private int ciclosEjecutados;
    private int ciclosEnListos;
    private int ciclosBloqueadoSemaforo;
    private int ciclosBloqueadoRed;

    
    
    // Inicializa el PCB copiando la configuración del proceso y asignándole un Kernel y Buffer
    
    public PCB(int id, DatosProceso datos, Kernel kernel, BufferAcotado buffer) {
        this.id = id;
        this.nombre = datos.nombre();
        this.kernel = kernel;
        this.tipo = datos.tipo();
        this.prioridad = datos.prioridad();
        this.memoriaMB = datos.memoriaMB();
        this.deadline = datos.deadline();
        this.cicloLimite = deadline;         // como si se creara en el ciclo 0, hasta que llegue a un kernel
        this.totalInstrucciones = datos.totalInstrucciones();
        this.ciclosEntreES = datos.ciclosEntreES();
        this.duracionES = datos.duracionES();
        this.buffer = buffer;
        this.ciclosPorElemento = datos.ciclosPorElemento();
        this.elementosRequeridos = datos.elementos();
    }
    
    
    
    /*
    QUE INSTRUCCION HAY EN CADA DIRECCION DEL PROGRAMA
    */

    //  Saber en que instruccion se encuentra el programa 

    public TipoInstruccion instruccionActual() {
        return instruccionEn(pc);
    }

    
    
     // El código es lineal, así que la instrucción se calcula a partir de la dirección
     
    public TipoInstruccion instruccionEn(int direccion) {
        switch (tipo) {
            case CPU_BOUND:
            case IO_BOUND:
                // cada "ciclosEntreES" instrucciones, una es pedir E/S
                if (ciclosEntreES > 0 && (direccion + 1) % ciclosEntreES == 0) {
                    return TipoInstruccion.SOLICITAR_ES;
                }
                return TipoInstruccion.CALCULO;
            case PRODUCTOR: {
                // k veces producir(), luego semWait(e), semWait(s), añadir(), semSignal(s), semSignal(n)
                int k = ciclosPorElemento;
                int o = direccion % largoIteracion();
                if (o < k) {
                    return TipoInstruccion.PRODUCIR;
                }
                return switch (o - k) {
                    case 0 -> TipoInstruccion.SEM_WAIT_E;
                    case 1 -> TipoInstruccion.SEM_WAIT_S;
                    case 2 -> TipoInstruccion.INSERTAR;
                    case 3 -> TipoInstruccion.SEM_SIGNAL_S;
                    default -> TipoInstruccion.SEM_SIGNAL_N;
                };
            }
            default: {
                // CONSUMIDOR: semWait(n), semWait(s), extraer(), semSignal(s), semSignal(e), luego k veces consumir()
                int o = direccion % largoIteracion();
                return switch (o) {
                    case 0 -> TipoInstruccion.SEM_WAIT_N;
                    case 1 -> TipoInstruccion.SEM_WAIT_S;
                    case 2 -> TipoInstruccion.EXTRAER;
                    case 3 -> TipoInstruccion.SEM_SIGNAL_S;
                    case 4 -> TipoInstruccion.SEM_SIGNAL_E;
                    default -> TipoInstruccion.CONSUMIR;
                };
            }
        }
    }
    

    // Instrucciones que ocupa un elemento de un productor o consumidor: k + 5
    public int largoIteracion() {
        return ciclosPorElemento + DatosProceso.OPERACIONES_POR_ELEMENTO;
    }

    
    // Verifica si el proceso ya ejecutó todas sus instrucciones
    public boolean haTerminadoInstrucciones() {
        return pc >= totalInstrucciones;
    }

    // VISTA DE LA CPU
    // metodos para el cpu, simulando lo que 've' o 'consulta' el procesador durante la ejecucion
    
    @Override
    public int getPc() {
        return pc;
    }

    @Override
    public void avanzarPc() {
        pc++;
    }

    @Override
    public void setMar(int direccion) {
        this.mar = direccion;
    }

    @Override
    public ModoCPU modoInstruccionActual() {
        return instruccionActual().getModo();
    }

    @Override
    public String descripcionInstruccionActual() {
        TipoInstruccion ins = instruccionActual();
        return ins.esOperacionDeBuffer() ? ins + " en " + buffer.getNombre() : ins.toString();
    }


    // ETIQUETA DEL PROCESO, para identicarlo en el log
    // como estamos en monohilo se coloca siempre H1
    @Override
    public String etiqueta() {
        return "P" + id + "-H1";
    }

    
    
    
    /*
    Metodos para el uso del kernel y sus metricas
    Cambios de estado (los hace el kernel)
    registrar cuando se crea un proceso o se ejecuta
    contar ciclos en colas de listos o bloqueados
    contar elementos, y tiempos de espera para buffer en productores/consumidores
    */

    // El proceso llega al sistema: desde aquí corre su deadline, hasta el ciclo límite
    void marcarCreacion(long ciclo) {
        cicloCreacion = ciclo;
        cicloLimite = ciclo + deadline;
        estado = EstadoProceso.NUEVO;
    }

    // Entra a la cola de listos: recibe su turno y su espera empieza de cero
    void pasarAListos(long orden) {
        estado = EstadoProceso.LISTO;
        motivoBloqueo = null;
        ordenLlegadaListos = orden;
        ciclosEsperando = 0;
    }
    
    void pasarAEjecucion() {
        estado = EstadoProceso.EJECUCION;
    }

    void bloquear(MotivoBloqueo motivo) {
        estado = EstadoProceso.BLOQUEADO;
        motivoBloqueo = motivo;
    }

    void terminar(MotivoTerminacion motivo, long ciclo) {
        estado = EstadoProceso.TERMINADO;
        motivoTerminacion = motivo;
        motivoBloqueo = null;
        cicloFin = ciclo;
    }

    // Anota que el proceso ejecutó una instrucción en este ciclo. 
    void registrarEjecucion(long ciclo) {
        if (cicloPrimeraEjecucion < 0) {
            cicloPrimeraEjecucion = ciclo;
        }
        ciclosEjecutados++;
    }

    void iniciarOperacionRemota(TipoInstruccion operacion, int latencia) {
        operacionRemota = operacion;
        ciclosRedRestantes = latencia;
    }

    // Resta un ciclo a la latencia y devuelve lo que falta. 
    int descontarCicloDeRed() {
        return --ciclosRedRestantes;
    }

    // Devuelve la operación que llegó por la red y la borra. 
    TipoInstruccion tomarOperacionRemota() {
        TipoInstruccion op = operacionRemota;
        operacionRemota = null;
        return op;
    }

    void setMemoriaAsignada(boolean asignada) {
        this.memoriaAsignada = asignada;
    }

    void contarCicloEnListos() {
        ciclosEnListos++;
        ciclosEsperando++;
    }

    void contarCicloBloqueado() {
        if (motivoBloqueo == MotivoBloqueo.RED) {
            ciclosBloqueadoRed++;
        } else if (motivoBloqueo != null && motivoBloqueo.esDeSemaforo()) {
            ciclosBloqueadoSemaforo++;
        }
    }

    // Uso de buffer

    public void contarElemento() {
        elementosHechos++;
    }

    public SemaforoSO getEsperandoEn() {
        return esperandoEn;
    }

    public void setEsperandoEn(SemaforoSO semaforo) {
        this.esperandoEn = semaforo;
    }

    //Metodos para consultar 
    
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public Kernel getKernel() { return kernel; }
    public int getNumeroComputador() { return kernel.getNumero(); }
    public int getMar() { return mar; }
    public EstadoProceso getEstado() { return estado; }
    public TipoProceso getTipo() { return tipo; }
    public int getPrioridad() { return prioridad; }
    public int getMemoriaMB() { return memoriaMB; }
    public boolean isMemoriaAsignada() { return memoriaAsignada; }
    public int getDeadline() { return deadline; }
    public long getCicloLimite() { return cicloLimite; }
    
    // Ciclos que faltan para el ciclo límite (al llegar a 0, el proceso muere)
    
    public long getDeadlineRestante(long ciclo) { return cicloLimite - ciclo; }
    public int getCiclosEsperando() { return ciclosEsperando; }
    public int getTotalInstrucciones() { return totalInstrucciones; }
    public int getInstruccionesRestantes() { return Math.max(0, totalInstrucciones - pc); }
    public int getCiclosEntreES() { return ciclosEntreES; }
    public int getDuracionES() { return duracionES; }
    public BufferAcotado getBuffer() { return buffer; }
    public int getCiclosPorElemento() { return ciclosPorElemento; }
    public int getElementosRequeridos() { return elementosRequeridos; }
    public int getElementosHechos() { return elementosHechos; }
    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
    public TipoInstruccion getOperacionRemota() { return operacionRemota; }
    public int getCiclosRedRestantes() { return ciclosRedRestantes; }
    public MotivoTerminacion getMotivoTerminacion() { return motivoTerminacion; }
    public long getOrdenLlegadaListos() { return ordenLlegadaListos; }
    public long getCicloCreacion() { return cicloCreacion; }
    public long getCicloPrimeraEjecucion() { return cicloPrimeraEjecucion; }
    public long getCicloFin() { return cicloFin; }
    public int getCiclosEjecutados() { return ciclosEjecutados; }
    public int getCiclosEnListos() { return ciclosEnListos; }
    public int getCiclosBloqueadoSemaforo() { return ciclosBloqueadoSemaforo; }
    public int getCiclosBloqueadoRed() { return ciclosBloqueadoRed; }

    @Override
    public String toString() {
        return etiqueta() + " (" + nombre + ")";
    }

}
