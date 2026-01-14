package reservahoteles.reservahoteles.Repository;

import reservahoteles.reservahoteles.DTO.CalculoPrecioRequest;
import reservahoteles.reservahoteles.DTO.CrearReservaRequest;
import reservahoteles.reservahoteles.DTO.CalculoPrecioResponse;
import reservahoteles.reservahoteles.DTO.CrearReservaResponse;
import reservahoteles.reservahoteles.DTO.DisponibilidadResponse;
import reservahoteles.reservahoteles.DTO.TarifaResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReservaCustomRepositoryImpl implements ReservaCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DisponibilidadResponse> verificarDisponibilidad(Integer idHotel, Integer idTipo, 
                                                                  LocalDate fechaInicio, LocalDate fechaFin) {
        // Usar consulta SQL nativa para llamar al stored procedure con sintaxis PostgreSQL
        // Nota: Verifica en Supabase el nombre exacto de la función que creaste
        // Puede ser: verificar_disponibilidad_pool o verificar_disponibilidad_pool_simple
        String sql = "SELECT * FROM verificar_disponibilidad_pool(?, ?, ?::date, ?::date)";
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter(1, idHotel)
            .setParameter(2, idTipo)
            .setParameter(3, fechaInicio.toString())
            .setParameter(4, fechaFin.toString());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<DisponibilidadResponse> responses = new ArrayList<>();
        
        for (Object[] row : results) {
            // Mapeo seguro de resultados de la función
            // La función retorna: (tipo_habitacion, cantidad_total, cantidad_disponible, capacidad_personas)
            DisponibilidadResponse response = new DisponibilidadResponse(
                row[0] != null ? row[0].toString() : "Desconocido",           // tipo_habitacion
                row[1] != null ? ((Number) row[1]).intValue() : 0,           // cantidad_total
                row[2] != null ? ((Number) row[2]).intValue() : 0,           // cantidad_disponible
                row[3] != null ? ((Number) row[3]).intValue() : 0            // capacidad_personas
            );
            responses.add(response);
        }
        
        return responses;
    }

    @Override
    public List<TarifaResponse> obtenerTarifas(Integer idHotel, Integer idTipo, LocalDate fechaInicio) {
        // Intentar usar la función consultar_tarifas si existe, sino usar consulta SQL directa
        // Nota: Se hace cast explícito de fechas por si las columnas son VARCHAR
        String sql = "SELECT * FROM consultar_tarifas(?, ?, ?::date)";
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter(1, idHotel)
            .setParameter(2, idTipo)
            .setParameter(3, fechaInicio.toString());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<TarifaResponse> responses = new ArrayList<>();
        
        for (Object[] row : results) {
            TarifaResponse response = new TarifaResponse(
                row[0] != null ? ((Number) row[0]).intValue() : null,           // id_tarifa
                row[1] != null ? row[1].toString() : null,                      // hotel
                row[2] != null ? row[2].toString() : null,                      // tipo_habitacion
                row[3] != null ? row[3].toString() : null,                      // temporada
                row[4] != null ? new BigDecimal(row[4].toString()) : null,      // precio_base_noche
                row[5] != null ? new BigDecimal(row[5].toString()) : null      // precio_persona_adicional
            );
            responses.add(response);
        }
        
        return responses;
    }

    @Override
    public CalculoPrecioResponse calcularPrecio(CalculoPrecioRequest request) {
        // Usar consulta SQL nativa para llamar al stored procedure con sintaxis PostgreSQL
        // Nota: Se convierte el JSONB a texto para evitar problemas de mapeo
        String sql = "SELECT " +
                    "precio_total, " +
                    "precio_por_noche, " +
                    "numero_noches, " +
                    "temporada, " +
                    "desglose::text " +
                    "FROM calcular_precio_reserva(?, ?, ?::date, ?::date, ?, ?)";
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter(1, request.getIdHotel())
            .setParameter(2, request.getIdTipo())
            .setParameter(3, request.getFechaInicio().toString())
            .setParameter(4, request.getFechaFin().toString())
            .setParameter(5, request.getCantidadHabitaciones())
            .setParameter(6, request.getNumeroPersonas());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            return new CalculoPrecioResponse(
                row[0] != null ? new BigDecimal(row[0].toString()) : null,   // precio_total
                row[1] != null ? new BigDecimal(row[1].toString()) : null,   // precio_por_noche
                row[2] != null ? ((Number) row[2]).intValue() : null,      // numero_noches
                row[3] != null ? row[3].toString() : null,       // temporada
                row[4] != null ? row[4].toString() : null      // desglose (como texto)
            );
        }
        
        return null;
    }

    @Override
    public CrearReservaResponse crearReserva(CrearReservaRequest request) {
        // Usar consulta SQL nativa para llamar al stored procedure con sintaxis PostgreSQL
        String sql = "SELECT * FROM crear_reserva(?, ?, ?::date, ?::date, ?, ?)";
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter(1, request.getIdHotel())
            .setParameter(2, request.getIdTipo())
            .setParameter(3, request.getFechaInicio().toString())
            .setParameter(4, request.getFechaFin().toString())
            .setParameter(5, request.getNumeroPersonas())
            .setParameter(6, request.getCantidadHabitaciones());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            return new CrearReservaResponse(
                (Integer) row[0],      // id_reserva
                (Boolean) row[1],      // exito
                (String) row[2],       // mensaje
                (BigDecimal) row[3]    // total_calculado
            );
        }
        
        return new CrearReservaResponse(null, false, "Error al crear reserva", BigDecimal.ZERO);
    }
}