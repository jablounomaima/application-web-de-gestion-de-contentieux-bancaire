@Entity
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMission;

    private String statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    @ManyToOne
    private Prestation prestation;

    @ManyToOne
    private Expert expert;

    @ManyToOne
    private HuissierDeJustice huissier;

    @OneToOne
    private Resultat resultat;

    @ManyToOne
    private Affaire affaire;
}
