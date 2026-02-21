package Model.entety;

import Model.enums.BedType;
import Model.enums.RiskLevel;

public class ClinicalData {
    private RiskLevel riskLevel;
    private int severityScore;
    private boolean needsVentilator;
    private BedType requiredBedType;

    public ClinicalData() {
    }

    public ClinicalData(RiskLevel riskLevel, int severityScore, boolean needsVentilator, BedType requiredBedType) {
        this.riskLevel = riskLevel;
        this.severityScore = severityScore;
        this.needsVentilator = needsVentilator;
        this.requiredBedType = requiredBedType;
    }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public int getSeverityScore() { return severityScore; }
    public void setSeverityScore(int severityScore) { this.severityScore = severityScore; }

    public boolean isNeedsVentilator() { return needsVentilator; }
    public void setNeedsVentilator(boolean needsVentilator) { this.needsVentilator = needsVentilator; }

    public BedType getRequiredBedType() { return requiredBedType; }
    public void setRequiredBedType(BedType requiredBedType) { this.requiredBedType = requiredBedType; }
}
