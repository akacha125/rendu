package edu.info0502.pocker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PokerHoldem {

    private Talon talon;
    private List<Joueur> joueurs;
    private CartesCommunes cartesCommunes;

    public PokerHoldem(List<String> nomsJoueurs) {
        if (nomsJoueurs.size() < 2 || nomsJoueurs.size() > 10) {
            throw new IllegalArgumentException("Le Texas Hold'em nécessite 2 à 10 joueurs");
        }

        talon = new Talon(1);
        joueurs = new ArrayList<>();
        cartesCommunes = new CartesCommunes();

      
        for (String nom : nomsJoueurs) {
            joueurs.add(new Joueur(nom));
        }
    }

    public void demarrerPartie() {
        talon.melanger();
        cartesCommunes.reinitialiser();
        distribuerCartesPrivees();
    }

    private void distribuerCartesPrivees() {
        for (Joueur joueur : joueurs) {
            Carte carte1 = talon.tirerCarte();
            Carte carte2 = talon.tirerCarte();
            joueur.recevoirCartesPrivees(carte1, carte2);
        }
    }

    public void distribuerFlop() {
        for (int i = 0; i < 3; i++) {
            cartesCommunes.ajouterCarte(talon.tirerCarte());
        }
    }

    public void distribuerTurn() {
        cartesCommunes.ajouterCarte(talon.tirerCarte());
    }

    public void distribuerRiver() {
        cartesCommunes.ajouterCarte(talon.tirerCarte());
    }

    public Map<String, String> calculerResultats() {
        Map<String, String> results = new HashMap<>();
        for (Joueur joueur : joueurs) {
            joueur.evaluerMeilleureMain(cartesCommunes);
            String mainInfo = joueur.getMeilleureMain().evaluerMain().toString();
            results.put(joueur.getNom(), mainInfo);
        }
        return results;
    }

    public String determinerGagnant() {
        Joueur gagnant = null;
        Main meilleurMain = null;

        for (Joueur joueur : joueurs) {
            joueur.evaluerMeilleureMain(cartesCommunes);
            Main mainActuelle = joueur.getMeilleureMain();

            if (meilleurMain == null || mainActuelle.comparerAvec(meilleurMain) > 0) {
                meilleurMain = mainActuelle;
                gagnant = joueur;
            }
        }

        return gagnant != null ? gagnant.getNom() + " gagne avec " + meilleurMain.evaluerMain() : null;
    }

    public List<Carte> getCartesCommunes() {
        return cartesCommunes.getCartes();
    }

    public Joueur getJoueurParNom(String nom) {
        for (Joueur joueur : joueurs) {
            if (joueur.getNom().equals(nom)) {
                return joueur;
            }
        }
        return null;
    }
}

class Joueur {

    private String nom;
    private List<Carte> cartesPrivees;
    private Main meilleureMain;

    public Joueur(String nom) {
        this.nom = nom;
        this.cartesPrivees = new ArrayList<>();
    }

    public void recevoirCartesPrivees(Carte carte1, Carte carte2) {
        cartesPrivees.clear();
        cartesPrivees.add(carte1);
        cartesPrivees.add(carte2);
    }

    public void evaluerMeilleureMain(CartesCommunes cartesCommunes) {
        List<Carte> toutesLesCartes = new ArrayList<>(cartesPrivees);
        toutesLesCartes.addAll(cartesCommunes.getCartes());
        meilleureMain = trouverMeilleureCombinaison(toutesLesCartes);
    }

    private Main trouverMeilleureCombinaison(List<Carte> toutesLesCartes) {
        Main meilleureCombinaison = null;
        List<List<Carte>> combinaisons = genererCombinaisons(toutesLesCartes, 5);

        for (List<Carte> combinaison : combinaisons) {
            Main main = new Main();
            for (Carte carte : combinaison) {
                main.ajouterCarte(carte);
            }

            if (meilleureCombinaison == null || main.comparerAvec(meilleureCombinaison) > 0) {
                meilleureCombinaison = main;
            }
        }

        return meilleureCombinaison;
    }

    private List<List<Carte>> genererCombinaisons(List<Carte> cartes, int taille) {
        List<List<Carte>> combinaisons = new ArrayList<>();
        genererCombinaisonsHelper(cartes, taille, 0, new ArrayList<>(), combinaisons);
        return combinaisons;
    }

    private void genererCombinaisonsHelper(List<Carte> cartes, int taille, int debut,
            List<Carte> actuelle, List<List<Carte>> resultat) {
        if (actuelle.size() == taille) {
            resultat.add(new ArrayList<>(actuelle));
            return;
        }

        for (int i = debut; i < cartes.size(); i++) {
            actuelle.add(cartes.get(i));
            genererCombinaisonsHelper(cartes, taille, i + 1, actuelle, resultat);
            actuelle.remove(actuelle.size() - 1);
        }
    }

    public String getNom() {
        return nom;
    }

    public List<Carte> getCartesPrivees() {
        return Collections.unmodifiableList(cartesPrivees);
    }

    public Main getMeilleureMain() {
        return meilleureMain;
    }

    public void afficherMain() {
        System.out.println(nom + " - Cartes privées: " + cartesPrivees);
        System.out.println("Meilleure main: " + meilleureMain);
        System.out.println("Combinaison: " + meilleureMain.evaluerMain());
        System.out.println();
    }

}

class CartesCommunes {

    private List<Carte> cartes;

    public CartesCommunes() {
        cartes = new ArrayList<>();
    }

    public void ajouterCarte(Carte carte) {
        if (cartes.size() >= 5) {
            throw new IllegalStateException("Trop de cartes communes");
        }
        cartes.add(carte);
    }

    public void reinitialiser() {
        cartes.clear();
    }

    public List<Carte> getCartes() {
        return Collections.unmodifiableList(cartes);
    }
}
