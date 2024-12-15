package edu.info0502.pocker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.Serializable;

//  les combinaisons possibles au poker
enum CombinaisonPoker {
    CARTE_HAUTE(1),
    PAIRE(2),
    DEUX_PAIRES(3),
    BRELAN(4),
    QUINTE(5),
    COULEUR(6),
    FULL(7),
    CARRE(8),
    QUINTE_FLUSH(9),
    QUINTE_FLUSH_ROYALE(10);

    private final int valeur;

    CombinaisonPoker(int valeur) {
        this.valeur = valeur;
    }

    public int getValeur() {
        return valeur;
    }
}



class Main implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Carte> cartes;
    private static final int TAILLE_MAIN = 5;

    public static int getTailleMain() {
        return TAILLE_MAIN;
    }
    public Main() {
        cartes = new ArrayList<>();
    }

    public void ajouterCarte(Carte carte) {
        if (cartes.size() >= TAILLE_MAIN) {
            throw new IllegalStateException("La main est déjà pleine");
        }
        cartes.add(carte);
    }

    // New method to add multiple cards to the hand
    public void ajouterCartes(List<Carte> cartesToAdd) {
        for (Carte carte : cartesToAdd) {
            ajouterCarte(carte); // Reuse ajouterCarte to ensure size constraints are respected
        }
    }

    public List<Carte> getCartes() {
        return new ArrayList<>(cartes);
    }
    // evaluer une main pour vérifier les combinaisons
    public CombinaisonPoker evaluerMain() {
        if (cartes.size() != TAILLE_MAIN) {
            // la main n'est pas ecnore pleine
            throw new IllegalStateException("La main doit contenir exactement 5 cartes");
        }

        List<Carte> cartesTriees = new ArrayList<>(cartes);
        Collections.sort(cartesTriees);

        if (estQuinteFlushRoyale(cartesTriees)) return CombinaisonPoker.QUINTE_FLUSH_ROYALE;
        if (estQuinteFlush(cartesTriees)) return CombinaisonPoker.QUINTE_FLUSH;
        if (estCarre(cartesTriees)) return CombinaisonPoker.CARRE;
        if (estFull(cartesTriees)) return CombinaisonPoker.FULL;
        if (estCouleur(cartesTriees)) return CombinaisonPoker.COULEUR;
        if (estQuinte(cartesTriees)) return CombinaisonPoker.QUINTE;
        if (estBrelan(cartesTriees)) return CombinaisonPoker.BRELAN;
        if (estDeuxPaires(cartesTriees)) return CombinaisonPoker.DEUX_PAIRES;
        if (estPaire(cartesTriees)) return CombinaisonPoker.PAIRE;
        return CombinaisonPoker.CARTE_HAUTE; // si aucune autre combinaison est valide
    }


    // vérifier un Quint Flush Royale
    private boolean estQuinteFlushRoyale(List<Carte> cartesTriees) {
        return estQuinteFlush(cartesTriees) && 
               cartesTriees.get(4).getValeur() == Valeur.AS;
    }

    // vérifier un Quint Flush
    private boolean estQuinteFlush(List<Carte> cartesTriees) {
        return estQuinte(cartesTriees) && estCouleur(cartesTriees);
    }

    // vérifier un Carré
    private boolean estCarre(List<Carte> cartesTriees) {
        return (compterOccurrences(cartesTriees, 0) == 4) || 
               (compterOccurrences(cartesTriees, 1) == 4);
    }

    // vérifier un Full
    private boolean estFull(List<Carte> cartesTriees) {
        boolean brelanDebut = compterOccurrences(cartesTriees, 0) == 3;
        boolean brelanFin = compterOccurrences(cartesTriees, 2) == 3;
        return (brelanDebut && compterOccurrences(cartesTriees, 3) == 2) || 
               (brelanFin && compterOccurrences(cartesTriees, 0) == 2);
    }

    // vérifier un Couleur
    private boolean estCouleur(List<Carte> cartesTriees) {
        Couleur couleur = cartesTriees.get(0).getCouleur();
        return cartesTriees.stream().allMatch(carte -> carte.getCouleur() == couleur);
    }

    // vérifier un Quint
    private boolean estQuinte(List<Carte> cartesTriees) {
        for (int i = 0; i < cartesTriees.size() - 1; i++) {
            if (cartesTriees.get(i + 1).getValeur().getValeur() - 
                cartesTriees.get(i).getValeur().getValeur() != 1) {
                return false;
            }
        }
        return true;
    }


    // vérifier un Brelan
    private boolean estBrelan(List<Carte> cartesTriees) {
        return cartesTriees.stream()
                .anyMatch(carte -> compterOccurrences(cartesTriees, 
                         cartesTriees.indexOf(carte)) == 3);
    }

    // vérifier Deux paires
    private boolean estDeuxPaires(List<Carte> cartesTriees) {
        int nombrePaires = 0;
        for (int i = 0; i < cartesTriees.size(); i++) {
            if (compterOccurrences(cartesTriees, i) == 2) {
                nombrePaires++;
                i++; // Sauter la deuxième carte de la paire
            }
        }
        return nombrePaires == 2;
    }

    // vérifier une Paire
    private boolean estPaire(List<Carte> cartesTriees) {
        return cartesTriees.stream()
                .anyMatch(carte -> compterOccurrences(cartesTriees, 
                         cartesTriees.indexOf(carte)) == 2);
    }



    private int compterOccurrences(List<Carte> cartes, int index) {
        Valeur valeur = cartes.get(index).getValeur();
        return (int) cartes.stream()
                .filter(carte -> carte.getValeur() == valeur)
                .count();
    }


    public int comparerAvec(Main autreMain) {
        CombinaisonPoker combinaison1 = this.evaluerMain();
        CombinaisonPoker combinaison2 = autreMain.evaluerMain();

        if (combinaison1 != combinaison2) {
            return combinaison1.getValeur() - combinaison2.getValeur();
        }

        // si les deux main on la même combinaison, comparer les valeurs des cartes
        List<Carte> cartes1 = new ArrayList<>(this.cartes);
        List<Carte> cartes2 = new ArrayList<>(autreMain.cartes);
        Collections.sort(cartes1, Collections.reverseOrder());
        Collections.sort(cartes2, Collections.reverseOrder());

        for (int i = 0; i < TAILLE_MAIN; i++) {
            int comp = cartes1.get(i).compareTo(cartes2.get(i));
            if (comp != 0) return comp;
        }

        return 0;
    }

    @Override
    public String toString() {
        return cartes.toString();
    }
}