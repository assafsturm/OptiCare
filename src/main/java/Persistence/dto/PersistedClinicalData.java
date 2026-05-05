package Persistence.dto;

import Model.enums.BedType;
import Model.enums.RiskLevel;

/** JSON DTO aligned with {@link Model.entety.ClinicalData}. */
public final class PersistedClinicalData {

    private RiskLevel riskLevel;
    private int severityScore;
    private boolean needsVentilator;
    private BedType requiredBedType;
    private Integer weightKg;

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(int severityScore) {
        this.severityScore = severityScore;
    }

    public boolean isNeedsVentilator() {
        return needsVentilator;
    }

    public void setNeedsVentilator(boolean needsVentilator) {
        this.needsVentilator = needsVentilator;
    }

    public BedType getRequiredBedType() {
        return requiredBedType;
    }

    public void setRequiredBedType(BedType requiredBedType) {
        this.requiredBedType = requiredBedType;
    }

    public Integer getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Integer weightKg) {
        this.weightKg = weightKg;
    }
}
