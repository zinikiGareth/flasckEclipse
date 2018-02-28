package org.flasck.test.eclipse;

import org.flasck.eclipse.editor.IRegionCollector;
import org.flasck.eclipse.editor.PartitionAccumulator;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.parser.Expression;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TestPartitioner {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();

	@Test
	public void test() throws Exception {
		final String input = "[(org.ziniki.Party \"Buyer\"), (org.ziniki.Party \"Seller\")]";
		IRegionCollector coll = context.mock(IRegionCollector.class);
		context.checking(new Expectations() {{
//			oneOf(coll).add(with(InputPositionMatcher.at(1,1).length(26).fake()), with("var"));
			oneOf(coll).add(with(InputPositionMatcher.at(1,2).length(16)), with("typename"));
			oneOf(coll).add(with(InputPositionMatcher.at(1,19).length(7)), with("literal"));
//			oneOf(coll).add(with(InputPositionMatcher.at(1,29).length(27).fake()), with("var"));
			oneOf(coll).add(with(InputPositionMatcher.at(1,30).length(16)), with("typename"));
			oneOf(coll).add(with(InputPositionMatcher.at(1,47).length(8)), with("literal"));
//			oneOf(coll).add(with(InputPositionMatcher.at(1,28).length(29).fake()), with("var"));
		}});
		Object top = new Expression().tryParsing(new Tokenizable(input));
		PartitionAccumulator acc = new PartitionAccumulator(coll);
		acc.processObject((ApplyExpr)top);
	}

}
