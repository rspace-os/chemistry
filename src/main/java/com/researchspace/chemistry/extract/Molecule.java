package com.researchspace.chemistry.extract;

public class Molecule {
  private final int atomCount;
  private final int bondCount;
  private final int formalCharge;
  private final double exactMass;
  private final double mass;
  private final String formula;
  private final String name;
  private final MoleculeRole role;

  public Molecule(Builder builder) {
    this.atomCount = builder.atomCount;
    this.bondCount = builder.bondCount;
    this.formalCharge = builder.formalCharge;
    this.exactMass = builder.exactMass;
    this.mass = builder.mass;
    this.formula = builder.formula;
    this.name = builder.name;
    this.role = builder.role;
  }

  public MoleculeRole getRole() {
    return role;
  }

  public int getAtomCount() {
    return atomCount;
  }

  public int getBondCount() {
    return bondCount;
  }

  public int getFormalCharge() {
    return formalCharge;
  }

  public double getExactMass() {
    return exactMass;
  }

  public double getMass() {
    return mass;
  }

  public String getFormula() {
    return formula;
  }

  public String getName() {
    return name;
  }

  public static class Builder {
    private int atomCount;
    private int bondCount;
    private int formalCharge;
    private double exactMass;
    private double mass;
    private String formula;
    private String name;
    private MoleculeRole role;

    public Builder atomCount(int atomCount) {
      this.atomCount = atomCount;
      return this;
    }

    public Builder bondCount(int bondCount) {
      this.bondCount = bondCount;
      return this;
    }

    public Builder formalCharge(int formalCharge) {
      this.formalCharge = formalCharge;
      return this;
    }

    public Builder exactMass(double exactMass) {
      this.exactMass = exactMass;
      return this;
    }

    public Builder mass(double mass) {
      this.mass = mass;
      return this;
    }

    public Builder formula(String formula) {
      this.formula = formula;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder role(MoleculeRole role) {
      this.role = role;
      return this;
    }

    public Molecule build() {
      return new Molecule(this);
    }
  }
}
