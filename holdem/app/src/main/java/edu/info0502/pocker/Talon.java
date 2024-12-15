package edu.info0502.pocker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


class Talon {
    private List<Carte> cartes;

    public Talon(int nombrePaquets) {
        cartes = new ArrayList<>();
        for (int i = 0; i < nombrePaquets; i++) {
            PaquetDeCartes paquet = new PaquetDeCartes();
            cartes.addAll(paquet.getCartes());
        }
        melanger();
    }

    public void melanger() {
        Collections.shuffle(cartes);
    }

    public Carte tirerCarte() {
        if (cartes.isEmpty()) {
            throw new IllegalStateException("Le talon est vide");
        }
        return cartes.remove(0);
    }

    public int nombreCartes() {
        return cartes.size();
    }

    @Override
    public String toString() {
        return cartes.toString();
    }
}