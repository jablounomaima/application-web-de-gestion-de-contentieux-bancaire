@Entity
public class Garantie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idGarantie;

    private String type;
    private Double valeur;

    @ManyToOne
    private Risque risque;
}
