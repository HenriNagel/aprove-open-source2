package aprove.verification.confluence;

import aprove.prooftree.Obligations.*;
import aprove.strategies.Abortions.*;
import aprove.strategies.ExecutableStrategies.*;
import aprove.verification.confluence.Utility.*;
import aprove.verification.dpframework.*;

public class NonConfluenceProcessor implements Processor {

    @Override
    public Result process(BasicObligation obl,
                          BasicObligationNode oblNode,
                          Abortion aborter,
                          RuntimeInformation rti) throws AbortionException {
        CriticalPairs cps = ProcessorUtil.getCP(obl);
        switch (cps.checkStatusDefinitive(aborter)) {
            case WCR_DONE:
                return ResultFactory.unsuccessful();
            case NOTCR:
                return ResultFactory.disproved(new NotCRProof(cps.getCounterExampleCR()));
            case NOTWCR:
                return ResultFactory.disproved(new NotWCRProof(cps.getCounterExampleWCR()));
            default:
                return ResultFactory.unsuccessful();
        }
    }

    @Override
    public boolean isApplicable(BasicObligation obl) {
        return obl instanceof ConfTRSProblem || obl instanceof CriticalPairProblem;
    }

}
