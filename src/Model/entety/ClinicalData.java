package Model.entety;


import Model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalData {
    private RiskLevel riskLevel;
    private int severityScore;
    private boolean needsVentilator;
    private BedType requiredBedType;
}