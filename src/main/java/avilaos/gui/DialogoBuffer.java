/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.Limites;
import avilaos.config.ConfiguracionSimulacion.ConfigBuffer;
import avilaos.so.sincronizacion.BufferAcotado;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 *
 * @author erick
 */
public class DialogoBuffer extends JDialog {

    private final JTextField capacidad = new JTextField("5", 6);
    private final ModeloLista<String> modeloAnfitrion;
    private ConfigBuffer resultado;

    private DialogoBuffer(Window padre, int cantidadComputadores) {
        super(padre, "Nuevo buffer", ModalityType.APPLICATION_MODAL);
        String[] opciones = new String[cantidadComputadores];
        for (int i = 0; i < cantidadComputadores; i++) {
            opciones[i] = "Computador " + (i + 1);
        }
        modeloAnfitrion = new ModeloLista<>(opciones);
        JPanel formulario = Formato.formulario();
        Formato.fila(formulario, 0, "Capacidad (elementos):", capacidad);
        Formato.fila(formulario, 1, "Computador anfitrión:", new JComboBox<>(modeloAnfitrion));
        Formato.fila(formulario, 2, "Memoria:", new JLabel("cada espacio ocupa " + BufferAcotado.MB_POR_ESPACIO
+ " MB del anfitrión"));
formulario.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        JButton crear = new JButton("Crear");
        JButton cancelar = new JButton("Cancelar");
        crear.addActionListener(e -> aceptar());
        cancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(cancelar);
        botones.add(crear);
        getRootPane().setDefaultButton(crear);
        setLayout(new BorderLayout());
        add(formulario, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(padre);
    }

    /**
     * @return capacidad y anfitrión, o null si el usuario canceló
     */
    public static ConfigBuffer pedir(Component padre, int cantidadComputadores) {
        Window ventana = padre == null ? null
                : SwingUtilities.getWindowAncestor(padre);
        DialogoBuffer dialogo = new DialogoBuffer(ventana,
                cantidadComputadores);
        dialogo.setVisible(true);
        return dialogo.resultado;
    }

    private void aceptar() {
        Validacion v = new Validacion();
        int cap = v.entero(capacidad, "La capacidad",
                Limites.CAPACIDAD_BUFFER_MIN, Limites.CAPACIDAD_BUFFER_MAX);
        if (v.hayErrores()) {
            v.mostrar(this);
            return;
        }
        resultado = new ConfigBuffer(cap,
                modeloAnfitrion.indiceSeleccionado() + 1);
        dispose();
    }
}
