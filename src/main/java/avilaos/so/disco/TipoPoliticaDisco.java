/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so.disco;

/**
 *
 * @author juandiegogodoy
 * Políticas de planificación de disco. El nombre es el mismo que se escribe en el JSON y en el enunciado.
 */
public enum TipoPoliticaDisco {
    FIFO("FIFO"),
    SSTF("SSTF"),
    SCAN("SCAN"),
    C_SCAN("C-SCAN");

    private final String nombre;

    TipoPoliticaDisco(String nombre) {
        this.nombre = nombre;
    }
    
    public PoliticaDisco crear() {
        return switch (this) {
            case FIFO -> new PoliticaFIFO();
            case SSTF -> new PoliticaSSTF();
            case SCAN -> new PoliticaSCAN();
            case C_SCAN -> new PoliticaCSCAN();
        };
    }

    //convierte el texto del JSON (FIFO, SSTF, SCAN, C-SCAN; tambien acepta C_SCAN o minusculas); null si no es valido
    public static TipoPoliticaDisco desdeTexto(String texto) {
        String buscado = normalizar(texto);
        for (TipoPoliticaDisco t : values()) {
            if (normalizar(t.nombre).equals(buscado) || normalizar(t.name()).equals(buscado)) {
                return t;
            }
        }
        return null;
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toUpperCase().replace("-", "").replace("_", "").replace(" ", "");
    }

    public String toString() {
        return nombre;
    }
}
