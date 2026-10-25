/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
 */
public class CPU implements ComponenteReloj {
    
    private ManejadorInterrupciones manejador;
    private ContextoEjecucion proceso;
    private ModoCPU modo= ModoCPU.SISTEMA_OPERATIVO;
    private String descripcion = "Ociosa";
    private boolean ociosa = true;
    private int ciclosDespachoPendientes;
    private int ciclosEnRafaga;
    
    private long ciclosUsuario;
    private long ciclosSistema;
    private long ciclosOciosa;
    
    
    //conecta la CPU con el SO que maneja sus traps
    public void conectar (ManejadorInterrupciones manejador){
        this.manejador = manejador;
    }
    
    
    //cargar un proceso en el cpu, agregando ciclos de modoSO por el cambio de contexto
    public void cargar(ContextoEjecucion proceso, int costoDespacho){
        this.proceso = proceso;
        this.ciclosEnRafaga = 0;
        this.ciclosDespachoPendientes = costoDespacho;
    }
    
    
    //el SO le quita la CPU a un proceso 
    public ContextoEjecucion desalojar(){
        ContextoEjecucion salida = proceso;
        proceso = null;
        ciclosDespachoPendientes = 0;
        ciclosEnRafaga = 0;
        return salida;
    }
    
    @Override
    public void tic(long ciclo){
        
        //No hay procesos
        if(proceso==null){
            ociosa = true;
            modo = ModoCPU.SISTEMA_OPERATIVO;
            descripcion = "Ociosa: no hay procesos listos";
            ciclosOciosa++;
            return;
        }
        ociosa = false;
        if(ciclosDespachoPendientes > 0){
            ciclosDespachoPendientes--;
            modo = ModoCPU.SISTEMA_OPERATIVO;
            descripcion = "Despachador: cambio de contexto a " + proceso.etiqueta();
            ciclosSistema++;
            return;
        }
        
        
        //Ciclo de instruccion
        proceso.setMar(proceso.getPc());
        modo = proceso.modoInstruccionActual();
        descripcion = proceso.descripcionInstruccionActual();
        ciclosEnRafaga++;
        
        //Ejecucion
        if(modo==ModoCPU.USUARIO){
            ciclosUsuario++;
            proceso.avanzarPc();
            manejador.instruccionCompletada(ciclo);
        }else{
            ciclosSistema++;
            manejador.llamadaAlSistema(ciclo);            
        }
    }
    
    public ContextoEjecucion getProceso(){
        return proceso;
    }
    
    public boolean isOciosa() {
        return ociosa;
    }

    public ModoCPU getModo() {
        return modo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCiclosEnRafaga() {
        return ciclosEnRafaga;
    }

    public long getCiclosUsuario() {
        return ciclosUsuario;
    }

    public long getCiclosSistema() {
        return ciclosSistema;
    }

    public long getCiclosOciosa() {
        return ciclosOciosa;
    }
    
}
