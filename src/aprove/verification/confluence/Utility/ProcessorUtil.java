package aprove.verification.confluence.Utility;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import aprove.prooftree.Obligations.*;
import aprove.verification.confluence.*;
import immutables.*;

public class ProcessorUtil {
    public static CriticalPairs getCP(BasicObligation obl) {
        if (obl instanceof ConfTRSProblem conftrs) {
            return conftrs.getCriticalPairs();
        }
        return ((CriticalPairProblem) obl).getCriticalPairs();
    }
    
    public static <T> Collection<T> findNonUniqueElements(Collection<ImmutableSet<T>> ruleSets){
        Objects.requireNonNull(ruleSets);
        
        return ruleSets.stream()
                .flatMap(Set::stream)
                .collect(Collectors.groupingBy(
                                               Function.identity(),
                                               Collectors.counting()
                        ))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
    }
    
}
