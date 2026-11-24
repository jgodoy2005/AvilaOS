package avilaos.gui;

import avilaos.so.ModoUsuario;
import javax.swing.JComponent;

/**
 * Modo de uso activo de la interfaz (administrador o usuario) y, en modo usuario, quién es.
 * Lo cambia el selector de la pestaña Simulación y lo leen los botones del sistema de archivos.
 * Es pública para que las pruebas (paquete pruebas) puedan usarla.
 */
public final class Sesion {

    private ModoUsuario modo = ModoUsuario.ADMINISTRADOR;
    private String usuario = "user1";

    public void cambiar(ModoUsuario modo, String usuario) {
        this.modo = modo;
        if (usuario != null) {
            this.usuario = usuario;
        }
    }

    public boolean esAdministrador() {
        return modo == ModoUsuario.ADMINISTRADOR;
    }

    void actualizarControlAdministrativo(JComponent control, boolean disponible) {
        boolean administrador = esAdministrador();
        control.setEnabled(administrador && disponible);
        control.setToolTipText(administrador ? null : "Requiere modo Administrador");
    }

    //la cuenta con la que se lanzan los procesos: "admin" o userN
    public String cuenta() {
        return esAdministrador() ? ModoUsuario.CUENTA_ADMIN : usuario;
    }

    public ModoUsuario getModo() {
        return modo;
    }
}