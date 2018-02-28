package org.flasck.eclipse.editor;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCardReference;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.TemplateToken;
import org.zinutils.reflection.Reflection;

public class PartitionAccumulator {
	private final IRegionCollector regionCollector;

	public PartitionAccumulator(IDocument document) {
		this.regionCollector = new RegionCollector(document);
	}

	// for collaborator testing
	public PartitionAccumulator(IRegionCollector collector) {
		this.regionCollector = collector;
	}

	public void processScope(IScope scope) {
		if (scope == null)
			return;
		
		for (ScopeEntry x : scope) {
			if (x != null)
				processEntry(x.getValue());
		}
		
		regionCollector.process();
	}

	private void processEntry(Object o) {
		if (o == null)
			return;
		
		try {
			Reflection.call(this, "processObject", o);
		} catch (RuntimeException ex) {
			System.out.println("Cannot process object of type " + o.getClass());
			if (!ex.toString().contains("There is no matching method")) {
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	private void processList(List<?> list) {
		if (list == null)
			return;
		for (Object o : list)
			processEntry(o);
	}

	public void processObject(StructDefn sd) {
		region(sd.location(), "keyword");
		processList(sd.fields);
	}
	
	public void processObject(CardDefinition card) {
		region(card.kw, "keyword");
		region(card.location(), "typename");
		processEntry(card.state);
		processList(card.templates);
		processList(card.contracts);
		processList(card.handlers);
		processScope(card.innerScope());
	}

	public void processObject(StateDefinition sd) {
		region(sd.location(), "keyword");
		processList(sd.fields);
	}
	
	public void processObject(StructField sf) {
		processEntry(sf.type);
		region(sf.location(), "field");
	}
	
	public void processObject(ContractDecl cd) {
		region(cd.kw, "keyword");
		region(cd.location(), "typename");
		for (ContractMethodDecl m : cd.methods) {
			processEntry(m);
		}
	}

	public void processObject(ContractMethodDecl cmd) {
		if (cmd.rkw != null)
			region(cmd.rkw, "keyword"); // the "optional" keyword
		region(cmd.dkw, "keyword"); // up/down
		region(cmd.location(), "methodname");
		for (Object arg : cmd.args) {
			processEntry(arg);
		}
	}

	public void processObject(TypeReference tr) {
		region(tr.location(), "typename");
		if (tr.hasPolys())
			processList(tr.polys());
	}

	public void processObject(Template t) {
		region(t.kw, "keyword");
		region(t.location(), "field");
		processList(t.args);
		processEntry(t.content);
	}

	public void processObject(TemplateDiv td) {
		region(td.kw, "keyword");
		processList(td.attrs);
		processList(td.nested);
		processFormatsEvents(td);
	}

	public void processObject(TemplateList td) {
		region(td.kw, "keyword");
		region(td.listLoc, "field");
		region(td.iterLoc, "var");
		processList(td.formats);
		processFormats(td);
		processEntry(td.template);
	}

	public void processObject(TemplateCardReference cr) {
		region(cr.kw, "keyword");
		if (cr.explicitCard != null)
			region(cr.location, "typename");
		else
			region(cr.location, "field");
	}

	public void processObject(ContentString cs) {
		region(((Locatable)cs).location(), "literal");
		processFormatsEvents(cs);
	}
	
	public void processObject(ContentExpr ce) {
		region(((Locatable)ce.expr).location(), "field");
		processFormatsEvents(ce);
	}
	
	public void processObject(EventHandler eh) {
		region(eh.actionPos, "keyword");
		region(eh.kw, "symbol");
		processEntry(eh.expr);
	}
	
	public void processObject(TemplateToken tt) {
		if (tt.type == TemplateToken.STRING) {
			region(tt.location, "literal");
		} else
			System.out.println("Handle template token " + tt.type);
	}

	private void processFormatsEvents(TemplateFormatEvents td) {
		processList(td.handlers);
		processFormats(td);
	}

	private void processFormats(TemplateFormat td) {
		processList(td.formats);
	}

	public void processObject(ContractImplements ci) {
		region(ci.kw, "keyword");
		region(ci.location(), "typename");
		if (ci.varLocation != null)
			region(ci.varLocation, "field");
		processList(ci.methods);
	}
	public void processObject(HandlerImplements hi) {
		region(hi.kw, "keyword");
		region(hi.typeLocation, "typename");
		region(hi.location(), "typename");
		processList(hi.methods);
	}

	public void processObject(EventCaseDefn ecd) {
		region(ecd.kw, "keyword");
		region(ecd.location(), "methodname");
		processList(ecd.intro.args);
		processList(ecd.messages);
		processScope(ecd.innerScope());
	}

	public void processObject(MethodCaseDefn q) {
		region(q.location(), "methodname");
		processList(q.intro.args);
		processList(q.messages);
		processScope(q.innerScope());
	}

	public void processObject(FunctionCaseDefn q) {
		region(q.location(), "methodname");
		processList(q.intro.args);
		processEntry(q.expr);
		processScope(q.innerScope());
	}

	public void processObject(TypedPattern tp) {
		region(tp.typeLocation, "typename");
		region(tp.varLocation, "var");
	}

	public void processObject(VarPattern vp) {
		region(vp.location(), "var");
	}

	public void processObject(MethodMessage mm) {
		region(mm.kw, "symbol");
		if (mm.slot != null) {
			for (Locatable t : mm.slot) {
				region(t.location(), "field");
			}
		}
		processEntry(mm.expr);
	}
	
	public void processObject(ApplyExpr ae) {
		if (tryHerdingDots(ae)) {
			Object a2 = ae.args.get(1);
			if (a2 instanceof UnresolvedVar)
				region(ae.location(), "field");
			else
				region(ae.location(), "typename");
			return;
		}
		processEntry(ae.fn);
		processList(ae.args);
	}

	private boolean tryHerdingDots(Object expr) {
		if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr)expr;
			if (ae.fn instanceof UnresolvedOperator) {
				UnresolvedOperator op = (UnresolvedOperator) ae.fn;
				return op.op.equals(".") && tryHerdingDots(ae.args.get(0));
			} else
				return false;
		} else if (expr instanceof UnresolvedVar) {
			return true;
		} else {
			System.out.println("Cannot herd " + expr.getClass());
			return false;
		}
	}

	public void processObject(StringLiteral sl) {
		region(sl.location, "literal");
	}

	public void processObject(NumericLiteral nl) {
		region(nl.location, "literal");
	}

	public void processObject(UnresolvedVar uv) {
		region(uv.location(), "var");
	}

	public void processObject(UnresolvedOperator op) {
//		System.out.println("op = " + op.op);
		region(op.location(), "symbol");
	}

	public void region(InputPosition loc, String style) {
		if (loc == null || loc.isFake())
			return;
		regionCollector.add(loc, style);
	}
	
	public ITypedRegion[] toArray() {
		return regionCollector.toArray();
	}
}
