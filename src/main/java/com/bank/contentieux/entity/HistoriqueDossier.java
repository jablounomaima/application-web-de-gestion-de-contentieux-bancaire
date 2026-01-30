@Entity
public class HistoriqueDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistorique;

    private LocalDate dateAction;
    private String action;

    @ManyToOne
    private Dossier dossier;
}
