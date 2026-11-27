package avilaos.gui;

import avilaos.estructuras.Accion;
import avilaos.estructuras.ListaEnlazada;
import avilaos.simulacion.FotoArchivo;
import avilaos.simulacion.FotoNodo;
import avilaos.simulacion.FotoSimulacion;
import avilaos.simulacion.Simulacion;
import avilaos.so.DatosProceso;
import avilaos.so.TipoOperacionES;
import avilaos.so.archivos.SistemaArchivos;
import avilaos.so.archivos.TipoNodo;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * El sistema de archivos: JTree con la información del elemento seleccionado, los botones de
 * las operaciones (cada una crea un proceso de E/S de disco) y la tabla de asignación.
 * En modo usuario, antes de crear el proceso se revisa el permiso y, si no lo tiene, se
 * explica por qué (la simulación sigue).
 */
public class PanelArchivos extends JPanel {

    private static final String[] COLUMNAS_TABLA = {"Color", "Archivo", "Bloques", "Primer bloque", "Dueño", "Creado por"};

    private final Simulacion simulacion;
    private final Sesion sesion;
    private final Runnable alCambiar;
    private final Accion<Integer> alSeleccionarArchivo;   //primer bloque del archivo elegido (-1: ninguno)
    private final ModeloArbol modeloArbol = new ModeloArbol();
    private final JTree arbol = new JTree(modeloArbol);
    private final ModeloTabla<FotoArchivo> tabla = new ModeloTabla<>(COLUMNAS_TABLA, PanelArchivos::valor);
    private final JTable tablaAsignacion = new JTable(tabla);
    private final JLabel[] datos = new JLabel[8];
    private final JButton crearArchivo = new JButton("Nuevo archivo…");
    private final JButton crearDirectorio = new JButton("Nuevo directorio…");
    private final JButton leer = new JButton("Leer");
    private final JButton renombrar = new JButton("Renombrar…");
    private final JButton eliminar = new JButton("Eliminar");
    private String firma = "";           //resumen del arbol, para reconstruirlo solo si cambia
    private int ramMB;

