package aprove.verification.confluence;

import aprove.prooftree.Obligations.*;
import aprove.strategies.Abortions.*;
import aprove.strategies.ExecutableStrategies.*;
import aprove.verification.confluence.Utility.*;
import aprove.verification.dpframework.*;


public class LimitedNotWCRProcessor implements Processor {

    @Override
    public Result process(BasicObligation obl,
                          BasicObligationNode oblNode,
                          Abortion aborter,
                          RuntimeInformation rti) throws AbortionException {
        CriticalPairs cps = ProcessorUtil.getCP(obl);
        CPK localConfluence = cps.checkStatusDefinitive(8, aborter);
        switch(localConfluence) {
            case WCR:
            case WCR_DONE:
                return ResultFactory.unsuccessful("Termination Proof needed");
            case NOTCR:
                return ResultFactory.disproved(new NotCRProof(cps.getCounterExampleCR()));
            case NOTWCR:
                return ResultFactory.disproved(new NotWCRProof(cps.getCounterExampleWCR()));
            case MAYBE:
                return ResultFactory.unsuccessful("limited locally confluence without result");
            default:
                return ResultFactory.error("local confluence algorithm returned null.");
        }
    }

    @Override
    public boolean isApplicable(BasicObligation obl) {
        return obl instanceof ConfTRSProblem;
    }

}
