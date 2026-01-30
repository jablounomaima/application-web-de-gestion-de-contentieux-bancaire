@Entity
public class Prestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPrestation;

    private String nomPrestation;
    private String description;
    private Double prix;
    private LocalDate dateCreation;
    private String statut;

    @ManyToOne
    private Utilisateur creePar;

    @OneToMany(mappedBy = "prestation")
    private List<Mission> missions;
}