    public PanelArchivos(Simulacion simulacion, Sesion sesion, Runnable alCambiar, Accion<Integer> alSeleccionarArchivo) {
        super(new BorderLayout(4, 4));
        this.simulacion = simulacion;
        this.sesion = sesion;
        this.alCambiar = alCambiar;
        this.alSeleccionarArchivo = alSeleccionarArchivo;

        arbol.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        arbol.addTreeSelectionListener(e -> {
            seleccionCambio();
            FotoNodo nodo = seleccionado();
            int primerBloque = nodo != null && nodo.tipo() == TipoNodo.ARCHIVO ? nodo.primerBloque() : -1;
            alSeleccionarArchivo.ejecutar(primerBloque);
        });
        JScrollPane scrollArbol = new JScrollPane(arbol);
        scrollArbol.setPreferredSize(new Dimension(160, 200));

        String[] titulos = {"Nombre:", "Tipo:", "Tamaño:", "Dueño:", "Permisos:", "Primer bloque:", "Creado por:", "Ruta:"};
        JPanel info = Formato.formulario();
        for (int i = 0; i < datos.length; i++) {
            datos[i] = new JLabel("-");
            Formato.filaCompacta(info, i, titulos[i], datos[i]);
        }
        info.setBorder(BorderFactory.createTitledBorder("Elemento seleccionado"));

        crearArchivo.addActionListener(e -> pedir(TipoOperacionES.CREAR, TipoNodo.ARCHIVO));
        crearDirectorio.addActionListener(e -> pedir(TipoOperacionES.CREAR, TipoNodo.DIRECTORIO));
        leer.addActionListener(e -> pedir(TipoOperacionES.LEER, null));
        renombrar.addActionListener(e -> pedir(TipoOperacionES.ACTUALIZAR, null));
        eliminar.addActionListener(e -> pedir(TipoOperacionES.ELIMINAR, null));
        JButton guardar = new JButton("Guardar JSON…");
        guardar.setToolTipText("Guarda el estado del sistema de archivos para cargarlo en otra ejecución");
        guardar.addActionListener(e -> guardar());
        JPanel botones = new JPanel(new GridLayout(2, 3, 4, 4));
        for (JButton b : new JButton[] {crearArchivo, crearDirectorio, leer, renombrar, eliminar, guardar}) {
            botones.add(b);
        }

        JPanel detalle = new JPanel(new BorderLayout(4, 4));
        detalle.add(info, BorderLayout.NORTH);
       // detalle.add(botones, BorderLayout.SOUTH);
        JPanel izquierda = new JPanel(new BorderLayout(4, 4));
        izquierda.add(scrollArbol, BorderLayout.CENTER);
        izquierda.add(detalle, BorderLayout.EAST);

        tablaAsignacion.setFillsViewportHeight(true);
        tablaAsignacion.setPreferredScrollableViewportSize(new Dimension(400, 8 * tablaAsignacion.getRowHeight()));
        int[] anchos = {35, 85, 50, 75, 50, 105};
        for (int i = 0; i < anchos.length; i++) {
            tablaAsignacion.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tablaAsignacion.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foco, int f, int c) {
                super.getTableCellRendererComponent(t, "", sel, foco, f, c);
                setBackground(Colores.archivo((Integer) valor));   //el mismo color que en el mapa
                return this;
            }
        });
        tablaAsignacion.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaAsignacion.getSelectedRow();
            if (!e.getValueIsAdjusting() && fila >= 0) {
                alSeleccionarArchivo.ejecutar(tabla.getFila(tablaAsignacion.convertRowIndexToModel(fila)).primerBloque());
            }
        });
        JScrollPane scrollTabla = new JScrollPane(tablaAsignacion);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Tabla de asignación de archivos"));

        //el arbol y los datos con su ancho; lo demas, para la tabla
        setBorder(BorderFactory.createTitledBorder("Sistema de archivos (seleccione un elemento y elija la operación)"));
        add(izquierda, BorderLayout.WEST);
        add(scrollTabla, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        seleccionCambio();
    }

    public void mostrar(FotoSimulacion f) {
        ramMB = f.computadores()[0].memoriaTotalMB();
        String nueva = firma(f.arbol());
        if (!nueva.equals(firma)) {
            reconstruirArbol(f.arbol(), firma.isEmpty());
            firma = nueva;
        }
        tabla.setFilas(f.tabla());
        seleccionCambio();
    }

    //cambia el arbol conservando lo que estaba abierto y lo seleccionado
    private void reconstruirArbol(FotoNodo raiz, boolean primeraVez) {
        ListaEnlazada<String> abiertos = new ListaEnlazada<>();
        for (int i = 0; i < arbol.getRowCount(); i++) {
            TreePath p = arbol.getPathForRow(i);
            if (arbol.isExpanded(p)) {
                abiertos.agregarAlFinal(((FotoNodo) p.getLastPathComponent()).ruta());
            }
        }
        String seleccionado = seleccionado() == null ? null : seleccionado().ruta();
        modeloArbol.setRaiz(raiz);
        if (primeraVez) {
            for (int i = 0; i < arbol.getRowCount(); i++) {
                arbol.expandRow(i);   //la primera vez se abre todo y se selecciona la raiz
            }
            arbol.setSelectionRow(0);
        } else {
            abiertos.paraCada(ruta -> {
                TreePath p = modeloArbol.camino(ruta);
                if (p != null) {
                    arbol.expandPath(p);
                }
            });
        }
        TreePath p = modeloArbol.camino(seleccionado);
        if (p != null) {
            arbol.setSelectionPath(p);
        }
    }

    private FotoNodo seleccionado() {
        TreePath p = arbol.getSelectionPath();
        return p == null ? null : (FotoNodo) p.getLastPathComponent();
    }

    //datos del elemento seleccionado y botones que tienen sentido para el
    private void seleccionCambio() {
        FotoNodo n = seleccionado();
        boolean hay = n != null;
        boolean directorio = hay && n.tipo() == TipoNodo.DIRECTORIO;
        boolean raiz = hay && n.ruta().equals("/");
        crearArchivo.setEnabled(directorio);
        crearDirectorio.setEnabled(directorio);
        leer.setEnabled(hay && !directorio);
        renombrar.setEnabled(hay && !raiz);
        eliminar.setEnabled(hay && !raiz);
        if (!hay) {
            for (JLabel d : datos) {
                d.setText("-");
            }
            return;
        }
        datos[0].setText(n.nombre());
        datos[1].setText(n.tipo().toString());
        datos[2].setText(directorio ? "-" : SistemaArchivos.bloques(n.cantidadBloques()));
        datos[3].setText(n.dueno());
        datos[4].setText(n.permiso().toString());
        datos[5].setText(directorio ? "-" : String.valueOf(n.primerBloque()));
        datos[6].setText(n.creador());
        datos[7].setText(n.ruta());
    }

    //arma el proceso de la operacion; en modo usuario revisa antes el permiso y explica si no lo tiene
    private void pedir(TipoOperacionES operacion, TipoNodo tipoNuevo) {
        FotoNodo n = seleccionado();
        if (n == null) {
            return;
        }
        if (n.tipo() == TipoNodo.ARCHIVO) {
            alSeleccionarArchivo.ejecutar(n.primerBloque());
        }
        if (!sesion.esAdministrador()) {
            String negado = simulacion.verificarPermiso(sesion.cuenta(), operacion, n.ruta());
            if (negado != null) {
                JOptionPane.showMessageDialog(this, sesion.cuenta() + " no puede " + operacion.toString().toLowerCase()
                        + (operacion == TipoOperacionES.CREAR ? " en " : " ") + n.ruta() + ":\n" + negado,
                        "Operación no permitida en modo usuario", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        DatosProceso datosProceso = DialogoOperacion.pedir(this, operacion, tipoNuevo, n, sesion, simulacion.getUsuarios(), ramMB);
        if (datosProceso == null) {
            return;
        }
        try {
            simulacion.crearProceso(datosProceso);
            alCambiar.run();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo crear el proceso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void guardar() {
        File carpeta = new File("configuraciones");
        JFileChooser selector = new JFileChooser(carpeta.isDirectory() ? carpeta : new File("."));
        selector.setFileFilter(new FileNameExtensionFilter("Sistema de archivos de AvilaOS (*.json)", "json"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".json")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".json");
        }
        try {
            simulacion.guardarSistemaArchivos(archivo.toPath());
            JOptionPane.showMessageDialog(this, "Sistema de archivos guardado en\n" + archivo.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
        }
    }

    //resumen de todo el arbol: si no cambia, no se reconstruye (asi no se cierran las carpetas)
    private static String firma(FotoNodo n) {
        StringBuilder sb = new StringBuilder();
        agregarFirma(n, sb);
        return sb.toString();
    }

    private static void agregarFirma(FotoNodo n, StringBuilder sb) {
        sb.append(n.ruta()).append('|').append(n.cantidadBloques()).append('|').append(n.primerBloque())
          .append('|').append(n.dueno()).append('|').append(n.permiso()).append(';');
        for (FotoNodo h : n.hijos()) {
            agregarFirma(h, sb);
        }
    }

    private static Object valor(FotoArchivo a, int columna) {
        return switch (columna) {
            case 0 -> a.color();
            case 1 -> a.nombre();
            case 2 -> a.cantidadBloques();
            case 3 -> a.primerBloque();
            case 4 -> a.dueno();
            default -> a.creador();
        };
    }
}