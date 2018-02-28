package org.flasck.test.eclipse;

import org.flasck.flas.blockForm.InputPosition;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class InputPositionMatcher extends TypeSafeMatcher<InputPosition> {

	private final int line;
	private final int pos;
	private Integer length;
	private boolean isFake;

	public InputPositionMatcher(int line, int pos) {
		this.line = line;
		this.pos = pos;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Match input position at line ");
		arg0.appendValue(line);
		arg0.appendText("; position ");
		arg0.appendValue(pos);
		if (length != null) {
			arg0.appendText("; length ");
			arg0.appendValue(length);
		}
		if (isFake)
			arg0.appendText("; want fake token");
	}

	@Override
	protected boolean matchesSafely(InputPosition arg0) {
		if (arg0.lineNo != line || arg0.off != pos)
			return false;
		System.out.println((arg0.pastEnd()-arg0.off) + " " + length);
		if (length != null && arg0.off+length != arg0.pastEnd())
			return false;
		if (isFake != arg0.isFake())
			return false;
		return true;
	}

	public static InputPositionMatcher at(int line, int pos) {
		return new InputPositionMatcher(line, pos);
	}

	public InputPositionMatcher length(int len) {
		this.length = len;
		return this;
	}

	public InputPositionMatcher fake() {
		this.isFake = true;
		return this;
	}
}
