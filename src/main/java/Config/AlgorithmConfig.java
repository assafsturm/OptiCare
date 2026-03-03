package Config;

/**
 * Centralized configuration for the Simulated Annealing optimizer and cost function.
 * Big M and penalty weights can be tuned here or loaded from file later.
 */
public class AlgorithmConfig {

    // --- Simulated Annealing ---
    private double initialTemperature = 10_000.0;// טמפרטורה התחלתית
    private double coolingRate = 0.995;// קצב הקירור
    private double minTemperature = 0.01;// טמפרטורה מינימלית
    private int iterationsPerTemperature = 100;// מספר האיטרציות לכל טמפרטורה
    private int maxTotalIterations = 100_000;// מספר האיטרציות הכולל

    // --- Early termination (optional) ---
    private int noImprovementStepsToStop = 0;// מספר הצעדים שלא נעשו שיפורים
    private double targetEnergyThreshold = 0.0;// תנאי עצירה

    // --- Reproducibility ---
    private long randomSeed = 0;// זריקת רנדומלית

    // --- Cost function constants ---
    private double bigM = 1_000_000.0;// מספר גדול מאוד
    private double transferPenaltyWeight = 5_000.0;// משקולת העברה
    private double policyPenaltyWeight = 500.0;// משקולת מדיניות

    public AlgorithmConfig() {
    }

    public double getInitialTemperature() { return initialTemperature; }// מחזיר את הטמפרטורה ההתחלתית
    public void setInitialTemperature(double initialTemperature) { this.initialTemperature = initialTemperature; }

    public double getCoolingRate() { return coolingRate; }// מחזיר את הקצב הקילוף
    public void setCoolingRate(double coolingRate) { this.coolingRate = coolingRate; }// מחזיר את הקצב הקילוף

    public double getMinTemperature() { return minTemperature; }
    public void setMinTemperature(double minTemperature) { this.minTemperature = minTemperature; }// מחזיר את הטמפרטורה המינימלית

    public int getIterationsPerTemperature() { return iterationsPerTemperature; }// מחזיר את המספר האיטרציות לכל טמפרטורה
    public void setIterationsPerTemperature(int iterationsPerTemperature) { this.iterationsPerTemperature = iterationsPerTemperature; }// מחזיר את המספר האיטרציות לכל טמפרטורה

    public int getMaxTotalIterations() { return maxTotalIterations; }
    public void setMaxTotalIterations(int maxTotalIterations) { this.maxTotalIterations = maxTotalIterations; }// מחזיר את המספר האיטרציות הכולל

    public int getNoImprovementStepsToStop() { return noImprovementStepsToStop; }
    public void setNoImprovementStepsToStop(int noImprovementStepsToStop) { this.noImprovementStepsToStop = noImprovementStepsToStop; }// מחזיר את המספר האיטרציות הכולל

    public double getTargetEnergyThreshold() { return targetEnergyThreshold; }
    public void setTargetEnergyThreshold(double targetEnergyThreshold) { this.targetEnergyThreshold = targetEnergyThreshold; }// מחזיר את המספר האיטרציות הכולל

    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }// מחזיר את המספר האיטרציות הכולל

    public double getBigM() { return bigM; }
    public void setBigM(double bigM) { this.bigM = bigM; }// מחזיר את המספר האיטרציות הכולל

    public double getTransferPenaltyWeight() { return transferPenaltyWeight; }
    public void setTransferPenaltyWeight(double transferPenaltyWeight) { this.transferPenaltyWeight = transferPenaltyWeight; }// מחזיר את המספר האיטרציות הכולל

    public double getPolicyPenaltyWeight() { return policyPenaltyWeight; }
    public void setPolicyPenaltyWeight(double policyPenaltyWeight) { this.policyPenaltyWeight = policyPenaltyWeight; }// מחזיר את המספר האיטרציות הכולל
}
