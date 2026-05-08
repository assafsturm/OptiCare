package Model.enums;

public enum RiskLevel {
    
    CLEAN,

    
    RESPIRATORY,

    

    INFECTIOUS,


    IMMUNO_COMPROMISED,

    UNKNOWN;

   // priority for waiting list (smaller is higher priority)
    public int waitingQueuePriority() {
        return switch (this) {
            case IMMUNO_COMPROMISED -> 0;
            case INFECTIOUS -> 1;
            case RESPIRATORY -> 2;
            case CLEAN -> 3;
            case UNKNOWN -> 4;
        };
    }
}