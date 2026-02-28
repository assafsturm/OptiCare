package Model.entety;

import Model.enums.BedType;
import Model.enums.RiskLevel;

public class ClinicalData {

    /** Weight in kg above which a bariatric bed is required. */
    public static final int BARIATRIC_WEIGHT_THRESHOLD_KG = 120;

    /** Risk level of the patient. */
    private RiskLevel riskLevel;
    /** Severity score of the patient. */
    private int severityScore;
    private boolean needsVentilator;
    /** Type of bed required for this patient. */
    private BedType requiredBedType;
    /** Weight in kg; null if unknown. Used for bariatric bed requirement. */
    private Integer weightKg;

    public ClinicalData() {
    }

    public ClinicalData(RiskLevel riskLevel, int severityScore, boolean needsVentilator, BedType requiredBedType) {
        this.riskLevel = riskLevel;
        this.severityScore = severityScore;
        this.needsVentilator = needsVentilator;
        this.requiredBedType = requiredBedType;
    }

    public ClinicalData(RiskLevel riskLevel, int severityScore, boolean needsVentilator, BedType requiredBedType, Integer weightKg) {
        this.riskLevel = riskLevel;
        this.severityScore = severityScore;
        this.needsVentilator = needsVentilator;
        this.requiredBedType = requiredBedType;
        this.weightKg = weightKg;
    }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public int getSeverityScore() { return severityScore; }
    public void setSeverityScore(int severityScore) { this.severityScore = severityScore; }

    public boolean isNeedsVentilator() { return needsVentilator; }
    public void setNeedsVentilator(boolean needsVentilator) { this.needsVentilator = needsVentilator; }

    public BedType getRequiredBedType() { return requiredBedType; }
    public void setRequiredBedType(BedType requiredBedType) { this.requiredBedType = requiredBedType; }

    public Integer getWeightKg() { return weightKg; }
    public void setWeightKg(Integer weightKg) { this.weightKg = weightKg; }

    /** True if weight is known and >= default threshold (bariatric bed required). */
    public boolean needsBariatricBed() {
        return weightKg != null && weightKg >= BARIATRIC_WEIGHT_THRESHOLD_KG;
    }

    /** True if weight is known and >= given threshold. */
    public boolean needsBariatricBed(int thresholdKg) {
        return weightKg != null && weightKg >= thresholdKg;
    }
}
