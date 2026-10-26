/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;

import avilaos.estructuras.Cola;
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
 */

//Falta implementar buffers y trato con procesos P/C


public class Kernel implements ManejadorInterrupciones{
    
    //define el costo en ciclos de reloj el costo de cambiar el proceso
    private static final int COSTO_DESPACHO = 1;
    
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

    
    //esqueleto de metodo para procesos con buffer, avisando que se desbloqueo
    public void notificarDesbloqueo(PCB proceso) {}
    
    
    //antes de iniciar un ciclo, planifica, quien entra a memoria, quien toma la cpu
    public void antesDeEjecutar(long ciclo) {
        this.ciclo = ciclo;
        admitirNuevos();          // planificación a largo plazo
        planificar();             // planificación a corto plazo
    }
    
    
    
    //despues de ejecutar actualiza las estadisticas de los que esperan y si cambio la politica reordena
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
    //solo esta implementado E/S
    @Override
    public void llamadaAlSistema(long ciclo) {
        this.ciclo = ciclo;
        PCB p = enEjecucion;
        p.registrarEjecucion(ciclo);
        TipoInstruccion operacion = p.instruccionActual();

        if (operacion == TipoInstruccion.SOLICITAR_ES) {
            p.avanzarPc();
            liberarCPU();
            bloquear(p, MotivoBloqueo.ES);
            dispositivo.solicitar(p, p.getDuracionES());
            registrar("Proceso " + p.etiqueta() + " solicita E/S por " + p.getDuracionES() + " ciclos y se bloquea");
            return;
        }
        throw new UnsupportedOperationException("Las operaciones de buffer todavía no están implementadas");
    }

    
    //Interrupcion del dispositivo E/S indicando al sistema que termino
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

    
    
    // Procesos no finalizados pendientes de una CPU (para indicar en la interfaz)
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

