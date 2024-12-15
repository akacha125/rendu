package edu.info0502.pocker;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import java.io.Serializable;


// un enum pour représenter la carte (Couleur, Valeur)
enum Couleur {
    COEUR, CARREAU, PIQUE, TREFLE
}

// les valeurs des cartes
enum Valeur {
    DEUX(2), TROIS(3), QUATRE(4), CINQ(5), SIX(6), SEPT(7), HUIT(8), NEUF(9), DIX(10),
    VALET(11), DAME(12), ROI(13), AS(14);

    private final int valeur;

    Valeur(int valeur) {
        this.valeur = valeur;
    }

    public int getValeur() {
        return valeur;
    }
}

// Une classe pour Carte
class Carte implements Comparable<Carte>, Serializable{
    private static final long serialVersionUID = 1L;
    private final Couleur couleur;
    private final Valeur valeur;

    public Carte(Couleur couleur, Valeur valeur) {
        this.couleur = couleur;
        this.valeur = valeur;
    }

    public Couleur getCouleur() {
        return couleur;
    }

    public Valeur getValeur() {
        return valeur;
    }

    @Override
    public String toString() {
        return valeur + " de " + couleur;
    }

    // comparer avec une autre carte
    @Override
    public int compareTo(Carte autre) {
        return this.valeur.getValeur() - autre.valeur.getValeur();
    }
}



