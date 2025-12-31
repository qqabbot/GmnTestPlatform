package com.testing.automation.dto;

import lombok.Data;
import java.util.List;

@Data
public class VariableContext {
    private List<String> producedVariables; // Variables extracted by this case/step
    private List<String> consumedVariables; // Variables used by this case/step
    private List<String> availableVariables; // Cumulative variables from all previous steps
}
