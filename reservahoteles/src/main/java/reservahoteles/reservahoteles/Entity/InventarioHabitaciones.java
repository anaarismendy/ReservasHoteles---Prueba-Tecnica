package reservahoteles.reservahoteles.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "inventario_habitaciones")
public class InventarioHabitaciones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario")
    private Integer idInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hotel", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoHabitacion tipoHabitacion;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal;
}