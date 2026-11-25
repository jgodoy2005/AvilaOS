package avilaos.gui;

import avilaos.Limites;
import avilaos.config.ArchivoProcesos;
import avilaos.config.ArchivoSistemaArchivos;
import avilaos.config.ConfiguracionDisco;
import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.ConfiguracionSimulacion.ConfigBuffer;
import avilaos.config.ConfiguracionSimulacion.ConfigComputador;
import avilaos.config.DatosProcesos;
import avilaos.config.DatosSistemaArchivos;
import avilaos.estructuras.Accion;
import avilaos.hardware.DireccionCabezal;
import avilaos.so.DatosProceso;
import avilaos.so.archivos.EntradaFS;
import avilaos.so.archivos.TipoNodo;
import avilaos.so.disco.TipoPoliticaDisco;
import avilaos.so.planificacion.TipoPolitica;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Configuración del Proyecto 2. Antes de iniciar hay que cargar los dos JSON obligatorios:
 * el del sistema de archivos (disco, archivos y cabezal) y el de procesos (políticas y
 * procesos). Sus valores quedan en pantalla para revisarlos o cambiarlos; la velocidad y la
 * dirección del cabezal y la cantidad de usuarios se eligen aquí.
 */
public class PanelConfiguracion extends JPanel {

    //lo necesario para crear una simulacion
    public record DatosInicio(ConfiguracionSimulacion config, ConfiguracionDisco disco, EntradaFS[] archivos) {
    }

    //la ventana entrega el estado del disco de la simulacion actual (null si no hay ninguna)
    public interface EstadoDelDisco {
        DatosSistemaArchivos actual();
    }

    private final JLabel estadoArchivos = new JLabel("Sin cargar");
    private final JLabel estadoProcesos = new JLabel("Sin cargar");
    private final JSpinner duracion = PanelSimulacion.spinner(500,
            Limites.DURACION_CICLO_MIN_MS, Limites.DURACION_CICLO_MAX_MS, 50);
    private final JSpinner ram = PanelSimulacion.spinner(1024, Limites.RAM_MIN_MB, Limites.RAM_MAX_MB, 128);
    private final ModeloLista<TipoPolitica> modeloPolitica = new ModeloLista<>(TipoPolitica.values());
    private final JSpinner quantum = PanelSimulacion.spinner(Limites.QUANTUM_DEFECTO,
            Limites.QUANTUM_MIN, Limites.QUANTUM_MAX, 1);
    private final JSpinner bloques = PanelSimulacion.spinner(200, Limites.BLOQUES_DISCO_MIN, Limites.BLOQUES_DISCO_MAX, 10);
    private final JSpinner swap = PanelSimulacion.spinner(100, Limites.SWAP_MIN, Limites.SWAP_MAX, 10);
    private final ModeloLista<TipoPoliticaDisco> modeloPoliticaDisco = new ModeloLista<>(TipoPoliticaDisco.values());
    private final JSpinner cabezal = PanelSimulacion.spinner(50, 0, Limites.BLOQUES_DISCO_MAX + Limites.SWAP_MAX - 1, 1);
    private final ModeloLista<DireccionCabezal> modeloDireccion = new ModeloLista<>(DireccionCabezal.values());
    private final JSpinner velocidad = PanelSimulacion.spinner(Limites.VELOCIDAD_CABEZAL_DEFECTO,
            Limites.VELOCIDAD_CABEZAL_MIN, Limites.VELOCIDAD_CABEZAL_MAX, 1);
    private final JSpinner usuarios = PanelSimulacion.spinner(Limites.USUARIOS_DEFECTO,
            Limites.USUARIOS_MIN, Limites.USUARIOS_MAX, 1);
    private final ModeloTabla<DatosProceso> modeloProcesos = new ModeloTabla<>(
            new String[]{"Nombre", "Operación", "Ruta", "MB", "Prio.", "Deadline", "Instrucciones"},
            PanelConfiguracion::valorProceso);
    private final JButton iniciar = new JButton("Iniciar simulación");
    private final Accion<DatosInicio> alIniciar;
    private final EstadoDelDisco estadoDelDisco;
    private DatosSistemaArchivos archivos;   //lo que trajo el JSON del sistema de archivos
    private DatosProcesos procesos;          //lo que trajo el JSON de procesos

