/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.sincronizacion;

import avilaos.so.MotivoBloqueo;
import avilaos.so.PCB;
import avilaos.so.TipoInstruccion;
import avilaos.so.TipoProceso;
import java.util.concurrent.Semaphore;

/**
 *
 * @author juandiegogodoy
 */


/**
 * 
 * @author erick
 * Buffer de capacidad limitada para productores y consumidores
 * Vive en la memoria de su computador anfitrion, pero lo pueden usar procesos de cualquier computador
 * los tres SemaforosSO (s, n, e) sincronizan a los procesos simulados
 * el semaforo de Java "candado" protege la estructura entre hilos de JAVA: dos
 * computadores pueden operar este buffer en el mismo ciclo
 */
public class BufferAcotado {
    public static final int MB_POR_ESPACIO = 4;
    
    private final String nombre;
    private final int numeroAnfitrion;
    private final int capacidad;
    
    private final String[] espacios;        //arreglo circular
    private int entra;                      // proxima posicion donde se anade
    private int sale;                       // procima posicion donde se extrae
    private int cantidad;
    private final SemaforoSO s;             // mutex: exlcusion mutua sobre el buffer (empieza en 1)
    private final SemaforoSO n;             // llenos: elementos disponibles (empieza en 0)
    private final SemaforoSO e;             // vacios: espacios disponibles
    
    private final Semaphore candado = new Semaphore(1, true);
    private long producidos;
    private long consumidos;
    
    public BufferAcotado(String nombre, int capacidad, int numeroAnfitrion) {
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad del buffer debe ser al menos 1");
        }
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.numeroAnfitrion = numeroAnfitrion;
        this.espacios = new String[capacidad];
        this.s = new SemaforoSO("s" , "mutex", 0, MotivoBloqueo.REGION_CRITICA);
        this.n = new SemaforoSO("n" , "llenos", 1, MotivoBloqueo.BUFFER_VACIO);
        this.e = new SemaforoSO("e", "vacios", capacidad, MotivoBloqueo.BUFFER_LLENO);
    }
    
    /** MB que ocupa en la RAM de su anfitrion*/
    public int memoriaMB() {
        return capacidad * MB_POR_ESPACIO;
    }
    
    public MotivoBloqueo ejecutarOperacion(PCB proceso, TipoInstruccion operacion) {
      candado.acquireUninterruptibly();
      try {
          proceso.avanzarPc();
          switch (operacion) {
              case SEM_WAIT_E:
                  return esperar(e, proceso);
              case SEM_WAIT_N:
                  return esperar(n, proceso);
              case SEM_WAIT_S:
                  return esperar(s, proceso);
              case SEM_SIGNAL_E:
                  senalar(e);
                  return null;
              case SEM_SIGNAL_N:
                  senalar(n);
                  return null;
              case SEM_SIGNAL_S:
                  senalar(s);
                  return null;
              case INSERTAR:
                  insertar(proceso);
                  return null;
              case EXTRAER:
                  extraer(proceso);
                  return null;
              default:
                  throw new IllegalArgumentException(operacion + "no es uuna operacion de buffer");
          }
      } finally {
          candado.release();
      }
    }
    
    private MotivoBloqueo esperar(SemaforoSO semaforo, PCB proceso) {
        return semaforo.semWait(proceso) ? semaforo.getMotivo() : null; 
    }
    
    private void senalar(SemaforoSO semaforo) {
        PCB despertado = semaforo.semSignal();
        if (despertado != null) {
            despertado.getKernel().notificarDesbloqueo(despertado);// oden de candados: buffer -> buzon
        }
    }
    
    private void insertar(PCB proceso) {
        if (cantidad == capacidad) {
            throw new IllegalStateException("Buffer " + nombre + "lleno: los semaforos debieron impedirlo");          
        }
        espacios[entra] = proceso.etiqueta() + '#' + (proceso.getElementosHechos() + 1);
        entra = (entra + 1) % capacidad;
        cantidad++;
        producidos++;
        proceso.contarElemento();
    }
    
    private void extraer(PCB proceso) {
        if (cantidad == 0) {
            throw new IllegalStateException("Buffer " + nombre + "vacio: los semaforos debieron impedirlo");
        }
        espacios[sale] = null;
        sale = (sale + 1) % capacidad;
        cantidad--;
        consumidos++;
        proceso.contarElemento();
    }
    
    public String liberarRecursos(PCB proceso) {
        candado.acquireUninterruptibly();
        try {
            StringBuilder devuelto = new StringBuilder();
            SemaforoSO enCola = proceso.getEsperandoEn();
            if (enCola != null) {
                enCola.retirar(proceso);            // deshace el semWait que lo tenia esperando
                devuelto.append(" sale de la cola de").append(enCola.getNombre()).append(";");
            }
            int k = proceso.getCiclosPorElemento();
            int o = proceso.getPc() % proceso.largoIteracion();     //proxima instruccion dentro del elemento
            
            boolean tieneMutex;
            boolean reservaSinUsar;         //productor: un espacio; consumidos: un elemento         
            boolean senalPendiente;         //productor: semSignal(n); consumidor:semSignal(e)
            if (proceso.getTipo() == TipoProceso.PRODUCTOR) {
                        reservaSinUsar = (o == k + 1 && enCola != e) || o == k + 2;
                         tieneMutex = (o == k + 2 && enCola != s) || o == k + 3;
                        senalPendiente = o == k + 3 || o == k + 4;
            } else {
                reservaSinUsar = (o == 1 && enCola != n) || o == 2;
                tieneMutex = (o == 2 && enCola != s) || o == 3;
                senalPendiente = o == 3 || o == 4;
            }
            boolean productor = proceso.getTipo() == TipoProceso.PRODUCTOR;
            if (tieneMutex) {
                senalar(s);
                devuelto.append(" devuelve el mutex s;");
            }
            if (reservaSinUsar) {
                senalar(productor ? e : n);
                devuelto.append(productor ? " devuelve un espacio (e);" : "devuelve un elemento (n);");
            }
            if (senalPendiente) {
                senalar(productor ? n : e);
                devuelto.append(productor ? " hace el semSignal(n) pendiente;" : " hace el semSignal(e) pendiente;");
            }
            String texto = devuelto.toString().trim();
            return texto.endsWith(";") ? texto.substring(0, texto.length() - 1) : texto;
        } finally {
            candado.release();
        }
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public int getNumeroAnfitrion() {
        return numeroAnfitrion;
    }
    
    public int getCapacidad() {
        return capacidad;
    }
    
    public int getCantidad() {
        candado.acquireUninterruptibly();
        try {
            return cantidad;
        } finally  {
            candado.release();
        }
    }
    
    //* elementos del mas viejo al mas nuevo*/
    public String[] contenido() {
        candado.acquireUninterruptibly();
        try {
            String[] copia = new String[cantidad];
            for (int i = 0; i < cantidad; i++) {
                copia[i] = espacios[(sale + i) % capacidad];
            }
            return copia;
        } finally {
            candado.release();
        }
    }
    
    public SemaforoSO getMutex() {
        return s;
    }
    
    public SemaforoSO getLlenos() {
        return n;
    }
    
    public SemaforoSO getVacios() {
        return e;
    }
    
    public long getProducidos() {
        return producidos;
    }

    public long getConsumidos() {
        return consumidos;
    }
}


