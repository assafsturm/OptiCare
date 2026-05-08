package Model.entety;

import Model.enums.BedType;
import Model.enums.RiskLevel;

public class ClinicalData {

    // Weight above which a bariatric bed is needes.
    public static final int BARIATRIC_WEIGHT_THRESHOLD_KG = 120;

    
    private RiskLevel riskLevel; 
    
    private int severityScore; // severity score, when higher, the patient is more severe
    private boolean needsVentilator; // does the patient need a ventilator?
   
    private BedType requiredBedType;
  
    private Integer weightKg;// weight in kg, null if unknown (using Integer to represent null)

    public ClinicalData() {
    }

    public ClinicalData(RiskLevel riskLevel, int severityScore, boolean needsVentilator, BedType requiredBedType) {
        this.riskLevel = riskLevel;
        this.severityScore = severityScore;
        this.needsVentilator = needsVentilator;
        this.requiredBedType = requiredBedType;
    } // constructor for clinical data when weight is not provided

    public ClinicalData(RiskLevel riskLevel, int severityScore, boolean needsVentilator, BedType requiredBedType, Integer weightKg) {
        this.riskLevel = riskLevel;
        this.severityScore = severityScore;
        this.needsVentilator = needsVentilator;
        this.requiredBedType = requiredBedType;
        this.weightKg = weightKg;
    }


    // getters and setters for the clinical data
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

    // checks if the patient needs a bariatric bed
    public boolean needsBariatricBed() {
        return weightKg != null && weightKg >= BARIATRIC_WEIGHT_THRESHOLD_KG;
    }

    // checks if the patient needs a bariatric bed with a custom threshold
    public boolean needsBariatricBed(int thresholdKg) {
        return weightKg != null && weightKg >= thresholdKg;
    }
}