    public PanelConfiguracion(Accion<DatosInicio> alIniciar, EstadoDelDisco estadoDelDisco) {
        super(new BorderLayout(8, 8));
        this.alIniciar = alIniciar;
        this.estadoDelDisco = estadoDelDisco;

        // Los dos JSON obligatorios
        JButton cargarArchivos = new JButton("Cargar JSON del sistema de archivos…");
        JButton usarActual = new JButton("Usar el disco de la simulación actual");
        JButton cargarProcesos = new JButton("Cargar JSON de procesos…");
        cargarArchivos.addActionListener(e -> cargarSistemaArchivos());
        usarActual.addActionListener(e -> usarDiscoActual());
        cargarProcesos.addActionListener(e -> cargarProcesos());
        JPanel entradas = Formato.formulario();
        JPanel botonesArchivos = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botonesArchivos.add(cargarArchivos);
        botonesArchivos.add(usarActual);
        Formato.fila(entradas, 0, "Sistema de archivos:", botonesArchivos);
        Formato.fila(entradas, 1, "", estadoArchivos);
        JPanel botonesProcesos = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botonesProcesos.add(cargarProcesos);
        Formato.fila(entradas, 2, "Procesos y políticas:", botonesProcesos);
        Formato.fila(entradas, 3, "", estadoProcesos);
        entradas.setBorder(BorderFactory.createTitledBorder("Archivos de entrada (los dos son obligatorios)"));

        // Parametros: vienen de los JSON y se pueden cambiar
        JPanel parametros = Formato.formulario();
        Formato.fila(parametros, 0, "Duración del ciclo (ms):", duracion);
        Formato.fila(parametros, 1, "RAM del computador (MB):", ram);
        Formato.fila(parametros, 2, "Política de la CPU:", new JComboBox<>(modeloPolitica));
        Formato.fila(parametros, 3, "Quantum de Round Robin (ciclos):", quantum);
        Formato.fila(parametros, 4, "Bloques del disco (zona de archivos):", bloques);
        Formato.fila(parametros, 5, "Bloques del área de intercambio (swap):", swap);
        Formato.fila(parametros, 6, "Política de disco inicial:", new JComboBox<>(modeloPoliticaDisco));
        Formato.fila(parametros, 7, "Posición inicial del cabezal:", cabezal);
        Formato.fila(parametros, 8, "Dirección inicial del cabezal:", new JComboBox<>(modeloDireccion));
        Formato.fila(parametros, 9, "Velocidad del cabezal (posiciones por ciclo):", velocidad);
        Formato.fila(parametros, 10, "Usuarios (además de admin):", usuarios);
        parametros.setBorder(BorderFactory.createTitledBorder("Parámetros (se llenan con los JSON)"));

        JTable tablaProcesos = new JTable(modeloProcesos);
        tablaProcesos.setPreferredScrollableViewportSize(new Dimension(600, 12 * tablaProcesos.getRowHeight()));
        tablaProcesos.getColumnModel().getColumn(2).setPreferredWidth(180);
        JScrollPane scrollProcesos = new JScrollPane(tablaProcesos);
        scrollProcesos.setBorder(BorderFactory.createTitledBorder("Procesos del JSON (se crean al iniciar)"));

        JPanel centro = new JPanel(new GridLayout(1, 2, 12, 0));
        centro.add(parametros);
        centro.add(scrollProcesos);

        iniciar.setFont(iniciar.getFont().deriveFont(Font.BOLD));
        iniciar.setEnabled(false);
        iniciar.addActionListener(e -> iniciar());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        botones.add(new JLabel("La simulación arranca en pausa: use \"Reanudar\" en la pestaña Simulación. "));
        botones.add(iniciar);

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(entradas, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    /**
     * Arma la configuración con lo que hay en pantalla y la valida.
     */
    public DatosInicio leer() {
        ConfiguracionSimulacion config = new ConfiguracionSimulacion(
                (Integer) duracion.getValue(),
                (Integer) quantum.getValue(),
                0,
                new ConfigComputador[] {new ConfigComputador((Integer) ram.getValue(),
                    (TipoPolitica) modeloPolitica.getSelectedItem())},
                new ConfigBuffer[0],
                procesos.procesos());
        config.validar();
        ConfiguracionDisco disco = new ConfiguracionDisco(
                (Integer) bloques.getValue(),
                (Integer) swap.getValue(),
                (Integer) cabezal.getValue(),
                (DireccionCabezal) modeloDireccion.getSelectedItem(),
                (Integer) velocidad.getValue(),
                (TipoPoliticaDisco) modeloPoliticaDisco.getSelectedItem(),
                (Integer) usuarios.getValue());
        disco.validar();
        return new DatosInicio(config, disco, archivos.entradas());
    }

    private void iniciar() {
        try {
            alIniciar.ejecutar(leer());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Configuración inválida", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cargarSistemaArchivos() {
        File archivo = elegirArchivo();
        if (archivo != null) {
            cargarSistemaArchivos(archivo);
        }
    }

    private void cargarSistemaArchivos(File archivo) {
        try {
            mostrarSistemaArchivos(ArchivoSistemaArchivos.cargar(archivo.toPath()), archivo.getName());
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo cargar el archivo", JOptionPane.WARNING_MESSAGE);
        }
    }

    //para reiniciar con otros procesos sin perder lo que se hizo en el disco
    private void usarDiscoActual() {
        DatosSistemaArchivos actual = estadoDelDisco.actual();
        if (actual == null) {
            JOptionPane.showMessageDialog(this, "Todavía no hay una simulación.", "Sin simulación",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        mostrarSistemaArchivos(actual, "estado actual del disco");
    }

    private void mostrarSistemaArchivos(DatosSistemaArchivos datos, String origen) {
        archivos = datos;
        bloques.setValue(datos.totalBloques());
        cabezal.setValue(datos.cabezalInicial());
        int cantidadArchivos = 0;
        int directorios = 0;
        int ocupados = 0;
        for (EntradaFS e : datos.entradas()) {
            if (e.tipo() == TipoNodo.ARCHIVO) {
                cantidadArchivos++;
                ocupados += e.bloques();
            } else {
                directorios++;
            }
        }
        estadoArchivos.setText(origen + ": " + datos.totalBloques() + " bloques (" + ocupados + " ocupados), cabezal en "
                + datos.cabezalInicial() + ", " + cantidadArchivos + " archivos y " + directorios + " directorios");
        iniciar.setEnabled(procesos != null);
    }

    private void cargarProcesos() {
        File archivo = elegirArchivo();
        if (archivo != null) {
            cargarProcesos(archivo);
        }
    }

    private void cargarProcesos(File archivo) {
        try {
            procesos = ArchivoProcesos.cargar(archivo.toPath());
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo cargar el archivo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        duracion.setValue(procesos.duracionCicloMs());
        ram.setValue(procesos.ramMB());
        modeloPolitica.setSelectedItem(procesos.politica());
        quantum.setValue(procesos.quantum());
        swap.setValue(procesos.bloquesSwap());
        modeloPoliticaDisco.setSelectedItem(procesos.politicaDisco());
        modeloProcesos.setFilas(procesos.procesos());
        estadoProcesos.setText(archivo.getName() + ": " + procesos.procesos().length + " procesos, CPU con "
                + procesos.politica() + ", disco con " + procesos.politicaDisco() + ", swap de " + procesos.bloquesSwap() + " bloques");
        iniciar.setEnabled(archivos != null);
    }

    private File elegirArchivo() {
        File carpeta = new File("configuraciones");
        JFileChooser selector = new JFileChooser(carpeta.isDirectory() ? carpeta : new File("."));
        selector.setFileFilter(new FileNameExtensionFilter("JSON de AvilaOS (*.json)", "json"));
        return selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? selector.getSelectedFile() : null;
    }

    private static Object valorProceso(DatosProceso p, int columna) {
        return switch (columna) {
            case 0 -> p.nombre();
            case 1 -> p.archivo() == null ? p.tipo() : p.archivo().operacion();
            case 2 -> p.archivo() == null ? "" : p.archivo().ruta();
            case 3 -> p.memoriaMB();
            case 4 -> p.prioridad();
            case 5 -> p.deadline();
            default -> p.totalInstrucciones();
        };
    }
}