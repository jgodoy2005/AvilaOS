/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;

import avilaos.estructuras.Cola;
import avilaos.estructuras.ColaSincronizada;
import avilaos.so.sincronizacion.BufferAcotado;
import avilaos.so.sincronizacion.SemaforoSO;
import avilaos.hardware.CPU;
import avilaos.hardware.ContextoEjecucion;
import avilaos.hardware.DispositivoES;
import avilaos.hardware.ManejadorInterrupciones;
import avilaos.hardware.Memoria;
import avilaos.so.planificacion.PoliticaPlanificacion;
import avilaos.so.planificacion.TipoPolitica;
/**
 *
 * @author juandiegogodoy
 * @author erick
 */

public class Kernel implements ManejadorInterrupciones{
    
    //define el costo en ciclos de reloj el costo de cambiar el proceso
    private static final int COSTO_DESPACHO = 1;
    
    //controla que los procesos que no puedan terminarse nisiquiera en el mejor de los casos a causa de su
    //ciclo limite (holgura), mueran o no 
    private static final boolean ABORTAR_SI_INALCANZABLE = true;
    
    // componentes de hardware asignados a este nucleo y modulos auxiliares
    private final int numero;
    private final String nombre;
    private final CPU cpu;
    private final Memoria memoria;
    private final DispositivoES dispositivo;
    private final Planificador planificador;
    private final Parametros parametros;
    private final RegistroEventos registro;
    private final MetricasComputador metricas = new MetricasComputador();
    
    // Colas para administrar el ciclo de vida de los procesos
    private final Cola<PCB> nuevos = new Cola<>();
    private final Cola<PCB> listos = new Cola<>();
    private final Cola<PCB> bloqueados = new Cola<>();
    private final Cola<PCB> terminados = new Cola<>();
    //buzon seguro donde otros computadores dejan aviso cuando despiertan a uno de los procesos
    private final ColaSincronizada<PCB> buzonDesbloqueos = new ColaSincronizada<>();
    
    //almacena el proceso que actualmente ocupa la CPU (null si esta ociosa)
    private PCB enEjecucion;

    private long ciclo;
    private long secuenciaListos;   // numera las llegadas a listos, para desempatar por orden de llegada

    
    //inicializa las referencias al hardware y configura la politica
    //con la que iniciara la planificacion de procesos
    public Kernel(  int numero, 
                    CPU cpu,    
                    Memoria memoria, 
                    DispositivoES dispositivo,
                    PoliticaPlanificacion politicaInicial, 
                    Parametros parametros, 
                    RegistroEventos registro){
        
        this.numero = numero;
        this.nombre = "Computador " + numero;
        this.cpu = cpu;
        this.memoria = memoria;
        this.dispositivo = dispositivo;
        this.planificador = new Planificador(politicaInicial);
        this.parametros = parametros;
        this.registro = registro;
    }
    
    
   
    
    //si un proceso nuevo entra en medio de un ciclo, entra a nuevos (de una vez) pero se marca
    //su creacion en el proximo ciclo.
    public void recibirProceso(PCB p) {
        p.marcarCreacion(ciclo + 1);
        nuevos.encolar(p);
        registro.registrar(ciclo + 1, "Proceso " + p.etiqueta() + " (" + p.getNombre()
                + ") llega a la cola de nuevos del " + nombre + ", pide " + p.getMemoriaMB()
                + " MB y vence en el ciclo " + p.getCicloLimite());
    }

    
    //buzon para notificar que un proceso fue desbloqueado
    public void notificarDesbloqueo(PCB proceso) {
        buzonDesbloqueos.encolar(proceso);
    }
    
    //revisa el buzon de desbloqueos al inicio de un ciclo y pasa a listos a 
    //todos los procesos que fueron despertados
    private void atenderDesbloqueos() {
        Cola<PCB> despertados = buzonDesbloqueos.extraerTodos();
        while (!despertados.estaVacia()) {
            PCB p = despertados.desencolar();
            
            // si el proceso ya no esta en estado BLOQUEADO, ignoramos el aviso
            if (p.getEstado() != EstadoProceso.BLOQUEADO) {
                continue;
            }
            
            bloqueados.eliminar(p);
            pasarAListos(p);
            registrar("Proceso " + p.etiqueta() + " despierta por un semSignal en el buffer "
                    + p.getBuffer().getNombre() + " y pasa a listos");
        }
    }
    
