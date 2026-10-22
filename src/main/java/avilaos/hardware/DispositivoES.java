/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;
import avilaos.estructuras.Cola;

/**
 *
 * @author erick
 */
public class DispositivoES implements ComponenteReloj {
    
    private ManejadorInterrupciones manejador;
    private final Cola<Solicitud> cola = new Cola<>();
    private Solicitud enCurso;
    
    public void conectar(ManejadorInterrupciones manjeador) {
        this.manejador = manejador;
    }
    
    public void solicitar(ContextoEjecucion proceso, int ciclos) {
        cola.encolar(new Solicitud(proceso, Math.max(1, ciclos)));
    }
    
    @Override
    public void tic(long ciclo) {
        if(enCurso == null) {
            enCurso = cola.desencolar();
        }
        if (enCurso == null) {
            return;
        }
        enCurso.ciclosRestantes--;
        if (enCurso.ciclosRestantes == 0) {
            ContextoEjecucion terminado = enCurso.proceso;
            enCurso = null;
            manejador.finDeES(terminado, ciclo);
        }
    }
    
    public boolean cancelar(ContextoEjecucion proceso) {
        if(enCurso != null && enCurso.proceso == proceso) {
            enCurso = null;
            return true;
        }
        Solicitud s = cola.buscar(x-> x.proceso == proceso);
        return s != null && cola.eliminar(s);
    }
    
    public String describir() {
        if (enCurso == null && cola.estaVacia()) {
            return "Libre";
        }
        StringBuilder texto = new StringBuilder();
        if (enCurso != null) {
            texto.append("Atiende a ").append(enCurso.proceso.etiqueta())
            .append(" (faltan ").append(enCurso.ciclosRestantes).append(")");
        }
        if (!cola.estaVacia()) {
            texto.append(texto.length() > 0 ? " . " : "").append("en cola:");
            boolean primero = true;
            for (Solicitud s : cola.aArreglo(new Solicitud[cola.tamano()])){
                texto.append(primero ? "" : " , ").append(s.proceso.etiqueta());
                primero = false;
            }
        }
        return texto.toString();
    }   
    
    private static final class Solicitud {
        final ContextoEjecucion proceso;
        int ciclosRestantes;
        
        Solicitud(ContextoEjecucion proceso, int ciclos) {
            this.proceso = proceso;
            this.ciclosRestantes = ciclos;
        }
    }
}
