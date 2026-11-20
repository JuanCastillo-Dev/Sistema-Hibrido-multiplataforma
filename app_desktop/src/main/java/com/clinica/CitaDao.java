package com.clinica;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CitaDao {
    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;
    private Connection connection;

    public CitaDao(ConfigManager config, int localTunnelPort) throws SQLException {
        String dbName = config.get("db.name");
        this.dbUser = config.get("db.user");
        this.dbPassword = config.get("db.password");
        this.jdbcUrl = "jdbc:mysql://127.0.0.1:" + localTunnelPort + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        this.connection = DriverManager.getConnection(this.jdbcUrl, this.dbUser, this.dbPassword);
    }

    public static class MascotaItem {
        public final int id;
        public final String etiqueta;

        public MascotaItem(int id, String etiqueta) {
            this.id = id;
            this.etiqueta = etiqueta;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    public List<MascotaItem> listarMascotas() throws SQLException {
        List<MascotaItem> lista = new ArrayList<>();
        String sql = "SELECT id_mascota, nombre, especie, propietario FROM mascotas ORDER BY nombre ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_mascota");
                String label = id + " - " + rs.getString("nombre")
                        + " (" + rs.getString("especie") + ") - " + rs.getString("propietario");
                lista.add(new MascotaItem(id, label));
            }
        }
        return lista;
    }

    public List<Object[]> listarCitas() throws SQLException {
        List<Object[]> filas = new ArrayList<>();
        String sql = "SELECT c.id_cita, c.id_mascota, m.nombre, m.propietario, c.motivo, c.fecha_cita, c.estado "
                   + "FROM citas c INNER JOIN mascotas m ON c.id_mascota = m.id_mascota "
                   + "ORDER BY c.fecha_cita DESC, c.id_cita DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{
                    rs.getInt("id_cita"),
                    rs.getInt("id_mascota"),
                    rs.getString("nombre"),
                    rs.getString("propietario"),
                    rs.getString("motivo"),
                    rs.getTimestamp("fecha_cita").toString(),
                    rs.getString("estado")
                });
            }
        }
        return filas;
    }

    public void crearCita(int idMascota, String motivo, String fechaCita, String estado) throws SQLException {
        String sql = "INSERT INTO citas (id_mascota, motivo, fecha_cita, estado) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idMascota);
            ps.setString(2, motivo);
            ps.setString(3, fechaCita);
            ps.setString(4, estado);
            ps.executeUpdate();
        }
    }

    public void actualizarCita(int idCita, int idMascota, String motivo, String fechaCita, String estado) throws SQLException {
        String sql = "UPDATE citas SET id_mascota = ?, motivo = ?, fecha_cita = ?, estado = ? WHERE id_cita = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idMascota);
            ps.setString(2, motivo);
            ps.setString(3, fechaCita);
            ps.setString(4, estado);
            ps.setInt(5, idCita);
            ps.executeUpdate();
        }
    }

    public void eliminarCita(int idCita) throws SQLException {
        String sql = "DELETE FROM citas WHERE id_cita = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            ps.executeUpdate();
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }
}