    //antes de iniciar un ciclo, limpia vencidos, mete a memoria y le da la CPU a quien le toque
    public void antesDeEjecutar(long ciclo) {
        this.ciclo = ciclo;
        completarOperacionesRemotas(); // primero procesar mensajes de red que ya completaron su tiempo de viaje (para poder tomar en cuenta bloqueados por red)
        atenderDesbloqueos();     // despierta a los procesos que se acaban de desbloquear (los pasa a listos)
        revisarDeadlines();       // limpia procesos vencidos o que ya no llegan a tiempo
        admitirNuevos();          // planificación a largo plazo
        planificar();             // planificación a corto plazo
    }
    
    
    //control de operaciones remotas en cluster
    
    //completar las operaciones de buffers remotos cuya latencia ya termino (terminaron su viaje)
    //se aplican sobre el buffer del anfitrion pero las ejecuta este kernel (emisor)
    private void completarOperacionesRemotas() {
        //recorrer bloqueados por red, y revisar si la latencia de alguno llego a 0 para desbloquearlo
        for (PCB p : bloqueados.aArreglo(new PCB[bloqueados.tamano()])) {
            if (p.getMotivoBloqueo() != MotivoBloqueo.RED || p.descontarCicloDeRed() > 0) {
                continue;
            }
            TipoInstruccion operacion = p.tomarOperacionRemota();
            BufferAcotado buffer = p.getBuffer();
            //una vez llega el mensaje se intenta ejecutar la operacion sobre el buffer
            MotivoBloqueo motivo = buffer.ejecutarOperacion(p, operacion);
            registrar("Llega al buffer " + buffer.getNombre() + " la operación " + operacion + " de "
                    + p.etiqueta() + " (remota)" + textoOcupacion(operacion, buffer));
            if (motivo != null) {
                //llego el mensaje el buffer lo bloqueo por otro motivo
                p.bloquear(motivo);
                registrarBloqueo(p, buffer, motivo);
            } else {
                //la operacion fue exitosa, se elimina de bloqueados y pasa a listos
                bloqueados.eliminar(p);
                if (p.haTerminadoInstrucciones()) {
                    finalizar(p, MotivoTerminacion.COMPLETADO);
                } else {
                    pasarAListos(p);
                }
            }
        }
    }
    
    
    
    //control de deadlines y aplicacion de holgura para ahorrar recursos de CPU
    
    //recorre todas las colas del sistema (nuevos, listos, bloqueados y CPU)
    //separa en dos listas a los que deben morir para no modificar las colas mientras las recorre
    private void revisarDeadlines() {
        Cola<PCB> vencidos = new Cola<>();
        Cola<PCB> inalcanzables = new Cola<>();
        
        nuevos.paraCada(p -> clasificar(p, vencidos, inalcanzables));
        listos.paraCada(p -> clasificar(p, vencidos, inalcanzables));
        bloqueados.paraCada(p -> clasificar(p, vencidos, inalcanzables));
        if (enEjecucion != null) {
            clasificar(enEjecucion, vencidos, inalcanzables);
        }
        
        // Despues de revisar a todos, procede a sacarlos del sistema uno por uno
        while (!vencidos.estaVacia()) {
            abortarPorDeadline(vencidos.desencolar(), MotivoTerminacion.DEADLINE);
        }
        while (!inalcanzables.estaVacia()) {
            abortarPorDeadline(inalcanzables.desencolar(), MotivoTerminacion.INALCANZABLE);
        }
    }
    
    //evalua si un proceso se paso de su tiempo o si la holgura dice que ya no llega
    private void clasificar(PCB p, Cola<PCB> vencidos, Cola<PCB> inalcanzables) {
        if (ciclo >= p.getCicloLimite()) {
            vencidos.encolar(p);
        } else if (ABORTAR_SI_INALCANZABLE && ciclo + ciclosMinimosParaTerminar(p) > p.getCicloLimite()) {
            inalcanzables.encolar(p);
        }
    }
    
    //CALCULOS PARA EL DEADLINE
    
    //le pide los datos al PCB para calcular sus ciclos pendientes 
    private int ciclosMinimosParaTerminar(PCB p) {
        int pc = p.getPc();
        if (p.getMotivoBloqueo() == MotivoBloqueo.RED) {
            pc++; // La operación remota en camino ya no necesita CPU para mandarse a otro CPU, se suma para no tomarse en cuenta en el calculo
        }
        return ciclosMinimos(p.getTipo(), pc, p.getTotalInstrucciones(),
                p.getCiclosEntreES(), p.getDuracionES(), p == enEjecucion);
    }
    
