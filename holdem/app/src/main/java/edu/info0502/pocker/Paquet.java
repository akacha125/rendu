package edu.info0502.pocker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



class PaquetDeCartes {
    private List<Carte> cartes;

    public PaquetDeCartes() {
        cartes = new ArrayList<>();
        for (Couleur couleur : Couleur.values()) {
            for (Valeur valeur : Valeur.values()) {
                cartes.add(new Carte(couleur, valeur));
            }
        }
    }

    public List<Carte> getCartes() {
        return cartes;
    }

    // Une méthode permettant de mélanger un ensemble de cartes
    public void melanger() {
        Collections.shuffle(cartes);
    }

    @Override
    public String toString() {
        return cartes.toString();
    }
}