/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.Limites;
import avilaos.hardware.DireccionCabezal;
import avilaos.simulacion.FotoDisco;
import avilaos.simulacion.FotoSolicitud;
import avilaos.simulacion.Simulacion;
import avilaos.so.disco.TipoPoliticaDisco;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;

/**
 *
 * @author erick
 * 
  El disco: política con su selector, velocidad del cabezal, botón para detenerlo, mapa de
 * bloques y cola de E/S (en orden de llegada) con la solicitud en curso.
 */
public class PanelDisco extends JPanel {

    private static final String[] COLUMNAS_COLA = {"Proceso", "Operación", "Detalle", "Primer bloque", "Bloques", "Llegó (ciclo)"};

    private final Simulacion simulacion;
    private final Runnable alCambiar;
    private final Sesion sesion;
    private final ModeloLista<TipoPoliticaDisco> modeloPolitica = new ModeloLista<>(TipoPoliticaDisco.values());
    private final JComboBox<TipoPoliticaDisco> selectorPolitica = new JComboBox<>(modeloPolitica);
    private final JLabel politicaEnUso = new JLabel();
    private final JSpinner velocidad;
    private final JButton detener = new JButton("Detener disco");
    private final JButton mover = new JButton("Mover cabezal…");
    private final JLabel estado = new JLabel(" ");
    private final JLabel cabezal = new JLabel(" ");
    private final PanelMapaDisco mapa = new PanelMapaDisco();
    private final ModeloTabla<FotoSolicitud> cola = new ModeloTabla<>(COLUMNAS_COLA, PanelDisco::valor);
    private FotoDisco ultima;
    private boolean ajustando;   //el programa cambia el spinner: no es el usuario

    public PanelDisco(Simulacion simulacion, Runnable alCambiar) {
        this(simulacion, alCambiar, new Sesion());
    }

    PanelDisco(Simulacion simulacion, Runnable alCambiar, Sesion sesion) {
        super(new BorderLayout(4, 4));
        this.simulacion = simulacion;
        this.alCambiar = alCambiar;
        this.sesion = sesion;

        selectorPolitica.addActionListener(e -> {
            if (!ajustando && sesion.esAdministrador()) {
                simulacion.cambiarPoliticaDisco((TipoPoliticaDisco) modeloPolitica.getSelectedItem());
                alCambiar.run();
            }
        });
        //la velocidad se aplica con Enter, con las flechas o al salir del campo (no en cada tecla)
        velocidad = new JSpinner(new SpinnerNumberModel(Limites.VELOCIDAD_CABEZAL_DEFECTO,
                Limites.VELOCIDAD_CABEZAL_MIN, Limites.VELOCIDAD_CABEZAL_MAX, 1));
        ((JSpinner.DefaultEditor) velocidad.getEditor()).getTextField().setColumns(3);
        velocidad.addChangeListener(e -> cambiarVelocidad());
        detener.addActionListener(e -> detenerOReanudar());
        mover.addActionListener(e -> moverCabezal());

        JPanel politica = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        politica.add(new JLabel("Política de disco:"));
        politica.add(selectorPolitica);
        politica.add(politicaEnUso);

        JPanel mandos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        mandos.add(new JLabel("Velocidad (posiciones/ciclo):"));
        mandos.add(velocidad);
        mandos.add(detener);
        mandos.add(mover);

        JPanel controles = new JPanel(new GridLayout(2, 1));
        controles.add(politica);
        controles.add(mandos);

        JPanel datos = new JPanel(new GridLayout(2, 1));
        datos.add(cabezal);
        datos.add(estado);

        mapa.setBorder(BorderFactory.createTitledBorder("Mapa de bloques (blanco: libre; color: archivo; violeta: "
                + "proceso suspendido; gris claro: swap libre; borde rojo: cabezal)"));
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(controles, BorderLayout.NORTH);
        norte.add(datos, BorderLayout.CENTER);
        JScrollPane scrollMapa = new JScrollPane(mapa);
        scrollMapa.setBorder(BorderFactory.createEmptyBorder());

        JTable tablaCola = new JTable(cola);
        tablaCola.setFillsViewportHeight(true);
        tablaCola.setPreferredScrollableViewportSize(new Dimension(600, 3 * tablaCola.getRowHeight()));
        tablaCola.getColumnModel().getColumn(2).setPreferredWidth(230);
        JScrollPane scrollCola = new JScrollPane(tablaCola);
        scrollCola.setBorder(BorderFactory.createTitledBorder("Cola de E/S del disco (en orden de llegada)"));

        setBorder(BorderFactory.createTitledBorder("Disco"));
        add(norte, BorderLayout.NORTH);
        add(scrollMapa, BorderLayout.CENTER);
        add(scrollCola, BorderLayout.SOUTH);
        actualizarPermisos();
    }