    //calcula lo minimo que necesita un proceso para terminar suponiendo que nadie lo hace esperar(el mejor caso)
    //es publico y estatico para que la interfaz pueda usar la misma cuenta al crearlo
    public static int ciclosMinimos(TipoProceso tipo, int pc, int total, int cadaES, int duracionES, boolean enCPU) {
        int restantes = Math.max(0, total - pc);
        int es = esPendientes(tipo, pc, total, cadaES);
        boolean terminaConES = es > 0 && total % cadaES == 0;
        int entradas = (restantes > 0 && !enCPU ? 1 : 0) + es - (terminaConES ? 1 : 0);
        return restantes + es * (Math.max(1, duracionES) - 1) + entradas * COSTO_DESPACHO;
    }
    
    //cuenta cuantas E/S le quedan por solicitar segun en que instruccion (PC) va
    private static int esPendientes(TipoProceso tipo, int pc, int total, int cadaES) {
        if (tipo.usaBuffer() || cadaES <= 0) {
            return 0;
        }
        return total / cadaES - pc / cadaES;
    }
    
    //LIBERACION DE RECURSOS SI UN PROCESO NO SE PODRA TERMINAR
    
    //saca al proceso de la cola donde este, libera memoria/buffers y guarda la razon en el log
    private void abortarPorDeadline(PCB p, MotivoTerminacion motivo) {
        // Armamos el mensaje explicativo antes de sacarlo de CPU (si estaba ahi)
        String porque;
        if (motivo == MotivoTerminacion.DEADLINE) {
            porque = "llegó a su ciclo límite (" + p.getCicloLimite() + ") sin terminar";
        } else {
            int es = esPendientes(p.getTipo(), p.getPc(), p.getTotalInstrucciones(), p.getCiclosEntreES());
            porque = "deadline inalcanzable (le faltan " + p.getInstruccionesRestantes() + " instrucciones"
                    + (es > 0 ? " y " + es + " E/S" : "") + ": necesita al menos " + ciclosMinimosParaTerminar(p)
                    + " ciclos y solo quedan " + p.getDeadlineRestante(ciclo) + ")";
        }
        
        //remueve el proceso de la cola en la que se encuentre actualmente
        switch (p.getEstado()) {
            case NUEVO -> nuevos.eliminar(p);
            case LISTO -> listos.eliminar(p);
            case BLOQUEADO -> {
                bloqueados.eliminar(p);
                if (p.getMotivoBloqueo() == MotivoBloqueo.ES) {
                    dispositivo.cancelar(p);
                }
            }
            case EJECUCION -> liberarCPU();
            default -> {
                return;
            }
        }
        
        //libera recursos consumidos y registra el evento
        String devuelto = p.getBuffer() != null ? p.getBuffer().liberarRecursos(p) : "";
        registrar("Proceso " + p.etiqueta() + " terminado: " + porque
                + (p.getCicloPrimeraEjecucion() < 0 ? ", sin haber recibido la CPU" : "")
                + (devuelto.isEmpty() ? "" : " (" + devuelto + ")"));
        
        finalizar(p, motivo);
    }
        
        
        
    //despues de ejecutar actualiza las estadisticas de los que esperan y ordena segun la politica
    public void despuesDeEjecutar(long ciclo) {
        this.ciclo = ciclo;
        planificador.ordenar(listos);                // la interfaz ve la cola en el orden de la política
        listos.paraCada(PCB::contarCicloEnListos);
        bloqueados.paraCada(PCB::contarCicloBloqueado);
        metricas.registrarCiclo(!cpu.isOciosa(), cpu.getModo());
    }
    
    
    
    //planificacion a largo plazo, se recorren los nuevos en orden, e ingresa a memoria a 
    //todos los que quepan
    private void admitirNuevos() {
        for (PCB p : nuevos.aArreglo(new PCB[nuevos.tamano()])) {
            if (memoria.reservar(p.getMemoriaMB())) {
                nuevos.eliminar(p);
                p.setMemoriaAsignada(true);
                pasarAListos(p);
                registrar(nombre + " admite a " + p.etiqueta() + " en memoria (" + p.getMemoriaMB()
                        + " MB; quedan " + memoria.getLibreMB() + " MB libres) y pasa a listos");
            }
        }
    }

