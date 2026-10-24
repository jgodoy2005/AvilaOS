/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;
import avilaos.estructuras.ListaEnlazada;
import java.util.concurrent.Semaphore;
/**
 *
 * @author juandiegogodoy
 */


public final class Reloj implements Runnable {
    
    //componentes que marcaran el paso del reloj
    private final ListaEnlazada<Participante> participantes = new ListaEnlazada<>();
    
    //semaforos para poder tomar el candado del reloj
    
    //Acumula los avisos de cada componente cuando terminan su trabajo del ciclo actual
    //para evitar que se termine un ciclo y se avance a otro sin que los componentes hayan
    //acabado ese ciclo
    private final Semaphore finCiclo = new Semaphore(0);
    
    //Ayuda a controlar las pausas del reloj para ver y entender su funcionamiento
    private final Semaphore permisoContinuar = new Semaphore(0);
    
    //Sirve para controlar quien puede modificar o ver el sistema, evita que cuando la interfaz 
    //quiere tomar los datos para plasmarlos, asegurarse que no es medio de un ciclo del reloj
    private final Semaphore candadoCiclo = new Semaphore(1, true);
    
    
    // Configuración del tiempo de espera entre ciclos (en milisegundos) y número de ciclo actual
    private volatile int duracionCicloMs;
    private volatile long ciclo = 0;
    
    
    //Control del estado del sistema (si esta corriendo, en una pausa temporal o se detuvo completamente)
    private volatile boolean pausado = true;   
    private volatile boolean detenido = false;
    
    
    //referencias a componentes externos, el observador para la interfaz, y el hilo del reloj
    private ObservadorReloj observador;
    private Thread hilo;
    
    
    // define cuánto tiempo durara la pausa entre cada ciclo de reloj
    public Reloj(int duracionCicloMs){
        setDuracionCicloMs(duracionCicloMs);
    }
    
    
    // registra un componente en la lista antes de arrancar
    // falla si el reloj ya esta corriendo para evitar desorden a mitad de ejecucion
    public void registrar(ComponenteReloj componente, String nombreHilo){
        if (hilo != null) {
            throw new IllegalStateException("No se pueden registrar componentes con el reloj en marcha");
        }
        participantes.agregarAlFinal(new Participante(componente, nombreHilo));
    }
    
    
    //conecta una pantalla u observador para avisarle cada vez que un ciclo termina
    public void setObservador(ObservadorReloj observador){
        this.observador = observador;
    }
    
    
    
    //Iniciar el hilo del reloj, si ya estaba corriendo, devuelve error 
    //ademas despierta los hilos individuales de cada componente
    public void iniciar(){
        if(hilo!=null){
            throw new IllegalStateException("El reloj ya fue iniciado");
        }
        participantes.paraCada(p ->p.hilo.start());
        hilo = new Thread (this, "Reloj");
        hilo.setDaemon(true);
        hilo.start();
    }
    
    
    
    //bucledel hilo del reloj, revisa si esta en pausa, sino, ejecuta el ciclo y duerme el reloj
    //el tiempo configurado antes del siguiente 
    public void run(){
        while(!detenido){
            if(pausado){
                permisoContinuar.acquireUninterruptibly();
                if(detenido){
                    break;
                }
            }
            ejecutarCiclo();
            if(!pausado){
                try {
                    Thread.sleep(duracionCicloMs);
 
                } catch(InterruptedException e){
                    break;          // detener() interrumpe el sueño
                }
            }
        }
        participantes.paraCada(p -> p.inicio.release()); //despierta a los componentes para que terminen
    }
    
    
    
    //Ejecucion de un ciclo del reloj, garantiza que durante un ciclo nadie toque el sistema,
    //cada componente ejecute su trabajo en paralelo, terminen y reporten, para liberar el candado
    //y avisar a la interfaz
    private void ejecutarCiclo() {
        candadoCiclo.acquireUninterruptibly();  // nadie más toca el sistema
        try {
            ciclo++;
            participantes.paraCada(p -> p.inicio.release());          // tic: despierta a todos
            finCiclo.acquireUninterruptibly(participantes.tamano());   // barrera: espera a todos
        } finally {
            candadoCiclo.release();                                    // el sistema queda libre
        }
        avisarObservador();
    }

    
    //Avisa a la interfaz que se termino un ciclo y si falla la interfaz atrapa el error
    private void avisarObservador() {
        if (observador == null) {
            return;
        }
        try {
            observador.cicloTerminado(ciclo);
        } catch (RuntimeException e) {
            System.err.println("Error al cerrar el ciclo " + ciclo + ": " + e);
            e.printStackTrace();
        }
    }

