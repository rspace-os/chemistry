package com.researchspace.chemistry.extract;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExtractionResult {
  private List<Molecule> moleculeInfo = new ArrayList<>();
  private String formula;
  private boolean isReaction;

  public List<Molecule> getMoleculeInfo() {
    return moleculeInfo;
  }

  public void setMoleculeInfo(List<Molecule> moleculeInfo) {
    this.moleculeInfo = moleculeInfo;
  }

  public String getFormula() {
    return formula;
  }

  public void setFormula(String formula) {
    this.formula = formula;
  }

  public boolean isReaction() {
    return isReaction;
  }

  public void setReaction(boolean reaction) {
    isReaction = reaction;
  }

  public List<Molecule> getAgents() {
    return filterByRole(MoleculeRole.AGENT);
  }

  public List<Molecule> getReactants() {
    return filterByRole(MoleculeRole.REACTANT);
  }

  public List<Molecule> getProducts() {
    return filterByRole(MoleculeRole.PRODUCT);
  }

  public List<Molecule> getMolecules() {
    return filterByRole(MoleculeRole.MOLECULE);
  }

  private List<Molecule> filterByRole(MoleculeRole role) {
    return moleculeInfo.stream()
        .filter(mol -> role.equals(mol.getRole()))
        .collect(Collectors.toList());
  }
}
