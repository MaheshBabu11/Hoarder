package com.maheshbabu11.hoarder.entity;

import com.maheshbabu11.hoarder.annotation.Hoarded;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Entity
@Hoarded
@Table(name = "periodic_table")
public class PeriodicTable {
    @Id
    @Column(name = "\"AtomicNumber\"", nullable = false)
    private Integer id;

    @Column(name = "\"Element\"", length = Integer.MAX_VALUE)
    private String element;

    @Column(name = "\"Symbol\"", length = Integer.MAX_VALUE)
    private String symbol;

    @Column(name = "\"AtomicMass\"")
    private BigDecimal atomicMass;

    @Column(name = "\"NumberOfNeutrons\"")
    private Integer numberOfNeutrons;

    @Column(name = "\"NumberOfProtons\"")
    private Integer numberOfProtons;

    @Column(name = "\"NumberOfElectrons\"")
    private Integer numberOfElectrons;

    @Column(name = "\"Period\"")
    private Integer period;

    @Column(name = "\"Group\"")
    private Integer group;

    @Column(name = "\"Phase\"", length = Integer.MAX_VALUE)
    private String phase;

    @Column(name = "\"Radioactive\"")
    private Boolean radioactive;

    @Column(name = "\"Natural\"")
    private Boolean natural;

    @Column(name = "\"Metal\"")
    private Boolean metal;

    @Column(name = "\"Nonmetal\"")
    private Boolean nonmetal;

    @Column(name = "\"Metalloid\"")
    private Boolean metalloid;

    @Column(name = "\"Type\"", length = Integer.MAX_VALUE)
    private String type;

    @Column(name = "\"AtomicRadius\"")
    private BigDecimal atomicRadius;

    @Column(name = "\"Electronegativity\"")
    private BigDecimal electronegativity;

    @Column(name = "\"FirstIonization\"")
    private BigDecimal firstIonization;

    @Column(name = "\"Density\"")
    private BigDecimal density;

    @Column(name = "\"MeltingPoint\"")
    private BigDecimal meltingPoint;

    @Column(name = "\"BoilingPoint\"")
    private BigDecimal boilingPoint;

    @Column(name = "\"NumberOfIsotopes\"")
    private Integer numberOfIsotopes;

    @Column(name = "\"Discoverer\"", length = Integer.MAX_VALUE)
    private String discoverer;

    @Column(name = "\"Year\"")
    private Integer year;

    @Column(name = "\"SpecificHeat\"")
    private BigDecimal specificHeat;

    @Column(name = "\"NumberOfShells\"")
    private Integer numberOfShells;

    @Column(name = "\"NumberOfValence\"")
    private Integer numberOfValence;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getAtomicMass() {
        return atomicMass;
    }

    public void setAtomicMass(BigDecimal atomicMass) {
        this.atomicMass = atomicMass;
    }

    public Integer getNumberOfNeutrons() {
        return numberOfNeutrons;
    }

    public void setNumberOfNeutrons(Integer numberOfNeutrons) {
        this.numberOfNeutrons = numberOfNeutrons;
    }

    public Integer getNumberOfProtons() {
        return numberOfProtons;
    }

    public void setNumberOfProtons(Integer numberOfProtons) {
        this.numberOfProtons = numberOfProtons;
    }

    public Integer getNumberOfElectrons() {
        return numberOfElectrons;
    }

    public void setNumberOfElectrons(Integer numberOfElectrons) {
        this.numberOfElectrons = numberOfElectrons;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Integer getGroup() {
        return group;
    }

    public void setGroup(Integer group) {
        this.group = group;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Boolean getRadioactive() {
        return radioactive;
    }

    public void setRadioactive(Boolean radioactive) {
        this.radioactive = radioactive;
    }

    public Boolean getNatural() {
        return natural;
    }

    public void setNatural(Boolean natural) {
        this.natural = natural;
    }

    public Boolean getMetal() {
        return metal;
    }

    public void setMetal(Boolean metal) {
        this.metal = metal;
    }

    public Boolean getNonmetal() {
        return nonmetal;
    }

    public void setNonmetal(Boolean nonmetal) {
        this.nonmetal = nonmetal;
    }

    public Boolean getMetalloid() {
        return metalloid;
    }

    public void setMetalloid(Boolean metalloid) {
        this.metalloid = metalloid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAtomicRadius() {
        return atomicRadius;
    }

    public void setAtomicRadius(BigDecimal atomicRadius) {
        this.atomicRadius = atomicRadius;
    }

    public BigDecimal getElectronegativity() {
        return electronegativity;
    }

    public void setElectronegativity(BigDecimal electronegativity) {
        this.electronegativity = electronegativity;
    }

    public BigDecimal getFirstIonization() {
        return firstIonization;
    }

    public void setFirstIonization(BigDecimal firstIonization) {
        this.firstIonization = firstIonization;
    }

    public BigDecimal getDensity() {
        return density;
    }

    public void setDensity(BigDecimal density) {
        this.density = density;
    }

    public BigDecimal getMeltingPoint() {
        return meltingPoint;
    }

    public void setMeltingPoint(BigDecimal meltingPoint) {
        this.meltingPoint = meltingPoint;
    }

    public BigDecimal getBoilingPoint() {
        return boilingPoint;
    }

    public void setBoilingPoint(BigDecimal boilingPoint) {
        this.boilingPoint = boilingPoint;
    }

    public Integer getNumberOfIsotopes() {
        return numberOfIsotopes;
    }

    public void setNumberOfIsotopes(Integer numberOfIsotopes) {
        this.numberOfIsotopes = numberOfIsotopes;
    }

    public String getDiscoverer() {
        return discoverer;
    }

    public void setDiscoverer(String discoverer) {
        this.discoverer = discoverer;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getSpecificHeat() {
        return specificHeat;
    }

    public void setSpecificHeat(BigDecimal specificHeat) {
        this.specificHeat = specificHeat;
    }

    public Integer getNumberOfShells() {
        return numberOfShells;
    }

    public void setNumberOfShells(Integer numberOfShells) {
        this.numberOfShells = numberOfShells;
    }

    public Integer getNumberOfValence() {
        return numberOfValence;
    }

    public void setNumberOfValence(Integer numberOfValence) {
        this.numberOfValence = numberOfValence;
    }

}