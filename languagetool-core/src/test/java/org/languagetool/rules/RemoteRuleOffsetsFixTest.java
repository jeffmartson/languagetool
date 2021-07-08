package org.languagetool.rules;

import org.junit.Test;
import org.languagetool.AnalyzedSentence;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Demo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class RemoteRuleOffsetsFixTest
{

  private List<Integer> printShifts(String text) {
    int[] shifts = RemoteRule.computeOffsetShifts(text);
    for(int i = 0; i < text.length(); i++)  {
      System.out.printf("%c -> %d | ", text.charAt(i), shifts[i]);
    }
    System.out.println();
    return Arrays.stream(shifts).boxed().collect(Collectors.toList());
  }

  @Test
  public void testShiftCalculation() {
    assertEquals(Arrays.asList(0, 2, 3, 4, 5), printShifts("😁foo"));
    assertEquals(Arrays.asList(0, 1, 2, 3, 4, 6, 7, 8, 9, 10), printShifts("foo 😁 bar"));
    assertEquals(Arrays.asList(0, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14), printShifts("😁 foo 😁 bar"));

    assertEquals("1 code point, length 2 / 1", Arrays.asList(0, 2), printShifts("👪"));
    assertEquals("1 code point for each part, length 4 / 2, displayed as 1", Arrays.asList(0, 2, 4, 5), printShifts("👍🏼"));
    assertEquals("normal text", Arrays.asList(0), printShifts("a"));
  }

  private void testMatch(String text, int from, int to, int fixedFrom, int fixedTo) throws IOException {
    JLanguageTool lt = new JLanguageTool(new Demo());
    AnalyzedSentence s = lt.getAnalyzedSentence(text);
    Rule r = new FakeRule();
    List<RuleMatch> matches = Collections.singletonList(new RuleMatch(r, s, from, to, "test match"));
    RemoteRule.fixMatchOffsets(s, matches);
    assertEquals(fixedFrom, matches.get(0).getFromPos());
    assertEquals(fixedTo, matches.get(0).getToPos());
  }

  @Test
  public void testMatches() throws IOException {
    JLanguageTool lt = new JLanguageTool(new Demo());
    AnalyzedSentence s = lt.getAnalyzedSentence("😁foo bar");
    Rule r = new FakeRule();
    // multiple matches
    List<RuleMatch> matches = Arrays.asList(
      new RuleMatch(r, s, 0, 1, "Emoji"),
      new RuleMatch(r, s, 1, 4, "foo")
      );
    RemoteRule.fixMatchOffsets(s, matches);
    assertEquals(matches.get(0).getFromPos(), 0);
    assertEquals(matches.get(0).getToPos(), 2);
    assertEquals(matches.get(1).getFromPos(), 2);
    assertEquals(matches.get(1).getToPos(), 5);

    testMatch("😁foo bar", 0, 1, 0, 2);
    testMatch("😁foo bar", 1, 4, 2, 5);
    testMatch("👪", 0, 1, 0, 2);
    testMatch("👍🏼", 0, 2, 0, 4);
  }

}