    //planificacion a corto plazo, revisa si hay que expulsar al que se esta ejecutando o
    //o si no hay nadie se lo da al primero de listos
    private void planificar() {
        if (enEjecucion != null) {
            if (planificador.debeExpulsar(enEjecucion, listos, cpu.getCiclosEnRafaga())) {
                PCB expulsado = enEjecucion;
                String razon = planificador.getPolitica().razonExpulsion(expulsado, listos.verPrimero());
                liberarCPU();
                pasarAListos(expulsado);
                registrar(nombre + " expulsa a " + expulsado.etiqueta() + ": " + razon);
                despachar(planificador.seleccionar(listos));
            }
        } else if (!listos.estaVacia()) {
            despachar(planificador.seleccionar(listos));
        }
    }
    
    
    //cambia la politica de planificacion, ordena segun la nueva politica y carga el registro para el log
    //se aplica a partir del proximo ciclo
    public void cambiarPolitica(TipoPolitica politica) {
        if (politica == planificador.getPolitica().getTipo()) {
            return;
        }
        planificador.setPolitica(politica.crear(parametros));
        planificador.ordenar(listos);
        registro.registrar(ciclo + 1, nombre + " cambia su política de planificación a " + politica);
    }
    
    //se le da la CPU a un proceso sumandole el costo del cambio de contexto
    private void despachar(PCB p) {
        p.pasarAEjecucion();
        enEjecucion = p;
        cpu.cargar(p, COSTO_DESPACHO);
        registrar(nombre + " selecciona el proceso " + p.etiqueta());
    }

    //remover al proceso que estaba en cpu
    private void liberarCPU() {
        cpu.desalojar();
        enEjecucion = null;
    }
    
    
    //cambia el estado de un proceso a listos y le asigna un numero en la secuencia
    private void pasarAListos(PCB p) {
        p.pasarAListos(++secuenciaListos);
        listos.encolar(p);
    }

    //cambia el estado de un proceso a bloqueados y lo manda a la cola de bloqueados
    private void bloquear(PCB p, MotivoBloqueo motivo) {
        p.bloquear(motivo);
        bloqueados.encolar(p);
    }

    //termina un proceso, libera su memoria, lo manda a terminados y registra su razon 
    //de terminacion, intent admitir otros para aprovechar el espacio nuevo
    private void finalizar(PCB p, MotivoTerminacion motivo) {
        p.terminar(motivo, ciclo);
        if (p.isMemoriaAsignada()) {
            memoria.liberar(p.getMemoriaMB());
            p.setMemoriaAsignada(false);
        }
        terminados.encolar(p);
        metricas.registrarTerminacion(p);
        if (motivo == MotivoTerminacion.COMPLETADO) {
            registrar("Proceso " + p.etiqueta() + " terminó sus " + p.getTotalInstrucciones()
                    + " instrucciones y libera " + p.getMemoriaMB() + " MB");
        }
        admitirNuevos();
    }
    
    

    //indica que se completo una instruccion y revisa si termino
    //el proceso completo (si era la ultima instruccion)
    @Override
    public void instruccionCompletada(long ciclo) {
        this.ciclo = ciclo;
        PCB p = enEjecucion;
        p.registrarEjecucion(ciclo);
        if (p.haTerminadoInstrucciones()) {
            liberarCPU();
            finalizar(p, MotivoTerminacion.COMPLETADO);
        }
    }

    
    //atiende las peticiones del proceso de ejecucion al sistema
    @Override
    public void llamadaAlSistema(long ciclo) {
        this.ciclo = ciclo;
        PCB p = enEjecucion;
        p.registrarEjecucion(ciclo);
        TipoInstruccion operacion = p.instruccionActual();
        
        //manejo de una llamada de E/S, se bloquea el proceso, se solicita al dispositivoES y se registra para el log
        if (operacion == TipoInstruccion.SOLICITAR_ES) {
            p.avanzarPc();
            liberarCPU();
            bloquear(p, MotivoBloqueo.ES);
            dispositivo.solicitar(p, p.getDuracionES());
            registrar("Proceso " + p.etiqueta() + " solicita E/S por " + p.getDuracionES() + " ciclos y se bloquea");
            return;
        }
        
        //manejo de operaciones sobre el Buffer (productor/consumidor)
        BufferAcotado buffer = p.getBuffer();
        int latencia = parametros.getLatenciaRed();
        
        
        //si es remoto (latencia >0)
        if (buffer.getNumeroAnfitrion() != numero && latencia > 0) {
            p.iniciarOperacionRemota(operacion, latencia); //guarda la instruccion y fija la latencia
            liberarCPU();                                 //el proceso suelta el procesador
            bloquear(p, MotivoBloqueo.RED);               //pasa a la cola de bloqueados por red
            registrar("Proceso " + p.etiqueta() + " accede al buffer " + buffer.getNombre()
                    + " de forma remota: " + operacion + " (latencia de " + latencia + " ciclos)");
            return; // interrumpe la ejecución local
        }
        
        
        //si es acceso local (o acceso remoto con latencia 0)
        MotivoBloqueo motivo = buffer.ejecutarOperacion(p, operacion);
        
        //si fue una operacion de insertar o extraer se registra el progreso para el log
        if (operacion == TipoInstruccion.INSERTAR || operacion == TipoInstruccion.EXTRAER) {
            registrar("Proceso " + p.etiqueta() + " ejecuta " + operacion + " en " + buffer.getNombre()
                    + textoOcupacion(operacion, buffer));
        }
        
        
        
        //si no pudo completarse la operacion, el proceso libera la CPU y se bloquea, se registra para el log
        if (motivo != null) {
            liberarCPU();
            bloquear(p, motivo);
            registrarBloqueo(p, buffer, motivo);
        } else if (p.haTerminadoInstrucciones()) {
            //si la operacion fue exitosa y era su ultima instruccion, termina el proceso
            liberarCPU();
            finalizar(p, MotivoTerminacion.COMPLETADO);
        }
    }
    