    public void mostrar(FotoDisco d) {
        ultima = d;
        mapa.mostrar(d);
        ajustando = true;
        modeloPolitica.setSelectedItem(d.politica());
        if ((Integer) velocidad.getValue() != d.velocidad()) {
            velocidad.setValue(d.velocidad());
        }
        ajustando = false;
        politicaEnUso.setText("en uso: " + d.politica());
        String movimiento = mapa.ultimoMovimiento() == null ? "—" : flecha(mapa.ultimoMovimiento());
        cabezal.setText("Cabezal en el bloque " + d.cabezal() + " · movimiento: " + movimiento
                + " · barrido: " + flecha(d.direccion()) + " · desplazamiento total: "
                + d.desplazamientoTotal() + " · archivos: 0 a " + (d.bloquesArchivos() - 1)
                + (d.bloquesSwap() > 0 ? " · swap: " + d.bloquesArchivos() + " a " + (d.totalBloques() - 1) : " · sin swap"));
        FotoSolicitud s = d.enCurso();
        String trabajo = s == null ? "libre" : "atiende a " + s.proceso() + ": " + s.descripcion()
                + (s.cantidadBloques() == 0 ? " (sin bloques)"
                : ", bloque " + Math.min(s.bloquesHechos() + 1, s.cantidadBloques()) + " de " + s.cantidadBloques());
        estado.setText("Estado: " + (d.detenido() ? "detenido (no toma solicitudes)"
                : d.deteniendo() ? "deteniéndose (termina la solicitud en curso)" : trabajo)
                + " · en cola: " + d.cola().length);
        detener.setText(d.detenido() || d.deteniendo() ? "Reanudar disco" : "Detener disco");
        actualizarControlMover();
        cola.setFilas(d.cola());
    }

    //marca en el mapa la cadena de un archivo (-1: ninguno)
    public void resaltar(int primerBloque) {
        mapa.resaltar(primerBloque);
    }

    void actualizarPermisos() {
        sesion.actualizarControlAdministrativo(selectorPolitica, true);
        sesion.actualizarControlAdministrativo(velocidad, true);
        sesion.actualizarControlAdministrativo(detener, true);
        actualizarControlMover();
    }

    private void actualizarControlMover() {
        boolean disponible = ultima != null && ultima.detenido() && ultima.enCurso() == null;
        sesion.actualizarControlAdministrativo(mover, disponible);
    }

    private static String flecha(DireccionCabezal direccion) {
        return direccion == DireccionCabezal.ARRIBA ? "↑" : "↓";
    }

    private void detenerOReanudar() {
        if (!sesion.esAdministrador()) {
            return;
        }
        if (ultima != null && (ultima.detenido() || ultima.deteniendo())) {
            simulacion.reanudarDisco();
        } else {
            simulacion.detenerDisco();
        }
        alCambiar.run();
    }

    //si se baja la velocidad con solicitudes pendientes, se confirma (tardarán más en atenderse)
    private void cambiarVelocidad() {
        if (ajustando || !sesion.esAdministrador() || ultima == null) {
            return;
        }
        int nueva = (Integer) velocidad.getValue();
        int pendientes = simulacion.solicitudesEnDisco();
        if (nueva < ultima.velocidad() && pendientes > 0) {
            int r = JOptionPane.showConfirmDialog(this, "Hay " + pendientes + " solicitudes en el disco. Con menos "
                    + "velocidad el cabezal tarda más en llegar a cada bloque.\n¿Bajar la velocidad de "
                    + ultima.velocidad() + " a " + nueva + "?", "Bajar la velocidad del cabezal", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) {
                ajustando = true;
                velocidad.setValue(ultima.velocidad());
                ajustando = false;
                return;
            }
        }
        simulacion.setVelocidadCabezal(nueva);
        alCambiar.run();
    }

    //con el disco detenido y libre se puede poner el cabezal en otro bloque y cambiar su dirección
    private void moverCabezal() {
        if (!sesion.esAdministrador() || ultima == null) {
            return;
        }
        JSpinner posicion = new JSpinner(new SpinnerNumberModel(ultima.cabezal(), 0, ultima.totalBloques() - 1, 1));
        ModeloLista<DireccionCabezal> modeloDireccion = new ModeloLista<>(DireccionCabezal.values());
        modeloDireccion.setSelectedItem(ultima.direccion());
        JPanel formulario = Formato.formulario();
        Formato.fila(formulario, 0, "Bloque (0 a " + (ultima.totalBloques() - 1) + "):", posicion);
        Formato.fila(formulario, 1, "Dirección:", new JComboBox<>(modeloDireccion));
        if (JOptionPane.showConfirmDialog(this, formulario, "Mover el cabezal", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            simulacion.moverCabezal((Integer) posicion.getValue(), (DireccionCabezal) modeloDireccion.getSelectedItem());
            alCambiar.run();
        } catch (IllegalStateException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo mover el cabezal", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static Object valor(FotoSolicitud s, int columna) {
        return switch (columna) {
            case 0 -> s.proceso();
            case 1 -> s.operacion();
            case 2 -> s.descripcion();
            case 3 -> s.primerBloque() < 0 ? "-" : s.primerBloque();
            case 4 -> s.cantidadBloques();
            default -> s.cicloLlegada();
        };
    }
}