package aprove.verification.confluence;

import java.util.*;

import aprove.prooftree.Export.Utility.*;
import aprove.prooftree.Proofs.Proof.*;
import aprove.verification.confluence.Utility.*;
import aprove.verification.dpframework.BasicStructures.*;
import aprove.verification.oldframework.Utility.*;
import aprove.verification.oldframework.Utility.GenericStructures.*;


public class NotCRProof extends DefaultProof {
    private List<TRSTerm> terms;
    private TRSTerm r;
    
    public NotCRProof(CriticalPairInfo disproof, TRSTerm s, TRSTerm t) {
        this.r = disproof.divergence();
        this.terms = Arrays.asList(s, t);
    }

    public NotCRProof(Triple<CriticalPairInfo, TRSTerm, TRSTerm> c) {
        this(c.getX(), c.getY(), c.getZ());
    }
    @Override
    public String export(Export_Util o, VerbosityLevel level) {
        final Iterator iter = terms.iterator();
        final StringBuilder s = new StringBuilder();
        s.append(o.export("The TRS is not confluent:"));
        s.append(o.cond_linebreak());
        s.append(o.export("The following term leads to following normal forms:"));
        s.append(o.cond_linebreak());
        s.append(o.cond_linebreak());
        s.append(o.export(this.r));
        s.append(o.linebreak());
        s.append(o.export("->* "));
        s.append(o.export(iter.next()));
        s.append(o.linebreak());
        s.append(o.export("->* "));
        s.append(o.export(iter.next()));
        return s.toString();
    }

}
