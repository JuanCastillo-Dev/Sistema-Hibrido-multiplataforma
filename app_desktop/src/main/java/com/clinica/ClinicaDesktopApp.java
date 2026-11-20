package com.clinica;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class ClinicaDesktopApp extends JFrame {
    private final SshTunnelManager sshManager = new SshTunnelManager();
    private CitaDao citaDao;

    private JComboBox<CitaDao.MascotaItem> comboMascota;
    private JTextField txtMotivo;
    private JTextField txtFechaHora;
    private JComboBox<String> comboEstado;
    private JTable tablaCitas;
    private DefaultTableModel modeloTabla;
    private Integer idCitaSeleccionada = null;

    public ClinicaDesktopApp() {
        setTitle("Clínica Veterinaria - App Escritorio (SSH + JDBC)");
        setSize(950, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        initComponents();
        initBackend();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarRecursosYSalir();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel panelForm = new JPanel(new GridLayout(2, 4, 8, 8));
        panelForm.setBorder(BorderFactory.createTitledBorder("Datos de la Cita"));

        comboMascota = new JComboBox<>();
        txtMotivo = new JTextField();
        txtFechaHora = new JTextField("2026-11-21 10:00:00");
        comboEstado = new JComboBox<>(new String[]{"Programada", "Atendida", "Cancelada"});

        panelForm.add(new JLabel("Mascota:"));
        panelForm.add(comboMascota);
        panelForm.add(new JLabel("Estado:"));
        panelForm.add(comboEstado);
        panelForm.add(new JLabel("Motivo:"));
        panelForm.add(txtMotivo);
        panelForm.add(new JLabel("Fecha y hora (YYYY-MM-DD HH:MM:SS):"));
        panelForm.add(txtFechaHora);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton btnCrear = new JButton("Crear");
        JButton btnActualizar = new JButton("Actualizar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnRefrescar = new JButton("Actualizar / Refrescar tabla");
        JButton btnLimpiar = new JButton("Limpiar");

        panelBotones.add(btnCrear);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnRefrescar);
        panelBotones.add(btnLimpiar);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(panelForm, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);
        add(panelSuperior, BorderLayout.NORTH);

        modeloTabla = new DefaultTableModel(
            new String[]{"ID Cita", "ID Mascota", "Mascota", "Propietario", "Motivo", "Fecha y Hora", "Estado"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaCitas = new JTable(modeloTabla);
        add(new JScrollPane(tablaCitas), BorderLayout.CENTER);

        tablaCitas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablaCitas.getSelectedRow() != -1) {
                int row = tablaCitas.getSelectedRow();
                idCitaSeleccionada = (Integer) modeloTabla.getValueAt(row, 0);
                int idMascota = (Integer) modeloTabla.getValueAt(row, 1);
                seleccionarMascotaPorId(idMascota);
                txtMotivo.setText(String.valueOf(modeloTabla.getValueAt(row, 4)));
                txtFechaHora.setText(String.valueOf(modeloTabla.getValueAt(row, 5)));
                comboEstado.setSelectedItem(String.valueOf(modeloTabla.getValueAt(row, 6)));
            }
        });

        btnCrear.addActionListener(e -> crearCita());
        btnActualizar.addActionListener(e -> actualizarCita());
        btnEliminar.addActionListener(e -> eliminarCita());
        btnRefrescar.addActionListener(e -> cargarDatos());
        btnLimpiar.addActionListener(e -> limpiarFormulario());
    }

    private void initBackend() {
        try {
            ConfigManager config = new ConfigManager();
            int localPort = sshManager.openTunnel(config);
            citaDao = new CitaDao(config, localPort);
            cargarDatos();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error al iniciar túnel SSH o conexión JDBC:\n" + ex.getMessage(),
                "Error de conexión",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void cargarDatos() {
        if (citaDao == null) return;
        try {
            CitaDao.MascotaItem mascotaPrevia = (CitaDao.MascotaItem) comboMascota.getSelectedItem();
            Integer idPrevio = mascotaPrevia != null ? mascotaPrevia.id : null;

            List<CitaDao.MascotaItem> mascotas = citaDao.listarMascotas();
            comboMascota.removeAllItems();
            for (CitaDao.MascotaItem m : mascotas) {
                comboMascota.addItem(m);
            }
            if (idPrevio != null) {
                seleccionarMascotaPorId(idPrevio);
            }

            modeloTabla.setRowCount(0);
            for (Object[] fila : citaDao.listarCitas()) {
                modeloTabla.addRow(fila);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarMascotaPorId(int idMascota) {
        for (int i = 0; i < comboMascota.getItemCount(); i++) {
            CitaDao.MascotaItem item = comboMascota.getItemAt(i);
            if (item.id == idMascota) {
                comboMascota.setSelectedIndex(i);
                break;
            }
        }
    }

    private void crearCita() {
        if (citaDao == null) return;
        CitaDao.MascotaItem mascota = (CitaDao.MascotaItem) comboMascota.getSelectedItem();
        String motivo = txtMotivo.getText().trim();
        String fecha = txtFechaHora.getText().trim();
        String estado = (String) comboEstado.getSelectedItem();

        if (mascota == null || motivo.isEmpty() || fecha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completa mascota, motivo y fecha/hora.");
            return;
        }

        try {
            citaDao.crearCita(mascota.id, motivo, fecha, estado);
            cargarDatos();
            limpiarFormulario();
            JOptionPane.showMessageDialog(this, "Cita creada correctamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear cita: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarCita() {
        if (citaDao == null || idCitaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una cita de la tabla para actualizar.");
            return;
        }
        CitaDao.MascotaItem mascota = (CitaDao.MascotaItem) comboMascota.getSelectedItem();
        String motivo = txtMotivo.getText().trim();
        String fecha = txtFechaHora.getText().trim();
        String estado = (String) comboEstado.getSelectedItem();

        try {
            citaDao.actualizarCita(idCitaSeleccionada, mascota.id, motivo, fecha, estado);
            cargarDatos();
            limpiarFormulario();
            JOptionPane.showMessageDialog(this, "Cita actualizada correctamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar cita: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarCita() {
        if (citaDao == null || idCitaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una cita de la tabla para eliminar.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
            this, "¿Eliminar la cita ID " + idCitaSeleccionada + "?", "Confirmar", JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            citaDao.eliminarCita(idCitaSeleccionada);
            cargarDatos();
            limpiarFormulario();
            JOptionPane.showMessageDialog(this, "Cita eliminada correctamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar cita: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        idCitaSeleccionada = null;
        txtMotivo.setText("");
        comboEstado.setSelectedIndex(0);
        tablaCitas.clearSelection();
    }

    private void cerrarRecursosYSalir() {
        if (citaDao != null) {
            citaDao.closeConnection();
        }
        sshManager.closeTunnel();
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClinicaDesktopApp().setVisible(true));
    }
}