@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotification;

    private String message;
    private LocalDate dateEnvoi;
    private Boolean lu;

    @ManyToOne
    private Dossier dossier;
}
