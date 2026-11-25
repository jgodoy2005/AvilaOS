package avilaos.gui;

import avilaos.Limites;
import avilaos.simulacion.FotoNodo;
import avilaos.so.DatosProceso;
import avilaos.so.ModoUsuario;
import avilaos.so.OperacionArchivo;
import avilaos.so.TipoOperacionES;
import avilaos.so.archivos.Permiso;
import avilaos.so.archivos.TipoNodo;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Formulario para pedir una operación sobre el sistema de archivos. La interfaz no toca el
 * árbol: solo crea un proceso de E/S de disco cuya primera instrucción es la operación.
 * Valida el tipo y el rango de cada campo; el espacio y los permisos los revisa el sistema
 * operativo cuando el proceso ejecuta la operación.
 */
public class DialogoOperacion extends JDialog {

    private final TipoOperacionES operacion;
    private final TipoNodo tipoNuevo;          //al crear: archivo o directorio
    private final FotoNodo nodo;               //al crear: el directorio donde se crea
    private final Sesion sesion;
    private final int ramMB;

    private final JTextField nombreProceso = new JTextField(14);
    private final JTextField memoria = new JTextField("16", 6);
    private final JTextField prioridad = new JTextField(String.valueOf(Limites.PRIORIDAD_DEFECTO), 6);
    private final JTextField deadline = new JTextField("500", 6);
    private final JTextField instrucciones = new JTextField("2", 6);
    private final JTextField nombre = new JTextField(14);         //crear: nombre nuevo; renombrar: nombre nuevo
    private final JTextField bloques = new JTextField("3", 6);
    private final ModeloLista<Permiso> modeloPermiso = new ModeloLista<>(Permiso.values());
    private final ModeloLista<String> modeloDueno;
    private DatosProceso resultado;

