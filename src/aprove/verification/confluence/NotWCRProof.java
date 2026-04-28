package aprove.verification.confluence;

import java.util.*;

import aprove.prooftree.Export.Utility.*;
import aprove.prooftree.Proofs.Proof.*;
import aprove.verification.confluence.Utility.*;
import aprove.verification.dpframework.BasicStructures.*;
import aprove.verification.oldframework.Utility.*;


public class NotWCRProof extends DefaultProof {
    private Collection<GeneralizedRule> rules;
    private Collection<TRSTerm> terms;
    private TRSTerm r;
    
    public NotWCRProof(CriticalPairInfo disproof) {
        this.rules = disproof.rules();
        this.r = disproof.divergence();
        this.terms = Arrays.asList(disproof.s(), disproof.t());
    }

    @Override
    public String export(Export_Util o, VerbosityLevel level) {
        final Iterator iter = terms.iterator();
        final StringBuilder s = new StringBuilder();
        s.append(o.export("The TRS is not locally confluent (WCR):"));
        s.append(o.cond_linebreak());
        s.append(o.export("These are the Rules causing the unjoinable critical Pair:"));
        s.append(o.cond_linebreak());
        s.append(o.set(rules, Export_Util.RULES));
        s.append(o.cond_linebreak());
        s.append(o.export("This leads to the following divergence:"));
        s.append(o.cond_linebreak());
        s.append(o.cond_linebreak());
        s.append(o.export(this.r));
        s.append(o.export(" ->* <"));
        s.append(o.export(iter.next()));
        s.append(o.export(", "));
        s.append(o.export(iter.next()));
        s.append(o.export(">"));
        return s.toString();
    }
}
