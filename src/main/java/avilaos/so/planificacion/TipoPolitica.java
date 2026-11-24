/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so.planificacion;
import avilaos.so.Parametros;
/**
 *
 * @author juandiegogodoy
 */

public enum TipoPolitica {
    //politicas disponibles
    FCFS("FCFS"),
    ROUND_ROBIN("Round Robin"),
    PRIORIDAD("Prioridades apropiativas"),
    EDF("EDF");
    
    
    private final String nombre;
    
    
    TipoPolitica(String nombre) {
        this.nombre =nombre;
    }
  
    
    //metodo para llamar y aplicar cada politica disponible
    public PoliticaPlanificacion crear(Parametros parametros){
        return switch(this){
            case FCFS -> new PoliticaFCFS();
            case ROUND_ROBIN -> new PoliticaRoundRobin(parametros);
            case PRIORIDAD -> new PoliticaPrioridad();
            case EDF -> new PoliticaEDF();
        };
    }
    
    //convierte el texto del JSON de procesos: el nombre del enum o el que se muestra (FCFS, ROUND_ROBIN,
    //Round Robin, RR, PRIORIDAD, EDF), sin importar mayusculas; null si no es valido
    public static TipoPolitica desdeTexto(String texto) {
        String buscado = normalizar(texto);
        if (buscado.equals("RR")) {
            return ROUND_ROBIN;
        }
        for (TipoPolitica t : values()) {
            if (normalizar(t.name()).equals(buscado) || normalizar(t.nombre).equals(buscado)) {
                return t;
            }
        }
        return null;
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toUpperCase().replace("-", "").replace("_", "").replace(" ", "");
    }

    
    @Override 
    public String toString(){
        return nombre;
    }
    
    
}
