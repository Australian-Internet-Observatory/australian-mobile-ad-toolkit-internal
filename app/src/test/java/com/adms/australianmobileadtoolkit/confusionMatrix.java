package com.adms.australianmobileadtoolkit;

import java.util.ArrayList;
import java.util.List;

public class confusionMatrix {
    List<String> tp = new ArrayList<>();
    List<String> fp = new ArrayList<>();
    List<String> tn = new ArrayList<>();
    List<String> fn = new ArrayList<>();
    public Integer tpN = 0;
    public Integer fpN = 0;
    public Integer tnN = 0;
    public Integer fnN = 0;

    confusionMatrix() { }

    public void add(String thisCase, String thisEntry) {
        switch (thisCase) {
            case "FN" : fn.add(thisEntry); break ;
            case "FP" : fp.add(thisEntry); break ;
            case "TN" : tn.add(thisEntry); break ;
            case "TP" : tp.add(thisEntry); break ;
            default : break ;
        }
    }

    public void addN(String thisCase, Integer thisEntry) {
        switch (thisCase) {
            case "FN" : fnN += thisEntry; break ;
            case "FP" : fpN += thisEntry; break ;
            case "TN" : tnN += thisEntry; break ;
            case "TP" : tpN += thisEntry; break ;
            default : break ;
        }
    }

    public void print() {
        System.out.println("TP: "+tp.size());
        for (String x : tp) { System.out.println("\t" + x); }
        System.out.println("FP: "+fp.size());
        for (String x : fp) { System.out.println("\t" + x); }
        System.out.println("TN: "+tn.size());
        for (String x : tn) { System.out.println("\t" + x); }
        System.out.println("FN: "+fn.size());
        for (String x : fn) { System.out.println("\t" + x); }
        System.out.println("Accuracy: "
                + ((tp.size()+tn.size()) / (double) (tp.size()+tn.size()+fp.size()+fn.size())));
    }

}