    private DialogoOperacion(Window padre, TipoOperacionES operacion, TipoNodo tipoNuevo, FotoNodo nodo,
                             Sesion sesion, String[] usuarios, int ramMB) {
        super(padre, "Nuevo proceso de E/S de disco", ModalityType.APPLICATION_MODAL);
        this.operacion = operacion;
        this.tipoNuevo = tipoNuevo;
        this.nodo = nodo;
        this.sesion = sesion;
        this.ramMB = ramMB;

        String[] duenos = new String[usuarios.length + 1];
        duenos[0] = ModoUsuario.CUENTA_ADMIN;
        System.arraycopy(usuarios, 0, duenos, 1, usuarios.length);
        modeloDueno = new ModeloLista<>(duenos);

        JPanel pedido = Formato.formulario();
        int fila = 0;
        Formato.fila(pedido, fila++, "Operación:", new JLabel(operacion + (tipoNuevo == null ? "" : " " + tipoNuevo.toString().toLowerCase())));
        Formato.fila(pedido, fila++, operacion == TipoOperacionES.CREAR ? "Dentro de:" : "Ruta:", new JLabel(nodo.ruta()));
        Formato.fila(pedido, fila++, "Lo pide:", new JLabel(sesion.cuenta() + " (modo " + sesion.getModo() + ")"));
        if (operacion == TipoOperacionES.CREAR) {
            Formato.fila(pedido, fila++, "Nombre:", nombre);
            if (tipoNuevo == TipoNodo.ARCHIVO) {
                Formato.fila(pedido, fila++, "Tamaño (bloques):", bloques);
            }
            Formato.fila(pedido, fila++, "Permiso:", new JComboBox<>(modeloPermiso));
            if (sesion.esAdministrador()) {
                Formato.fila(pedido, fila++, "Dueño:", new JComboBox<>(modeloDueno));
            } else {
                Formato.fila(pedido, fila++, "Dueño:", new JLabel(sesion.cuenta()));
            }
        } else if (operacion == TipoOperacionES.ACTUALIZAR) {
            nombre.setText(nodo.nombre());
            Formato.fila(pedido, fila++, "Nombre nuevo:", nombre);
        }
        pedido.setBorder(BorderFactory.createTitledBorder("Operación"));

        nombreProceso.setText(recortar(operacion + "_" + (operacion != TipoOperacionES.CREAR ? nodo.nombre()
                : tipoNuevo == TipoNodo.ARCHIVO ? "archivo" : "directorio")));
        JPanel proceso = Formato.formulario();
        Formato.fila(proceso, 0, "Nombre del proceso:", nombreProceso);
        Formato.fila(proceso, 1, "Memoria (MB):", memoria);
        Formato.fila(proceso, 2, "Prioridad (1 = la más alta, hasta 10):", prioridad);
        Formato.fila(proceso, 3, "Deadline (ciclos):", deadline);
        Formato.fila(proceso, 4, "Instrucciones (la 1.ª es la operación):", instrucciones);
        proceso.setBorder(BorderFactory.createTitledBorder("Datos del PCB"));

        JButton crear = new JButton("Crear proceso");
        JButton cancelar = new JButton("Cancelar");
        crear.addActionListener(e -> aceptar());
        cancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(cancelar);
        botones.add(crear);
        getRootPane().setDefaultButton(crear);

        JPanel contenido = new JPanel(new BorderLayout(8, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(pedido, BorderLayout.NORTH);
        contenido.add(proceso, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);
        setContentPane(contenido);
        pack();
        setLocationRelativeTo(padre);
    }

    /**
     * Muestra el formulario y espera.
     *
     * @return los datos del proceso, o null si se canceló
     */
    public static DatosProceso pedir(Component padre, TipoOperacionES operacion, TipoNodo tipoNuevo, FotoNodo nodo,
                                     Sesion sesion, String[] usuarios, int ramMB) {
        Window ventana = padre == null ? null : SwingUtilities.getWindowAncestor(padre);
        DialogoOperacion dialogo = new DialogoOperacion(ventana, operacion, tipoNuevo, nodo, sesion, usuarios, ramMB);
        dialogo.setVisible(true);
        return dialogo.resultado;
    }

    private void aceptar() {
        Validacion v = new Validacion();
        String n = v.texto(nombreProceso, "El nombre del proceso", Limites.NOMBRE_MAX);
        int mb = v.entero(memoria, "La memoria", Limites.MEMORIA_PROCESO_MIN_MB, ramMB);
        int prio = v.entero(prioridad, "La prioridad", Limites.PRIORIDAD_MIN, Limites.PRIORIDAD_MAX);
        int dl = v.entero(deadline, "El deadline", Limites.DEADLINE_MIN, Limites.DEADLINE_MAX);
        int ins = v.entero(instrucciones, "La cantidad de instrucciones", Limites.INSTRUCCIONES_MIN, Limites.INSTRUCCIONES_MAX);
        String nuevo = null;
        int tam = 0;
        if (operacion == TipoOperacionES.CREAR || operacion == TipoOperacionES.ACTUALIZAR) {
            nuevo = v.texto(nombre, "El nombre", Limites.NOMBRE_ARCHIVO_MAX);
            if (nuevo.contains("/")) {
                v.error("El nombre no puede tener '/'.");
            }
        }
        if (operacion == TipoOperacionES.CREAR && tipoNuevo == TipoNodo.ARCHIVO) {
            tam = v.entero(bloques, "El tamaño", Limites.TAMANO_ARCHIVO_MIN, Limites.BLOQUES_DISCO_MAX);
        }
        if (v.hayErrores()) {
            v.mostrar(this);
            return;
        }
        //renombrar con el mismo nombre se permite (como el UPDATE del JSON), pero se avisa
        if (operacion == TipoOperacionES.ACTUALIZAR && nuevo.equals(nodo.nombre())
                && JOptionPane.showConfirmDialog(this, "El nombre nuevo es igual al actual: el proceso usará el disco, "
                        + "pero el nombre no cambia.\n¿Crear el proceso igual?", "Mismo nombre",
                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        String cuenta = sesion.cuenta();
        OperacionArchivo op = switch (operacion) {
            case LEER -> OperacionArchivo.leer(nodo.ruta(), cuenta);
            case ELIMINAR -> OperacionArchivo.eliminar(nodo.ruta(), cuenta);
            case ACTUALIZAR -> OperacionArchivo.renombrar(nodo.ruta(), nuevo, cuenta);
            default -> {
                String ruta = (nodo.ruta().equals("/") ? "" : nodo.ruta()) + "/" + nuevo;
                Permiso permiso = (Permiso) modeloPermiso.getSelectedItem();
                String dueno = sesion.esAdministrador() ? (String) modeloDueno.getSelectedItem() : cuenta;
                yield tipoNuevo == TipoNodo.ARCHIVO
                        ? OperacionArchivo.crearArchivo(ruta, tam, permiso, dueno, cuenta)
                        : OperacionArchivo.crearDirectorio(ruta, permiso, dueno, cuenta);
            }
        };
        resultado = DatosProceso.deDisco(n, op, ins, mb, prio, dl, 1);
        dispose();
    }

    private static String recortar(String texto) {
        return texto.length() <= Limites.NOMBRE_MAX ? texto : texto.substring(0, Limites.NOMBRE_MAX);
    }
}