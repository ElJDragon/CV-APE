package Vistas;

import Controladores.EventoControlador;
import Modelos.Evento;
import Modelos.SesionUsuario;
import com.toedter.calendar.JCalendar;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class AgendaPersonalizada extends JFrame {

    private EventoControlador controlador;
    private Evento eventoSeleccionado;

    // Componentes de la interfaz
    private JCalendar calendar;
    private JSpinner timeSpinner;
    private JTextField txtTitulo, txtDescripcion, txtUsuario;
    private JButton btnAgregar, btnEditar, btnEliminar;
    private JLabel lblContador;
    private JPanel panelEventos;

    public AgendaPersonalizada() {
        super("Agenda de Eventos");
        controlador = new EventoControlador();
        configurarInterfaz();
        cargarEventosUsuario();
    }

    private void configurarInterfaz() {
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel superior con información del usuario
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtUsuario = new JTextField(20);
        txtUsuario.setEditable(false);
        if (SesionUsuario.sesionActiva()) {
            txtUsuario.setText(SesionUsuario.getUsuarioActual().getNombre() + " "
                    + SesionUsuario.getUsuarioActual().getApellido());
        }
        panelSuperior.add(new JLabel("Usuario:"));
        panelSuperior.add(txtUsuario);
        add(panelSuperior, BorderLayout.NORTH);

        // Panel central con calendario y formulario
        JPanel panelCentral = new JPanel(new GridLayout(1, 2, 10, 10));

        // Panel del calendario
        JPanel panelCalendario = new JPanel(new BorderLayout());
        calendar = new JCalendar();
        panelCalendario.add(calendar, BorderLayout.CENTER);
        panelCalendario.setBorder(new TitledBorder("Seleccione una fecha"));

        // Panel del formulario
        JPanel panelFormulario = new JPanel();
        panelFormulario.setLayout(new BoxLayout(panelFormulario, BoxLayout.Y_AXIS));
        panelFormulario.setBorder(new TitledBorder("Detalles del Evento"));

        txtTitulo = new JTextField();
        txtDescripcion = new JTextField();

        // Configurar spinner de hora
        timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setValue(new Date());

        panelFormulario.add(new JLabel("Título:"));
        panelFormulario.add(txtTitulo);
        panelFormulario.add(Box.createRigidArea(new Dimension(0, 10)));
        panelFormulario.add(new JLabel("Descripción:"));
        panelFormulario.add(txtDescripcion);
        panelFormulario.add(Box.createRigidArea(new Dimension(0, 10)));
        panelFormulario.add(new JLabel("Hora:"));
        panelFormulario.add(timeSpinner);
        panelFormulario.add(Box.createRigidArea(new Dimension(0, 20)));

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnAgregar = new JButton("Agregar");
        btnEditar = new JButton("Editar");
        btnEliminar = new JButton("Eliminar");

        btnAgregar.addActionListener(e -> agregarEvento());
        btnEditar.addActionListener(e -> editarEvento());
        btnEliminar.addActionListener(e -> eliminarEvento());

        btnEditar.setVisible(false);
        btnEliminar.setVisible(false);

        panelBotones.add(btnAgregar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);

        panelFormulario.add(panelBotones);

        panelCentral.add(panelCalendario);
        panelCentral.add(panelFormulario);
        add(panelCentral, BorderLayout.CENTER);

        // Panel inferior con lista de eventos
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBorder(new TitledBorder("Mis Eventos"));

        lblContador = new JLabel("Total eventos: 0");
        panelInferior.add(lblContador, BorderLayout.NORTH);

        panelEventos = new JPanel();
        panelEventos.setLayout(new BoxLayout(panelEventos, BoxLayout.Y_AXIS));
        JScrollPane scrollEventos = new JScrollPane(panelEventos);
        panelInferior.add(scrollEventos, BorderLayout.CENTER);

        add(panelInferior, BorderLayout.SOUTH);
    }

    private void agregarEvento() {
        String titulo = txtTitulo.getText().trim();
        LocalDate fecha = calendar.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        if (titulo.isEmpty() || fecha == null) {
            JOptionPane.showMessageDialog(this, "Título y fecha son obligatorios",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Evento nuevoEvento = new Evento();
        // Usar USE_USU en lugar de ID_USU si esa es la relación
        nuevoEvento.setIdUsuario(String.valueOf(SesionUsuario.getUsuarioActual().getId()));// getUsername() devuelve USE_USU
        nuevoEvento.setTitulo(titulo);
        nuevoEvento.setDescripcion(txtDescripcion.getText().trim());
        nuevoEvento.setFecha(fecha);
        nuevoEvento.setHora(((Date) timeSpinner.getValue()).toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime());

        if (controlador.crear(nuevoEvento)) {
            JOptionPane.showMessageDialog(this, "Evento creado exitosamente",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarCampos();
            cargarEventosUsuario();
        } else {
            JOptionPane.showMessageDialog(this, "Error al crear evento",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarEvento() {
        if (eventoSeleccionado == null) {
            return;
        }

        eventoSeleccionado.setTitulo(txtTitulo.getText().trim());
        eventoSeleccionado.setDescripcion(txtDescripcion.getText().trim());
        eventoSeleccionado.setHora(((Date) timeSpinner.getValue()).toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime());

        if (controlador.actualizar(eventoSeleccionado)) {
            JOptionPane.showMessageDialog(this, "Evento actualizado",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarCampos();
            cargarEventosUsuario();
        } else {
            JOptionPane.showMessageDialog(this, "Error al actualizar",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarEvento() {
        if (eventoSeleccionado == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este evento?", "Confirmar", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION && controlador.eliminar(eventoSeleccionado.getIdEvento())) {
            JOptionPane.showMessageDialog(this, "Evento eliminado",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarCampos();
            cargarEventosUsuario();
        }
    }

    private void cargarEventosUsuario() {
        panelEventos.removeAll();

        if (!SesionUsuario.sesionActiva()) {
            panelEventos.add(new JLabel("No hay sesión activa"));
            panelEventos.revalidate();
            return;
        }

        try {
            // Obtener el ID del usuario como string (como estaba originalmente)
            int idUsuario = SesionUsuario.getUsuarioActual().getId();

            List<Evento> eventos = controlador.obtenerPorUsuario(idUsuario);
            lblContador.setText("Total eventos: " + eventos.size());

            if (eventos.isEmpty()) {
                panelEventos.add(new JLabel("No hay eventos registrados"));
            } else {
                for (Evento evento : eventos) {
                    JPanel panelEvento = new JPanel(new BorderLayout());
                    panelEvento.setBorder(BorderFactory.createEtchedBorder());

                    JLabel lblInfo = new JLabel(
                            "<html><b>" + evento.getTitulo() + "</b><br>"
                            + "Fecha: " + evento.getFecha() + " - Hora: " + evento.getHora()
                            + "</html>");

                    panelEvento.add(lblInfo, BorderLayout.CENTER);
                    panelEventos.add(panelEvento);
                    panelEventos.add(Box.createRigidArea(new Dimension(0, 5)));
                }
            }

            panelEventos.revalidate();
            panelEventos.repaint();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar eventos: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel crearPanelEvento(Evento evento) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(5, 5, 5, 5)
        ));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel lblFecha = new JLabel("Fecha: " + evento.getFecha() + " - Hora: " + evento.getHora());
        JLabel lblTitulo = new JLabel("Título: " + evento.getTitulo());
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD));

        panel.add(lblFecha);
        panel.add(lblTitulo);

        if (!evento.getDescripcion().isEmpty()) {
            JLabel lblDesc = new JLabel("Descripción: " + evento.getDescripcion());
            panel.add(lblDesc);
        }

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarEvento(evento);
            }
        });

        return panel;
    }

    private void seleccionarEvento(Evento evento) {
        eventoSeleccionado = evento;
        txtTitulo.setText(evento.getTitulo());
        txtDescripcion.setText(evento.getDescripcion());
        timeSpinner.setValue(Date.from(evento.getHora().atDate(LocalDate.now())
                .atZone(ZoneId.systemDefault())
                .toInstant()));

        btnAgregar.setVisible(false);
        btnEditar.setVisible(true);
        btnEliminar.setVisible(true);
    }

    private void limpiarCampos() {
        txtTitulo.setText("");
        txtDescripcion.setText("");
        timeSpinner.setValue(new Date());
        eventoSeleccionado = null;

        btnAgregar.setVisible(true);
        btnEditar.setVisible(false);
        btnEliminar.setVisible(false);
    }
}
