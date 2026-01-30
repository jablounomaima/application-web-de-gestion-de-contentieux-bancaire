@Entity
public class Tribunal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTribunal;

    private String nom;
    private String ville;
}
