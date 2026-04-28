package aprove.verification.confluence;

import java.util.*;

import aprove.prooftree.Export.Utility.*;
import aprove.prooftree.Obligations.*;
import aprove.prooftree.Proofs.Proof.*;
import aprove.strategies.Abortions.*;
import aprove.strategies.ExecutableStrategies.*;
import aprove.verification.dpframework.*;
import aprove.verification.dpframework.TRSProblem.*;
import aprove.verification.oldframework.Logic.*;
import aprove.verification.oldframework.Utility.*;


public class ConfNewmanProcessor implements Processor {
    
    public class NewmanProof extends DefaultProof {
        @Override
        public String export(Export_Util o, VerbosityLevel level) {
            StringBuilder s = new StringBuilder();
            s.append("A terminating, locally confluent TRS is confluent. ");
            s.append(o.cite(Citation.NEW42));
            return s.toString();
        }
    }
    

    @Override
    public Result process(BasicObligation obl,
                          BasicObligationNode oblNode,
                          Abortion aborter,
                          RuntimeInformation rti) throws AbortionException {
        ConfTRSProblem conftrs = (ConfTRSProblem) obl;
        QTRSProblem qtrs = conftrs.getQTRSProblem();
        CriticalPairProblem cpp = new CriticalPairProblem(conftrs, aborter);
        
        Collection<BasicObligation> obls = Arrays.asList(qtrs, cpp);
        return ResultFactory.provedAnd(obls,YNMImplication.SOUND, new NewmanProof());
    }

    @Override
    public boolean isApplicable(BasicObligation obl) {
        return obl instanceof ConfTRSProblem;
    }

}
