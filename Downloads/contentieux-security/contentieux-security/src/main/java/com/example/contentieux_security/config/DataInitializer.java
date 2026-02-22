package com.example.contentieux_security.config;

import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.entity.Client;
import com.example.contentieux_security.repository.AgenceRepository;
import com.example.contentieux_security.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AgenceRepository agenceRepository;
    private final ClientRepository clientRepository;

    @Override
    public void run(String... args) throws Exception {
        if (agenceRepository.count() == 0) {
            agenceRepository.save(Agence.builder().nom("Agence Nabeul").code("NAB001").ville("Nabeul").build());
            agenceRepository.save(Agence.builder().nom("Agence Tunis").code("TUN001").ville("Tunis").build());
        }

        if (clientRepository.count() == 0) {
            clientRepository.save(Client.builder().nom("Ben Ali").prenom("Mohamed").cin("01234567")
                    .adresse("Route de Tunis, Nabeul").build());
            clientRepository.save(Client.builder().nom("Trabelsi").prenom("Amira").cin("08765432")
                    .adresse("Avenue Habib Bourguiba, Tunis").build());
        }
    }
}
