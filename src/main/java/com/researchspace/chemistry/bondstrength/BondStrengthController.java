package com.researchspace.chemistry.bondstrength;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BondStrengthController {

  private final BondStrengthService bondStrengthService;

  public BondStrengthController(BondStrengthService bondStrengthService) {
    this.bondStrengthService = bondStrengthService;
  }

  @PostMapping(value = "/chemistry/bondStrengths", produces = MediaType.APPLICATION_JSON_VALUE)
  public String predict(@Valid @RequestBody BondStrengthRequest request) {
    return bondStrengthService.predict(request.input());
  }
}
