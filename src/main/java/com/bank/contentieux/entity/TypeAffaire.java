@Entity
public class TypeAffaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idType;

    private String libelle;
}