    // Ejecuta varios ciclos de forma secuencial en el mismo hilo sin usar hilos en paralelo
    // sirve para hacer pruebas exactas y repetibles
    public void ejecutarCiclosSinHilos(int n) {
        if (hilo != null) {
            throw new IllegalStateException("El reloj ya tiene hilos en marcha");
        }
        for (int i = 0; i < n; i++) {
            candadoCiclo.acquireUninterruptibly();
            try {
                ciclo++;
                participantes.paraCada(p -> p.componente.tic(ciclo));
            } finally {
                candadoCiclo.release();
            }
            avisarObservador();
        }
    }

    
    
    // cierra el paso del reloj para que la interfaz de usuario tome una "foto"
    // o cambie datos sin que el sistema avance 
    public void tomarCandadoCiclo() {
        candadoCiclo.acquireUninterruptibly();
    }

    // abre el paso nuevamente para que el reloj pueda seguir
    public void soltarCandadoCiclo() {
        candadoCiclo.release();
    }

    
    
    //metodos para controlar la simulacion
    
    //quita la pausa y libera el semaforo del hilo para que el reloj continue
    public void reanudar() {
        pausado = false;
        permisoContinuar.release();
    }
    
    //pausa al reloj y bloquea su semaforo para que no pueda reanudarse
    public void pausar() {
        pausado = true;
        permisoContinuar.drainPermits();   // descarta permisos sobrantes
    }

    //le da un solo espacio al semaforo del reloj para que avance un solo ciclo
    //funciona solo desde pausado
    public void avanzarUnCiclo() {
        if (pausado) {
            permisoContinuar.release();
        }
    }

    //termina la simulacion, despierta a todos los hilos suspendidos 
    //para que puedan cerrar limpios
    public void detener() {
        detenido = true;
        permisoContinuar.release();
        if (hilo != null) {
            hilo.interrupt();
        } else {
            participantes.paraCada(p -> p.inicio.release());
        }
    }
    
    
    
    //metodos para conocer el estado del reloj y manipularlo (en que ciclo esta, si esta pausado,
    //duracion del ciclo y cambiar la duracion de los ciclos)
    public long getCiclo() {
        return ciclo;
    }
    public boolean isPausado() {
        return pausado;
    }
    public int getDuracionCicloMs() {
        return duracionCicloMs;
    }
    public void setDuracionCicloMs(int ms) {
        if (ms < 0) {
            throw new IllegalArgumentException("La duración del ciclo no puede ser negativa");
        }
        this.duracionCicloMs = ms;
    }
    
    
    
    

    // unifica a cada componente, su hilo y su semaforo personal como una sola cosa
    private final class Participante implements Runnable {
        final ComponenteReloj componente;
        final Semaphore inicio = new Semaphore(0);
        final Thread hilo;

        Participante(ComponenteReloj componente, String nombreHilo) {
            this.componente = componente;
            this.hilo = new Thread(this, nombreHilo);
            this.hilo.setDaemon(true);
        }

        
        
        //bucle de cada participante, cuando el reloj le avisa (inicia un ciclo), cumple
        //su tarea y avisa que ya termino, esperando el proximo aviso (ciclo)
        @Override
        public void run() {
            while (true) {
                inicio.acquireUninterruptibly();   // espera su tic
                if (detenido) {
                    return;
                }
                try {
                    componente.tic(ciclo);
                } catch (RuntimeException e) {
                    System.err.println("Error en " + hilo.getName() + " durante el ciclo " + ciclo + ": " + e);
                    e.printStackTrace();
                } finally {
                    finCiclo.release();            // avisa al reloj aunque haya fallado: la barrera no se traba
                }
            }
        }
    }   
    
}
