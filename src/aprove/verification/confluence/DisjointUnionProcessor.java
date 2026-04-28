package aprove.verification.confluence;

import java.util.*;

import aprove.prooftree.Obligations.*;
import aprove.strategies.Abortions.*;
import aprove.strategies.ExecutableStrategies.*;
import aprove.verification.confluence.Utility.*;
import aprove.verification.dpframework.*;
import aprove.verification.dpframework.BasicStructures.*;
import aprove.verification.oldframework.Logic.*;
import immutables.*;


public class DisjointUnionProcessor implements Processor {

    @Override
    public Result process(BasicObligation obl,
                          BasicObligationNode oblNode,
                          Abortion aborter,
                          RuntimeInformation rti) throws AbortionException {
        ConfTRSProblem conftrs = (ConfTRSProblem) obl;
        Collection<ImmutableSet<Rule>> disjointRules = conftrs.disjointRules();
        
        if(1 == disjointRules.size()) {
            return ResultFactory.unsuccessful();
        }
       
        if(0 == disjointRules.size()) {
            return ResultFactory.error("Split lead to empty List");
        }
        
        HereditaryProperties mp = conftrs.getHereditaryProperties();
        
        Collection<ConfTRSProblem> subconftrss = disjointRules.stream()
                .map(R -> ConfTRSProblem.create(R, mp))
                .toList();
               
                
        return ResultFactory.provedAnd(subconftrss, YNMImplication.EQUIVALENT, new UnionDisjointProof());
    }

    @Override
    public boolean isApplicable(BasicObligation obl) {
        return obl instanceof ConfTRSProblem;
    }
}
