package Controladores;

import Modelos.Recordatorio;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecordatorioControlador {

    private Connection conn;
    public List<Recordatorio> recordatorios = new ArrayList<>();
    private int usuario;

    {
        iniciarHiloProcesamiento();
    }

    public RecordatorioControlador(Connection conn, int usuario) {
        this.conn = conn;
        this.usuario = usuario;
    }

    public boolean crearRecordatorio(Recordatorio recordatorio) throws SQLException {
        String sql = """
        INSERT INTO RECORDATORIOS ( ID_EVE, MIN_ANT_REC, ACT_REC, ID_USU)
        VALUES ( ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recordatorio.getIdEvento());
            ps.setInt(2, recordatorio.getMinutosAntes());
            ps.setBoolean(3, recordatorio.isActivo());
            ps.setInt(4, usuario);

            int filasInsertadas = ps.executeUpdate();
            return filasInsertadas > 0;
        }
    }

    public List<Recordatorio> obtenerRecordatoriosActivos() throws SQLException {
        List<Recordatorio> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM RECORDATORIOS WHERE ACT_REC = TRUE AND ID_USU = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Recordatorio r = new Recordatorio();
                r.setId(rs.getInt("ID_REC"));
                r.setIdEvento(rs.getString("ID_EVE"));
                r.setMinutosAntes(rs.getInt("MIN_ANT_REC"));
                r.setActivo(rs.getBoolean("ACT_REC"));
                lista.add(r);
            }
        }
        return lista;
    }

    private void iniciarHiloProcesamiento() {
        Thread hilo = new Thread(() -> {
            while (true) {
                try {
                    procesarRecordatorios();
                    Thread.sleep(1000); // cada 1 segundo (ajusta si eran 60)
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private void procesarRecordatorios() throws SQLException {
        eliminarRecordatoriosPasados();
        activarRecordatoriosCercanos();
    }

    private void eliminarRecordatoriosPasados() throws SQLException {
        String sql = """
        DELETE FROM RECORDATORIOS
        WHERE ID_EVE IN (
            SELECT ID_EVE FROM EVENTOS
            WHERE FEC_EVE + HOR_EVE < CURRENT_TIMESTAMP
        ) AND ID_USU = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario);
            ps.executeUpdate();
        }
    }

    private void activarRecordatoriosCercanos() throws SQLException {
        String sql = """
        UPDATE RECORDATORIOS
        SET ACT_REC = TRUE
        FROM EVENTOS
        WHERE RECORDATORIOS.ID_EVE = EVENTOS.ID_EVE
          AND RECORDATORIOS.ID_USU = ?
          AND RECORDATORIOS.ACT_REC = FALSE
          AND (EVENTOS.FEC_EVE + EVENTOS.HOR_EVE) >= CURRENT_TIMESTAMP
          AND (EVENTOS.FEC_EVE + EVENTOS.HOR_EVE) <= CURRENT_TIMESTAMP + (RECORDATORIOS.MIN_ANT_REC || ' minutes')::INTERVAL
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario);
            ps.executeUpdate();
        }
    }

    public List<String> obtenerRecordatoriosProximos() throws SQLException {
        List<String> proximos = new ArrayList<>();
        String sql = """
            SELECT R.ID_REC, E.TIT_EVE, E.FEC_EVE, E.HOR_EVE, R.MIN_ANT_REC
            FROM RECORDATORIOS R
            JOIN EVENTOS E ON R.ID_EVE = E.ID_EVE
            WHERE R.ACT_REC = TRUE AND R.ID_USU = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date fecha = rs.getDate("FEC_EVE");
                Time hora = rs.getTime("HOR_EVE");
                int minAnt = rs.getInt("MIN_ANT_REC");

                Timestamp fechaHoraEvento = Timestamp.valueOf(fecha.toString() + " " + hora.toString());
                Timestamp ahora = new Timestamp(System.currentTimeMillis());

                long minutosFaltantes = (fechaHoraEvento.getTime() - ahora.getTime()) / 60000;

                if (minutosFaltantes <= minAnt && minutosFaltantes >= 0) {
                    proximos.add(rs.getString("TIT_EVE") + " en " + minutosFaltantes + " minutos");
                }
            }
        }
        return proximos;
    }

    public boolean eliminarRecordatorio(int idRec) throws SQLException {
        String sql = "DELETE FROM RECORDATORIOS WHERE ID_REC = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRec);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Recordatorio> obtenerRecordatoriosEvento(String idEvento) {
        List<Recordatorio> recordatorios = new ArrayList<>();
        try {
            String sql = "SELECT ID_REC, MIN_ANT_REC, ACT_REC FROM RECORDATORIOS WHERE ID_EVE = ? AND ID_USU = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, idEvento);
                ps.setInt(2, usuario);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idRec = rs.getInt("ID_REC");
                        int minutosAntes = rs.getInt("MIN_ANT_REC");
                        boolean activo = rs.getBoolean("ACT_REC");
                        Recordatorio rec = new Recordatorio(idEvento, minutosAntes, activo, usuario);
                        rec.setId(idRec);
                        recordatorios.add(rec);
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al obtener recordatorios: " + ex.getMessage());
            ex.printStackTrace();
        }
        return recordatorios;
    }

    public void cargarRecordatorios() {
        recordatorios.clear();

        String sql = """
            SELECT R.ID_REC, R.ID_EVE, R.MIN_ANT_REC,
                   E.TIT_EVE, E.FEC_EVE, E.HOR_EVE, E.DES_EVE
            FROM RECORDATORIOS R
            JOIN EVENTOS E ON R.ID_EVE = E.ID_EVE
            WHERE R.ACT_REC = TRUE AND R.ID_USU = ?
            ORDER BY E.FEC_EVE, E.HOR_EVE
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Recordatorio rec = new Recordatorio();
                rec.setId(rs.getInt("ID_REC"));
                rec.setIdEvento(rs.getString("ID_EVE"));
                rec.setMinutosAntes(rs.getInt("MIN_ANT_REC"));
                rec.setTituloEvento(rs.getString("TIT_EVE"));
                rec.setFechaEvento(rs.getDate("FEC_EVE"));
                rec.setHoraEvento(rs.getTime("HOR_EVE"));
                rec.setDescripcionEvento(rs.getString("DES_EVE"));

                recordatorios.add(rec);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
