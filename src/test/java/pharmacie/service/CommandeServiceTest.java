package pharmacie.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pharmacie.dao.CommandeRepository;
import pharmacie.dao.LigneRepository;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Commande;
import pharmacie.entity.Ligne;
import pharmacie.entity.Medicament;

@SpringBootTest
@Transactional
class CommandeServiceTest {

    @Autowired
    private CommandeService service;

    @Autowired
    private CommandeRepository commandeDao;

    @Autowired
    private MedicamentRepository medicamentDao;

    @Autowired
    private LigneRepository ligneDao;

    @Test
    void ajouterLigne_incrementeQuantiteSiDejaPresente() {
        // commande 99998 contient déjà une ligne pour le médicament 98 (quantité 16)
        Ligne ligne = service.ajouterLigne(99998, 98, 4);
        assertNotNull(ligne.getId());
        assertEquals(20, ligne.getQuantite());
    }

    @Test
    void ajouterLigne_commandeDejaEnvoyee_lanceIllegalState() {
        assertThrows(IllegalStateException.class, () -> service.ajouterLigne(99999, 98, 1));
    }

    @Test
    void ajouterLigne_medicamentIndisponible_lanceIllegalState() {
        assertThrows(IllegalStateException.class, () -> service.ajouterLigne(99998, 97, 1));
    }

    @Test
    void ajouterLigne_medicamentInexistant_lanceNoSuchElement() {
        assertThrows(java.util.NoSuchElementException.class, () -> service.ajouterLigne(99998, 123456, 1));
    }

    @Test
    void ajouterLigne_quantiteNonPositive_lanceConstraintViolation() {
        assertThrows(ConstraintViolationException.class, () -> service.ajouterLigne(99998, 98, 0));
    }

    @Test
    void ajouterLigne_stockInsuffisant_lanceIllegalState() {
        // medicament 98 a unitesEnStock = 26 dans les données de test
        assertThrows(IllegalStateException.class, () -> service.ajouterLigne(99998, 98, 30));
    }

    @Test
    void enregistreExpedition_metAJourDatesEtStocks() {
        // état initial connu depuis test_data.sql
        Medicament med98 = medicamentDao.findById(98).orElseThrow();
        assertEquals(26, med98.getUnitesEnStock());
        assertEquals(20, med98.getUnitesCommandees());

        Commande c = service.enregistreExpedition(99998);
        assertNotNull(c.getEnvoyeele());
        assertEquals(LocalDate.now(), c.getEnvoyeele());

        Medicament medAfter = medicamentDao.findById(98).orElseThrow();
        // la ligne pour 99998 contient 16 unités pour le médicament 98
        assertEquals(10, medAfter.getUnitesEnStock());
        assertEquals(4, medAfter.getUnitesCommandees());
    }

    @Test
    void enregistreExpedition_dejaEnvoyee_lanceIllegalState() {
        assertThrows(IllegalStateException.class, () -> service.enregistreExpedition(99999));
    }

    @Test
    void getCommande_retourneOuLanceSiIntrouvable() {
        Commande c = service.getCommande(99998);
        assertEquals(99998, c.getNumero());
        assertThrows(java.util.NoSuchElementException.class, () -> service.getCommande(123456));
    }

    @Test
    void getCommandeEnCoursPour_retourneCommandesNonEnvoyees() {
        List<Commande> list = service.getCommandeEnCoursPour("2COM");
        assertTrue(list.stream().anyMatch(c -> c.getNumero() == 99998));
        assertTrue(list.stream().noneMatch(c -> c.getNumero() == 99999));
    }

    @Test
    void supprimerLigne_nestPasImplementee_lanceUnsupportedOperation() {
        // La méthode n'étant pas implémentée actuellement, on vérifie l'exception
        assertThrows(UnsupportedOperationException.class, () -> service.supprimerLigne(1));
    }

}
