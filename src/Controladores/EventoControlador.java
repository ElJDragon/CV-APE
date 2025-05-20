package Controladores;

import BD.ConexionBD;
import Modelos.Evento;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventoControlador {

    private ConexionBD conn;

    public EventoControlador() {
        conn = new ConexionBD();
    }

    public boolean crear(Evento evento) {
        String sql = "INSERT INTO EVENTOS (ID_EVE, ID_USU, TIT_EVE, DES_EVE, FEC_EVE, HOR_EVE) "
                + "VALUES (?, CAST(? AS INTEGER), ?, ?, ?, ?)";

        try ( Connection conexion = conn.conectar();  PreparedStatement stmt = conexion.prepareStatement(sql)) {

            stmt.setString(1, generarNuevoIdEvento());
            stmt.setInt(2, Integer.parseInt(evento.getIdUsuario())); // Convertir a entero
            stmt.setString(3, evento.getTitulo());
            stmt.setString(4, evento.getDescripcion());
            stmt.setDate(5, Date.valueOf(evento.getFecha()));
            stmt.setTime(6, Time.valueOf(evento.getHora()));

            return stmt.executeUpdate() > 0;
        } catch (NumberFormatException e) {
            System.err.println("Error: El ID de usuario debe ser numérico");
            return false;
        } catch (SQLException e) {
            System.err.println("Error al crear evento: " + e.getMessage());
            return false;
        }
    }

    private boolean usuarioExiste(String useUsu) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE USE_USU = ?";
        try ( Connection conexion = conn.conectar();  PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, useUsu);
            return stmt.executeQuery().next();
        }
    }

    public List<Evento> obtenerPorUsuario(int idUsuario) {
        List<Evento> eventos = new ArrayList<>();

        // Consulta modificada para hacer casting explícito
        String sql = "SELECT * FROM EVENTOS WHERE ID_USU = CAST(? AS INTEGER) ORDER BY FEC_EVE DESC, HOR_EVE DESC";

        try ( Connection conexion = conn.conectar();  PreparedStatement stmt = conexion.prepareStatement(sql)) {

            // Convertir el String a int para asegurar compatibilidad
            stmt.setInt(1, idUsuario);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Evento evento = new Evento();
                evento.setIdEvento(rs.getString("ID_EVE"));
                evento.setIdUsuario(String.valueOf(rs.getInt("ID_USU"))); // Convertir a String
                evento.setTitulo(rs.getString("TIT_EVE"));
                evento.setDescripcion(rs.getString("DES_EVE"));
                evento.setFecha(rs.getDate("FEC_EVE").toLocalDate());
                evento.setHora(rs.getTime("HOR_EVE").toLocalTime());
                eventos.add(evento);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: El ID de usuario debe ser numérico");
        } catch (SQLException e) {
            System.err.println("Error al obtener eventos por usuario: " + e.getMessage());
        }
        return eventos;
    }

    public String generarNuevoIdEvento() {
        String sql = "SELECT MAX(ID_EVE) AS ultimo_id FROM EVENTOS";

        try ( Connection conexion = conn.conectar();  Statement stmt = conexion.createStatement();  ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String ultimoId = rs.getString("ultimo_id");
                if (ultimoId != null) {
                    int numero = Integer.parseInt(ultimoId.substring(2));
                    return ""+ (numero + 1);
                }
            }
            return "EV1";
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Error al generar nuevo ID: " + e.getMessage());
            return "EV" + System.currentTimeMillis();
        }
    }

    public boolean eliminar(String idEvento) {
        String sql = "DELETE FROM EVENTOS WHERE ID_EVE = ?";

        try ( Connection conexion = conn.conectar();  PreparedStatement stmt = conexion.prepareStatement(sql)) {

            stmt.setString(1, idEvento);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar evento: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Evento evento) {
        String sql = "UPDATE EVENTOS SET TIT_EVE = ?, DES_EVE = ?, FEC_EVE = ?, HOR_EVE = ? WHERE ID_EVE = ?";

        try ( Connection conexion = conn.conectar();  PreparedStatement stmt = conexion.prepareStatement(sql)) {

            stmt.setString(1, evento.getTitulo());
            stmt.setString(2, evento.getDescripcion());
            stmt.setDate(3, Date.valueOf(evento.getFecha()));
            stmt.setTime(4, Time.valueOf(evento.getHora()));
            stmt.setString(5, evento.getIdEvento());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar evento: " + e.getMessage());
            return false;
        }
    }
}
