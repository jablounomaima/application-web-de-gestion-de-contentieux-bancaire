@Entity
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDossier;

    private String statut;
    private Double montantCredit;
    private LocalDate dateCreation;

    @ManyToOne
    private Client client;

    @OneToMany(mappedBy = "dossier")
    private List<Risque> risques;

    @OneToMany(mappedBy = "dossier")
    private List<HistoriqueDossier> historiques;

    @OneToMany(mappedBy = "dossier")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "dossier")
    private List<Facture> factures;

    @ManyToOne
    private ValidateurFinancier validateurFinancier;

    @ManyToOne
    private ValidateurJuridique validateurJuridique;
}
