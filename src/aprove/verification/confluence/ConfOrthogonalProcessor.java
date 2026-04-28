package aprove.verification.confluence;

import aprove.prooftree.Obligations.*;
import aprove.strategies.Abortions.*;
import aprove.strategies.ExecutableStrategies.*;
import aprove.verification.dpframework.*;
import aprove.verification.oldframework.Logic.*;


public class ConfOrthogonalProcessor implements Processor {
    
    @Override
    public Result process(BasicObligation obl,
                          BasicObligationNode oblNode,
                          Abortion aborter,
                          RuntimeInformation rti) throws AbortionException {
        ConfTRSProblem conftrs = (ConfTRSProblem) obl;
        
        if (conftrs.isNonOverlapping(aborter)) {
            return ResultFactory.provedWithValueAndImplication(YNM.YES, YNMImplication.SOUND, new OrthogonalProof(false));
        }
        
        if (conftrs.getCriticalPairs().onlyTrivialCriticalPairs(aborter)) {
            return ResultFactory.provedWithValueAndImplication(YNM.YES, YNMImplication.SOUND, new OrthogonalProof(true));
        }
        
        return ResultFactory.unsuccessful();
    }

    @Override
    public boolean isApplicable(BasicObligation obl) {
        if(obl instanceof ConfTRSProblem conftrs) {
            return conftrs.isLeftLinear();
        }
        return false;
    }

}
