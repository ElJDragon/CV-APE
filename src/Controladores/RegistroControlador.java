package Controladores;

import BD.ConexionBD;
import Modelos.Usuario_Model;
import Vistas.registro_View;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import javax.swing.JOptionPane;

public class RegistroControlador implements ActionListener {

    private final registro_View vista;
    private final Connection cc;

    public RegistroControlador(registro_View vista) {
        this.vista = vista;
        this.cc = new ConexionBD().conectar();
        agregarListeners();
        vista.setLocationRelativeTo(null); // Centrar ventana
    }

    private void agregarListeners() {
        vista.jBtnGuardar.setActionCommand("GUARDAR");
        vista.jBtnGuardar.addActionListener(this);

        vista.jBtnReiniciar.addActionListener(e -> limpiarCampos());

        vista.jBtnCancelar.addActionListener(e -> vista.dispose());

        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("GUARDAR".equals(e.getActionCommand())) {
            Usuario_Model usuario = new Usuario_Model(
                    vista.jTxtUser.getText().trim(),
                    vista.jTxtPass.getText().trim(),
                    vista.jTxtNombre.getText().trim(),
                    vista.jTxtApellido.getText().trim()
            );

            if (registrarUsuario(usuario)) {
                JOptionPane.showMessageDialog(vista, "✅ Usuario registrado exitosamente.");
                
                vista.dispose();
            } else {
                JOptionPane.showMessageDialog(vista, "❌ Error al registrar usuario. Verifica los datos o intenta de nuevo.");
            }

        }
    }

    private boolean registrarUsuario(Usuario_Model usuario) {
        System.out.println("Registrando");
        String sql = "INSERT INTO USUARIOS (USE_USU, PAS_USU, NOM_USU, APE_USU) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cc.prepareStatement(sql)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPassword());
            ps.setString(3, usuario.getNombre());
            ps.setString(4, usuario.getApellido());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void limpiarCampos() {
        vista.jTxtUser.setText("");
        vista.jTxtPass.setText("");
        vista.jTxtNombre.setText("");
        vista.jTxtApellido.setText("");
    }
}
