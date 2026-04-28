package aprove.verification.confluence.Utility;


public record HereditaryProperties(
                             Boolean isLeftLinear,
                             Boolean isLinear,
                             Boolean isNonOverlapping) {
    public HereditaryProperties(Boolean isLeftLinear,
                              Boolean isLinear,
                              Boolean isNonOverlapping) {
        this.isLeftLinear = (isLeftLinear != null && !isLeftLinear) ? null : isLeftLinear;
        this.isLinear = (isLinear != null && !isLinear) ? null : isLinear;
        this.isNonOverlapping = (isNonOverlapping != null && !isNonOverlapping) ? null : isNonOverlapping;
    }
}
