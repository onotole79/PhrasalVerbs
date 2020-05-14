package com.onotole.phrasalverbs;

import java.util.List;

class Base {
    List<VerbInfo> VerbsInfo;
    int Meanings;
}

class Variation {
    String Preposition;
    String Meaning;
    String Example;
}

class VerbInfo {
    String Verb;
    List<Variation> Variations;
}

class Card {
    String Verb;
    String Preposition;
    String Meaning;
    String Example;
    Card(String verb, String preposition, String meaning, String example){
        Verb = verb;
        Preposition = preposition;
        Meaning = meaning;
        Example = example;
    }
}



