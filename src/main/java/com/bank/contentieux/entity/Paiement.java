@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPaiement;

    private LocalDate datePaiement;
    private Double montant;
}
