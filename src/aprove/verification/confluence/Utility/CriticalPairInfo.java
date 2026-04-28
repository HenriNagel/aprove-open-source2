package aprove.verification.confluence.Utility;


import aprove.verification.dpframework.BasicStructures.*;
import immutables.*;

public record CriticalPairInfo(ImmutableLinkedHashSet<GeneralizedRule> rules, 
                               TRSTerm s, 
                               TRSTerm t, 
                               boolean isRootOverlap,
                               TRSTerm divergence
                           ) {
        public static boolean isTrivial(CriticalPairInfo cpi) {
            return cpi.s().equals(cpi.t());
        }
}