/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.config;
import avilaos.Limites;
import avilaos.so.DatosProceso;
import avilaos.so.planificacion.TipoPolitica;
/**
 *
 * @author erick
 *  Estructura completa de configuración inicial de la simulación.
 Mapea directamente con el documento JSON mediante Gson.
 */


public record ConfiguracionSimulacion(
    int duracionCicloMs,
    int quantum,
    int latenciaRed,
    ConfigComputador[] computadores,
    ConfigBuffer[] buffers,
    DatosProceso[] procesos) {

    // Record anidado para la configuración individual de cada computador
    public record ConfigComputador(int ramMB, TipoPolitica politica) {}

    // Record anidado para definir capacidad y nodo anfitrión de un buffer
    public record ConfigBuffer(int capacidad, int anfitrion) {}

    // Configuración por defecto: 2 computadores de 1024 MB sin carga inicial
    public static ConfiguracionSimulacion porDefecto() {
        return new ConfiguracionSimulacion(
            500, 4, 3,
            new ConfigComputador[] {
                new ConfigComputador(1024, TipoPolitica.FCFS),
                new ConfigComputador(1024, TipoPolitica.ROUND_ROBIN)
            },
            new ConfigBuffer[0],
            new DatosProceso[0]
        );
    }
    
// Revisa todos los rangos y consistencia lógica. Acumula todos los errores antes de fallar.
    public void validar() {
        StringBuilder errores = new StringBuilder();

        // Validaciones numéricas principales usando la clase Limites
        rango(errores, "La duración del ciclo (ms)", duracionCicloMs,
              Limites.DURACION_CICLO_MIN_MS, Limites.DURACION_CICLO_MAX_MS);
        rango(errores, "El quantum", quantum, Limites.QUANTUM_MIN, Limites.QUANTUM_MAX);
        rango(errores, "La latencia de red", latenciaRed, Limites.LATENCIA_MIN, Limites.LATENCIA_MAX);

        // Validación de computadores
        if (computadores == null || computadores.length < Limites.COMPUTADORES_MIN
            || computadores.length > Limites.COMPUTADORES_MAX) {
            errores.append("- Debe haber entre ").append(Limites.COMPUTADORES_MIN)
                   .append(" y ").append(Limites.COMPUTADORES_MAX).append(" computadores.\n");
        } else {
            for (int i = 0; i < computadores.length; i++) {
                ConfigComputador c = computadores[i];
                String quien = "Computador " + (i + 1);
                if (c == null || c.politica() == null) {
                    errores.append("- ").append(quien).append(": falta la política.\n");
                }
                if (c != null) {
                    rango(errores, quien + ": la RAM (MB)", c.ramMB(),
                          Limites.RAM_MIN_MB, Limites.RAM_MAX_MB);
                }
            }
        }

        int cantidadComputadores = computadores == null ? 0 : computadores.length;

        // Validación de buffers
        ConfigBuffer[] bs = buffers == null ? new ConfigBuffer[0] : buffers;
        for (int i = 0; i < bs.length; i++) {
            String quien = "Buffer B" + (i + 1);
            if (bs[i] == null) {
                errores.append("- ").append(quien).append(": vacío.\n");
                continue;
            }
            rango(errores, quien + ": la capacidad", bs[i].capacidad(),
                  Limites.CAPACIDAD_BUFFER_MIN, Limites.CAPACIDAD_BUFFER_MAX);
            rango(errores, quien + ": el anfitrión", bs[i].anfitrion(), 1,
                  Math.max(1, cantidadComputadores));
        }

        // Validación de procesos y referencias cruzadas
        DatosProceso[] ps = procesos == null ? new DatosProceso[0] : procesos;
        for (int i = 0; i < ps.length; i++) {
            DatosProceso p = ps[i];
            String quien = "Proceso " + (i + 1) + (p != null && p.nombre() != null ? " (" + p.nombre() + ")" : "");
            if (p == null) {
                errores.append("- ").append(quien).append(": vacío.\n");
                continue;
            }
            try {
                p.validar();
            } catch (IllegalArgumentException e) {
                errores.append("- ").append(quien).append(":\n").append(e.getMessage().indent(2));
            }
            if (p.computador() > cantidadComputadores) {
                errores.append("- ").append(quien).append(": el computador ")
                       .append(p.computador()).append(" no existe.\n");
            }
            if (p.tipo() != null && p.tipo().usaBuffer() && p.buffer() != null
                && !existeBuffer(p.buffer(), bs.length)) {
                errores.append("- ").append(quien).append(": el buffer ")
                       .append(p.buffer()).append(" no existe.\n");
            }
        }

        // Si se acumuló al menos un error, se lanza la excepción con el reporte completo
        if (errores.length() > 0) {
            throw new IllegalArgumentException(errores.toString().trim());
        }
    }

    private static boolean existeBuffer(String nombre, int cantidadBuffers) {
        for (int i = 1; i <= cantidadBuffers; i++) {
            if (nombre.equalsIgnoreCase("B" + i)) {
                return true;
            }
        }
        return false;
    }

    private static void rango(StringBuilder errores, String campo, int valor, int min, int max) {
        if (valor < min || valor > max) {
            errores.append("- ").append(campo).append(" debe estar entre ")
                   .append(min).append(" y ").append(max).append(".\n");
        }
    }
}