        //arma un mensaje para el registro cuando un proceso queda bloqueado en un semaforo del buffer
        private void registrarBloqueo(PCB p, BufferAcotado buffer, MotivoBloqueo motivo) {
            SemaforoSO semaforo;
            String porque;
            switch (motivo) {
                case REGION_CRITICA -> { semaforo = buffer.getMutex(); porque = "región crítica ocupada"; }
                case BUFFER_VACIO -> { semaforo = buffer.getLlenos(); porque = "buffer vacío"; }
                default -> { semaforo = buffer.getVacios(); porque = "buffer lleno"; }
            }
            registrar("Proceso " + p.etiqueta() + " se bloquea en el semáforo '" + semaforo.getDescripcion() + "' ("
                    + semaforo.getNombre() + ") del buffer " + buffer.getNombre() + ": " + porque);
        }

        
        //si la operacion de buffer era añadir o extraer del buffer, devuelve la nueva ocupacion del buffer para el registro
        private String textoOcupacion(TipoInstruccion operacion, BufferAcotado buffer) {
            if (operacion != TipoInstruccion.INSERTAR && operacion != TipoInstruccion.EXTRAER) {
                return "";
            }
            return " (" + buffer.getCantidad() + "/" + buffer.getCapacidad() + ")";
        }
    
    //interrupcion del dispositivo E/S indicando al sistema que termino
    @Override
    public void finDeES(ContextoEjecucion proceso, long ciclo) {
        this.ciclo = ciclo;
        PCB p = (PCB) proceso;   // todo lo que este kernel le pide al dispositivo es un PCB
        if (p.getEstado() != EstadoProceso.BLOQUEADO) {
            return;
        }
        bloqueados.eliminar(p);
        registrar("Termina la E/S de " + p.etiqueta() + " (interrupción del dispositivo)");
        if (p.haTerminadoInstrucciones()) {
            finalizar(p, MotivoTerminacion.COMPLETADO);
        } else {
            pasarAListos(p);
        }
    }

    
    
    //guarda los mensajes del kernel y los asocia con el ciclo en que se mandaron
    private void registrar(String mensaje) {
        registro.registrar(ciclo, mensaje);
    }

    
    
    // Procesos vivos en este computador, para poder ver cual esta mas 'libre'
    public int procesosActivos() {
        return nuevos.tamano() + listos.tamano() + bloqueados.tamano() + (enEjecucion != null ? 1 : 0);
    }

    
    // metodos para obtener datos del sistema
    public int getNumero() { return numero; }
    public String getNombre() { return nombre; }
    public PCB getEnEjecucion() { return enEjecucion; }
    public PCB[] getNuevos() { return nuevos.aArreglo(new PCB[nuevos.tamano()]); }
    public PCB[] getListos() { return listos.aArreglo(new PCB[listos.tamano()]); }
    public PCB[] getBloqueados() { return bloqueados.aArreglo(new PCB[bloqueados.tamano()]); }
    public PCB[] getTerminados() { return terminados.aArreglo(new PCB[terminados.tamano()]); }
    public TipoPolitica getPolitica() { return planificador.getPolitica().getTipo(); }
    public MetricasComputador getMetricas() { return metricas; }
    public CPU getCpu() { return cpu; }
    public Memoria getMemoria() { return memoria; }
    public DispositivoES getDispositivo() { return dispositivo; }
}

