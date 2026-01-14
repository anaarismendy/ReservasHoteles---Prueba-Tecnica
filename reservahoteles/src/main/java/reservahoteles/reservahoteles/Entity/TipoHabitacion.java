package reservahoteles.reservahoteles.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipos_habitacion")
public class TipoHabitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo")
    private Integer idTipo;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "capacidad_personas", nullable = false)

    private Integer capacidadPersonas;
    
    private String descripcion;
}
