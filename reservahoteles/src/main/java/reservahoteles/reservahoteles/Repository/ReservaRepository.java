package reservahoteles.reservahoteles.Repository;

import reservahoteles.reservahoteles.Entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer>,
        ReservaCustomRepository {
}
