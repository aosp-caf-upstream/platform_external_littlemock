/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.testing.littlemock;

import static com.google.testing.littlemock.LittleMock.anyBoolean;
import static com.google.testing.littlemock.LittleMock.anyByte;
import static com.google.testing.littlemock.LittleMock.anyChar;
import static com.google.testing.littlemock.LittleMock.anyDouble;
import static com.google.testing.littlemock.LittleMock.anyFloat;
import static com.google.testing.littlemock.LittleMock.anyInt;
import static com.google.testing.littlemock.LittleMock.anyLong;
import static com.google.testing.littlemock.LittleMock.anyObject;
import static com.google.testing.littlemock.LittleMock.anyShort;
import static com.google.testing.littlemock.LittleMock.anyString;
import static com.google.testing.littlemock.LittleMock.anyTimes;
import static com.google.testing.littlemock.LittleMock.atLeast;
import static com.google.testing.littlemock.LittleMock.atLeastOnce;
import static com.google.testing.littlemock.LittleMock.atMost;
import static com.google.testing.littlemock.LittleMock.between;
import static com.google.testing.littlemock.LittleMock.checkForProgrammingErrorsDuringTearDown;
import static com.google.testing.littlemock.LittleMock.doAnswer;
import static com.google.testing.littlemock.LittleMock.doNothing;
import static com.google.testing.littlemock.LittleMock.doReturn;
import static com.google.testing.littlemock.LittleMock.doThrow;
import static com.google.testing.littlemock.LittleMock.eq;
import static com.google.testing.littlemock.LittleMock.initMocks;
import static com.google.testing.littlemock.LittleMock.isA;
import static com.google.testing.littlemock.LittleMock.mock;
import static com.google.testing.littlemock.LittleMock.never;
import static com.google.testing.littlemock.LittleMock.reset;
import static com.google.testing.littlemock.LittleMock.times;
import static com.google.testing.littlemock.LittleMock.verify;
import static com.google.testing.littlemock.LittleMock.verifyNoMoreInteractions;
import static com.google.testing.littlemock.LittleMock.verifyZeroInteractions;

import junit.framework.TestCase;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Unit tests for the LittleMock class.
 *
 * @author hugohudson@gmail.com (Hugo Hudson)
 */
public class LittleMockTest extends TestCase {
  /**
   * Used in these unit tests to indicate that the method should throw a given type of exception.
   */
  @Target({ ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ShouldThrow {
    public Class<? extends Throwable> value();
    public String[] messages() default {};
  }

  @Mock private Foo mFoo;
  @Mock private Bar mBar;
  @Mock private BarSubtype mBarSubtype;
  @Captor private ArgumentCaptor<String> mCaptureString;
  @Captor private ArgumentCaptor<String> mCaptureAnotherString;
  @Captor private ArgumentCaptor<Integer> mCaptureInteger;
  @Captor private ArgumentCaptor<Callback> mCaptureCallback;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LittleMock.initMocks(this);
  }

  @Override
  protected void runTest() throws Throwable {
    Method method = getClass().getMethod(getName(), (Class[]) null);
    ShouldThrow shouldThrowAnnotation = method.getAnnotation(ShouldThrow.class);
    if (shouldThrowAnnotation != null) {
      try {
        super.runTest();
        fail("Should have thrown " + shouldThrowAnnotation.value());
      } catch (Throwable e) {
        if (!e.getClass().equals(shouldThrowAnnotation.value())) {
          fail("Should have thrown " + shouldThrowAnnotation.value() + " but threw " + e);
        }
        for (String requiredSubstring : shouldThrowAnnotation.messages()) {
          if (!e.getMessage().contains(requiredSubstring)) {
            fail("Error message didn't contain " + requiredSubstring + ", was " + e.getMessage());
          }
        }
        // Good, test passes.
      }
    } else {
      super.runTest();
    }
  }

  /** Simple interface for testing against. */
  public interface Callback {
    public void callMeNow();
  }

  /** Simple interface for testing against. */
  public interface Foo {
    public int anInt();
    public boolean aBoolean();
    public byte aByte();
    public short aShort();
    public long aLong();
    public float aFloat();
    public double aDouble();
    public char aChar();
    public String aString();
    public Object anObject();
    public Foo anInterface();
    public void add(String input);
    public void clear();
    public String get(int index);
    public String lookup(String string);
    public void getResultLater(Callback callback);
    public String findByInt(int input);
    public String findByBoolean(boolean input);
    public String findByByte(byte input);
    public String findByShort(short input);
    public String findByLong(long input);
    public String findByFloat(float input);
    public String findByDouble(double input);
    public String findByChar(char input);
    public void takesObject(Object input);
    public void takesList(List<String> input);
    public void takesBar(Bar bar);
    public void exceptionThrower() throws Exception;
    public Bar aBar();
    public BarSubtype aBarSubtype();
  }

  /** Simple interface for testing against. */
  public interface Bar {
    public void doSomething();
    public String twoStrings(String first, String second);
    public void mixedArguments(int first, String second);
    public void getResultLater(Callback callback);
  }

  /** Subtype of Bar. */
  public interface BarSubtype extends Bar {
    public void doSomethingElse();
  }

  /** Another interface for testing with. */
  public interface OnClickListener {
    void onClick(Bar bar);
  }

  public void testDefaultReturnTypesForNewMocks() {
    assertEquals(0, mFoo.anInt());
    assertEquals(false, mFoo.aBoolean());
    assertEquals(0, mFoo.aByte());
    assertEquals(0, mFoo.aShort());
    assertEquals(0L, mFoo.aLong());
    assertEquals(0.0f, mFoo.aFloat());
    assertEquals(0.0d, mFoo.aDouble());
    assertEquals('\u0000', mFoo.aChar());
    assertEquals(null, mFoo.aString());
    assertEquals(null, mFoo.anObject());
    assertEquals(null, mFoo.anInterface());
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testVerify_FailsIfNotDoneOnAProxy() {
    verify("hello").contains("something");
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testVerify_FailsIfNotCreatedByOurMockMethod() {
    verify(createNotARealMock()).add("something");
  }

  public void testVerify_SuccessfulVerification() {
    mFoo.add("something");
    verify(mFoo).add("something");
  }

  public void testVerify_SuccessfulVerification_NormalOrder() {
    mFoo.add("something");
    mFoo.add("something else");
    verify(mFoo).add("something");
    verify(mFoo).add("something else");
  }

  public void testVerify_SuccessfulVerification_ReverseOrder() {
    mFoo.add("something");
    mFoo.add("something else");
    verify(mFoo).add("something else");
    verify(mFoo).add("something");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_MeansOnlyOnceSoShouldFailIfCalledTwice() {
    mFoo.add("something");
    mFoo.add("something");
    verify(mFoo).add("something");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_FailedVerification_CalledWithWrongArgument() {
    mFoo.add("something else");
    verify(mFoo).add("something");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_FailedVerification_WasNeverCalled() {
    verify(mFoo).add("something");
  }

  public void testVerify_TimesTwice_Succeeds() {
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, LittleMock.times(2)).add("jim");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_TimesTwice_ButThreeTimesFails() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, LittleMock.times(2)).add("jim");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_TimesTwice_ButOnceFails() {
    mFoo.add("jim");
    verify(mFoo, LittleMock.times(2)).add("jim");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_TimesTwice_DifferentStringsFails() {
    mFoo.add("jim");
    mFoo.add("bob");
    verify(mFoo, LittleMock.times(2)).add("jim");
  }

  public void testVerify_TimesTwice_WorksWithAnyString() {
    mFoo.add("jim");
    mFoo.add("bob");
    verify(mFoo, LittleMock.times(2)).add(anyString());
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_TimesTwice_FailsIfJustOnceWithAnyString() {
    mFoo.add("bob");
    verify(mFoo, LittleMock.times(2)).add(anyString());
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_TimesTwice_FailsIfThreeTimesWithAnyString() {
    mFoo.add("bob");
    mFoo.add("jim");
    mFoo.add("james");
    verify(mFoo, LittleMock.times(2)).add(anyString());
  }

  public void testVerify_Never_Succeeds() {
    verify(mFoo, never()).add("jim");
    verify(mFoo, never()).anInt();
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_Never_FailsIfWasCalled() {
    mFoo.add("jim");
    verify(mFoo, never()).add("jim");
  }

  public void testVerify_Never_PassesIfArgumentsDontMatch() {
    mFoo.add("bobby");
    verify(mFoo, never()).add("jim");
  }

  public void testVerify_AtLeastOnce_SuceedsForOneCall() {
    mFoo.add("jim");
    verify(mFoo, atLeastOnce()).add("jim");
  }

  public void testVerify_AtLeastOnce_SuceedsForThreeCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atLeastOnce()).add("jim");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_AtLeastOnce_FailsForNoCalls() {
    verify(mFoo, atLeastOnce()).add("jim");
  }

  public void testVerify_AtLeastThreeTimes_SuceedsForThreeCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atLeast(3)).add("jim");
  }

  public void testVerify_AtLeastThreeTimes_SuceedsForFiveCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atLeast(3)).add("jim");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_AtLeastThreeTimes_FailsForTwoCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atLeast(3)).add("jim");
  }

  public void testVerify_AtMostThreeTimes_SuceedsForThreeCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atMost(3)).add("jim");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_AtMostThreeTimes_FailsForFiveCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atMost(3)).add("jim");
  }

  public void testVerify_AtMostThreeTimes_SucceedsForTwoCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atMost(3)).add("jim");
  }

  public void testVerify_AtMostThreeTimes_SucceedsForNoCalls() {
    verify(mFoo, atMost(3)).add("jim");
  }

  public void testVerify_BetweenTwoAndFour_SucceedsForTwoCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, between(2, 4)).add("jim");
  }

  public void testVerify_BetweenTwoAndFour_SucceedsForFourCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, between(2, 4)).add("jim");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_BetweenTwoAndFour_FailsForOneCall() {
    mFoo.add("jim");
    verify(mFoo, between(2, 4)).add("jim");
  }

  @ShouldThrow(AssertionError.class)
  public void testVerify_BetweenTwoAndFour_FailsForFiveCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, LittleMock.between(2, 4)).add("jim");
  }

  public void testDoReturnWhen_SimpleReturn() {
    doReturn("first").when(mFoo).get(0);
    assertEquals("first", mFoo.get(0));
  }

  public void testDoReturnWhen_LastStubCallWins() {
    doReturn("first").when(mFoo).get(0);
    doReturn("second").when(mFoo).get(0);
    assertEquals("second", mFoo.get(0));
  }

  public void testDoReturnWhen_CorrectStubMethodIsChosen() {
    doReturn("one").when(mFoo).get(1);
    doReturn("two").when(mFoo).get(2);
    assertEquals("one", mFoo.get(1));
    assertEquals("two", mFoo.get(2));
  }

  public void testDoReturnWhen_UnstubbedMethodStillReturnsDefault() {
    doReturn("one").when(mFoo).get(1);
    assertEquals(null, mFoo.get(2));
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testDoReturnWhen_CalledOnString() {
    doReturn("first").when("hello").contains("something");
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testDoReturnWhen_CalledOnNonMock() {
    doReturn("first").when(createNotARealMock()).get(0);
  }

  public void testDoReturnWhen_CanAlsoBeVerified() {
    // Mockito home page suggests that you don't verify stubbed calls.
    // I agree.  They support it anyway.  So will I.
    doReturn("one").when(mFoo).get(8);
    mFoo.get(8);
    verify(mFoo).get(8);
  }

  public void testDoReturn_CanPassIntForIntMethod() {
    doReturn(90).when(mFoo).anInt();
    assertEquals(90, mFoo.anInt());
  }

  // Interesting, you have to explicity convert the Integer class back into an int before it
  // is happy to accept this.
  public void testDoReturn_CanPassIntegerClassForIntMethod() {
    doReturn((int) Integer.valueOf(10)).when(mFoo).anInt();
    assertEquals(10, mFoo.anInt());
  }

  public void testDoReturn_PrimitiveLong() {
    doReturn((long) Long.valueOf(10L)).when(mFoo).aLong();
    assertEquals(10L, mFoo.aLong());
  }

  public void testDoReturn_PrimitiveTypes() {
    doReturn(5).when(mFoo).anInt();
    assertEquals(5, mFoo.anInt());
    doReturn((short) 5).when(mFoo).aShort();
    assertEquals(5, mFoo.aShort());
    doReturn(true).when(mFoo).aBoolean();
    assertEquals(true, mFoo.aBoolean());
    doReturn((byte) 3).when(mFoo).aByte();
    assertEquals(3, mFoo.aByte());
    doReturn(0.6f).when(mFoo).aFloat();
    assertEquals(0.6f, mFoo.aFloat());
    doReturn(0.7).when(mFoo).aDouble();
    assertEquals(0.7, mFoo.aDouble());
    doReturn('c').when(mFoo).aChar();
    assertEquals('c', mFoo.aChar());
    assertEquals(null, mFoo.anInterface());
  }

  @ShouldThrow(RuntimeException.class)
  public void testDoThrow_SimpleException() {
    doThrow(new RuntimeException()).when(mFoo).aDouble();
    mFoo.aDouble();
  }

  public void testDoThrow_IfItDoesntMatchItIsntThrown() {
    doThrow(new RuntimeException()).when(mFoo).aDouble();
    mFoo.aChar();
  }

  @ShouldThrow(RuntimeException.class)
  public void testDoThrow_KeepsThrowingForever() {
    doThrow(new RuntimeException()).when(mFoo).aDouble();
    try {
      mFoo.aDouble();
      fail("Should have thrown a RuntimeException");
    } catch (RuntimeException e) {
      // Expected.
    }
    // This second call should also throw a RuntimeException.
    mFoo.aDouble();
  }

  public void testDoNothing() {
    doNothing().when(mFoo).add("first");
    mFoo.add("first");
  }

  public void testVerifyZeroInteractions_PassesWhenNothingHasHappened() {
    verifyZeroInteractions(mFoo);
  }

  @ShouldThrow(AssertionError.class)
  public void testVerifyZeroInteractions_FailsIfSomethingHasHappened() {
    mFoo.aBoolean();
    verifyZeroInteractions(mFoo);
  }

  public void testVerifyZeroInteractions_HappyWithMultipleArguments() {
    verifyZeroInteractions(mFoo, mBar);
  }

  @ShouldThrow(AssertionError.class)
  public void testVerifyZeroInteractions_ShouldFailEvenIfOtherInteractionsWereFirstVerified() {
    mFoo.add("test");
    verify(mFoo).add("test");
    verifyZeroInteractions(mFoo);
  }

  public void testVerifyEq_Method() {
    mFoo.add("test");
    verify(mFoo).add(eq("test"));
  }

  public void testVerifyEq_MethodWithTwoSameTypeParameters() {
    mBar.twoStrings("first", "test");
    verify(mBar).twoStrings(eq("first"), eq("test"));
  }

  public void testVerifyEq_MethodWithTwoDifferentTypeParameters() {
    mBar.mixedArguments(8, "test");
    verify(mBar).mixedArguments(eq(8), eq("test"));
  }

  @ShouldThrow(AssertionError.class)
  public void testVerifyEq_MethodFailsIfNotEqual() {
    mFoo.add("bob");
    verify(mFoo).add(eq("jim"));
  }

  @ShouldThrow(AssertionError.class)
  public void testVerifyEq_MethodFailsIfJustOneIsNotEqual() {
    mBar.twoStrings("first", "second");
    verify(mBar).twoStrings(eq("first"), eq("third"));
  }

  @ShouldThrow(AssertionError.class)
  public void testVerifyEq_MethodFailsIfSameParamsButInWrongOrder() {
    mBar.twoStrings("first", "second");
    verify(mBar).twoStrings(eq("second"), eq("first"));
  }

  public void testCapture_SimpleCapture() {
    // We verify that there are zero matchers by using the check for programming errors method.
    checkForProgrammingErrorsDuringTearDown();
    mFoo.add("test");
    verify(mFoo).add(mCaptureString.capture());
    assertEquals("test", mCaptureString.getValue());
    checkForProgrammingErrorsDuringTearDown();
  }

  public void testCapture_DuringStubbing() {
    checkForProgrammingErrorsDuringTearDown();
    doReturn("hello").when(mFoo).lookup(mCaptureString.capture());

    assertEquals("hello", mFoo.lookup("what"));
    assertEquals("what", mCaptureString.getValue());
  }

  public void testCapture_TwoCallbacksDuringStubbing() {
    checkForProgrammingErrorsDuringTearDown();
    doNothing().when(mFoo).add(mCaptureString.capture());
    doNothing().when(mFoo).getResultLater(mCaptureCallback.capture());

    mFoo.add("hi");
    assertEquals("hi", mCaptureString.getValue());

    Callback callback = createNoOpCallback();
    mFoo.getResultLater(callback);
    assertEquals(callback, mCaptureCallback.getValue());
  }

  // TODO(hugohudson): 6. Is this impossible to fix?  You can't pass a
  // createCapture().capture() into a method expecting an int, because the capture
  // method returns null, and that gets auto-boxed to Integer on the way out of the
  // capture method, then auto-unboxed into an int when being passed to the underlying
  // method, which gives the NPE.  How best can we fix this?
  // It's not like you need to anyway - there's no point / need to capture a primitive,
  // just use eq(5) for example.
  public void testCapture_NPEWhenUnboxing() {
    try {
      mBar.mixedArguments(5, "ten");
      verify(mBar).mixedArguments(mCaptureInteger.capture(), mCaptureString.capture());
      // These lines are never reached, the previous line throws an NPE.
      fail("You shouldn't be able to reach here");
      assertEquals(Integer.valueOf(5), mCaptureInteger.getValue());
      assertEquals("ten", mCaptureString.getValue());
    } catch (NullPointerException e) {
      // Expected, unfortunately.
      // Now also we're in the situation where we have some captures hanging about in the static
      // variable, which will cause the tear down of this method to fail - we can clear them
      // as follows:
      try {
        checkForProgrammingErrorsDuringTearDown();
        fail("Expected an IllegalStateException");
      } catch (IllegalStateException e2) {
        // Expected.
      }
    }
  }

  public void testCapture_MultiCapture() {
    mFoo.lookup("james");
    mFoo.add("whinny");
    mFoo.add("jessica");
    verify(mFoo).lookup(mCaptureString.capture());
    verify(mFoo, atLeastOnce()).add(mCaptureAnotherString.capture());
    assertEquals("james", mCaptureString.getValue());
    assertEquals("jessica", mCaptureAnotherString.getValue());
    assertEquals(newList("whinny", "jessica"), mCaptureAnotherString.getAllValues());
  }

  public static <T> List<T> newList(T... things) {
    ArrayList<T> list = new ArrayList<T>();
    for (T thing : things) {
      list.add(thing);
    }
    return list;
  }

  public void testAnyString() {
    doReturn("jim").when(mFoo).lookup(anyString());
    assertEquals("jim", mFoo.lookup("barney"));
  }

  public void testAnyString_ObjectArgument() {
    // It can also be passed to a method that takes object.
    mFoo.takesObject("barney");
    verify(mFoo).takesObject(anyString());
  }

  @ShouldThrow(AssertionError.class)
  public void testAnyString_ObjectValue() {
    mFoo.takesObject(new Object());
    verify(mFoo).takesObject(anyString());
  }

  public void testAnyObject() {
    doReturn("jim").when(mFoo).lookup((String) anyObject());
    assertEquals("jim", mFoo.lookup("barney"));
  }

  public void testAnyPrimitives() {
    mFoo.findByBoolean(true);
    mFoo.findByInt(10000);
    mFoo.findByByte((byte) 250);
    mFoo.findByShort((short) 6666);
    mFoo.findByLong(13L);
    mFoo.findByFloat(8f);
    mFoo.findByDouble(1.1);
    mFoo.findByChar('h');
    verify(mFoo).findByBoolean(anyBoolean());
    verify(mFoo).findByInt(anyInt());
    verify(mFoo).findByByte(anyByte());
    verify(mFoo).findByShort(anyShort());
    verify(mFoo).findByLong(anyLong());
    verify(mFoo).findByFloat(anyFloat());
    verify(mFoo).findByDouble(anyDouble());
    verify(mFoo).findByChar(anyChar());
  }

  public void testAnyPrimitivesWhenMatching() {
    doReturn("a").when(mFoo).findByBoolean(anyBoolean());
    doReturn("b").when(mFoo).findByInt(anyInt());
    doReturn("c").when(mFoo).findByByte(anyByte());
    doReturn("d").when(mFoo).findByShort(anyShort());
    doReturn("e").when(mFoo).findByLong(anyLong());
    doReturn("f").when(mFoo).findByFloat(anyFloat());
    doReturn("g").when(mFoo).findByDouble(anyDouble());
    doReturn("h").when(mFoo).findByChar(anyChar());
    assertEquals("a", mFoo.findByBoolean(true));
    assertEquals("b", mFoo.findByInt(388));
    assertEquals("c", mFoo.findByByte((byte) 38));
    assertEquals("d", mFoo.findByShort((short) 16));
    assertEquals("e", mFoo.findByLong(1000000L));
    assertEquals("f", mFoo.findByFloat(15.3f));
    assertEquals("g", mFoo.findByDouble(13.3));
    assertEquals("h", mFoo.findByChar('i'));
  }

  public void testReset_NoInteractionsAfterReset() {
    mFoo.aByte();
    reset(mFoo);
    verifyZeroInteractions(mFoo);
  }

  @ShouldThrow(AssertionError.class)
  public void testReset_VerifyFailsAfterReset() {
    mFoo.aByte();
    reset(mFoo);
    verify(mFoo).aByte();
  }

  public void testCapture_BothBeforeAndAfter() {
    doNothing().when(mFoo).add(mCaptureString.capture());
    mFoo.add("first");
    verify(mFoo).add(mCaptureAnotherString.capture());
    assertEquals("first", mCaptureString.getValue());
    assertEquals("first", mCaptureAnotherString.getValue());
  }

  public void testDoAction_NormalOperation() {
    doAnswer(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return Boolean.TRUE;
      }
    }).when(mFoo).aBoolean();
    assertEquals(true, mFoo.aBoolean());
  }

  public void testComplexSituationWithCallback() {
    // I want to specify that when hasCallback(Callback) method is called, the framework
    // should immediately call on the captured callback.
    doAnswer(new CallCapturedCallbackCallable())
        .when(mBar).getResultLater(mCaptureCallback.capture());

    // The test.
    mBar.getResultLater(new Callback() {
      @Override
      public void callMeNow() {
        mFoo.add("yes");
      }
    });

    verify(mFoo).add("yes");
  }

  @ShouldThrow(IOException.class)
  public void testDoAction_CanThrowDeclaredException() throws Exception {
    doAnswer(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        throw new IOException("Problem");
      }
    }).when(mFoo).exceptionThrower();
    mFoo.exceptionThrower();
  }

  @ShouldThrow(RuntimeException.class)
  public void testDoAction_CanThrowUndelcaredException() {
    doAnswer(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        throw new RuntimeException("Problem");
      }
    }).when(mFoo).aBoolean();
    mFoo.aBoolean();
  }

  public void testRunThisIsAnAliasForDoAction() {
    doAnswer(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return Boolean.TRUE;
      }
    }).when(mFoo).aBoolean();
    assertEquals(true, mFoo.aBoolean());
  }

  public void testVerifyingTwice() {
    // Behaviour from Mockito docs online seems to be undefined for what should happen if you
    // try to verify the same behaviour twice.
    // I'm going to make a call on this one until I have more concrete information, and my
    // call is that it is okay to verify the same thing twice - a verify doesn't "consume"
    // the other verifications.
    // Thus this will pass:
    mFoo.aByte();
    verify(mFoo).aByte();
    verify(mFoo).aByte();
  }

  public void testVerifyNoMoreInteractions_SuccessWhenNoInteractions() {
    // Not absolutely certain how this is supposed to behave.
    // My guess is that every verify "tags" all the methods it verifies against.
    // Then verifyNoMoreInteractions() will pass only if there are no "untagged" method calls.
    // Thus, for a start, no interactions will pass.
    verifyNoMoreInteractions(mFoo, mBar);
  }

  public void testVerifyNoMoreInteractions_SuccessWhenOneActionWasVerified() {
    mFoo.aBoolean();
    verify(mFoo).aBoolean();
    verifyNoMoreInteractions(mFoo, mBar);
  }

  @ShouldThrow(AssertionError.class)
  public void testVerifyNoMoreInteractions_FailsWhenOneActionWasNotVerified() {
    mFoo.aBoolean();
    verifyNoMoreInteractions(mFoo, mBar);
  }

  public void testVerifyNoMoreInteractions_SucceedsWhenAllActionsWereVerified() {
    mFoo.get(3);
    mFoo.get(20);
    verify(mFoo, atLeastOnce()).get(anyInt());
    verifyNoMoreInteractions(mFoo);
  }

  @ShouldThrow(AssertionError.class)
  public void testVerifyNoMoreInteractions_FailsWhenMostButNotAllActionsWereVerified() {
    mFoo.get(3);
    mFoo.get(20);
    mFoo.aByte();
    verify(mFoo, atLeastOnce()).get(anyInt());
    verifyNoMoreInteractions(mFoo);
  }

  public void testVerifyNoMoreInteractions_ShouldIngoreStubbedCalls() {
    doReturn("hi").when(mFoo).get(8);
    mFoo.get(8);
    verifyNoMoreInteractions(mFoo);
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testMatchers_WrongNumberOfMatchersOnStubbingCausesError() {
    doReturn("hi").when(mBar).twoStrings("jim", eq("bob"));
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testMatchers_WrongNumberOfMatchersOnVerifyCausesError() {
    verify(mBar).twoStrings("jim", eq("bob"));
  }

  @ShouldThrow(IllegalStateException.class)
  public void testCreateACaptureButDontUseItShouldFailAtNextVerify() {
    // If we create a capture illegally, outside of a method call, like so:
    mCaptureString.capture();
    // Then we will have illegally created an extra matcher object that we shouldn't have
    // created that is now sitting on the stack, and that will confuse the next true method
    // call on the mock object.
    // Thus we should check in the verify() method that there are *no matchers* on the static
    // list, as this would indicate a programming error such as the above.
    verify(mFoo, anyTimes()).aBoolean();
  }

  @ShouldThrow(IllegalStateException.class)
  public void testCreateACaptureButDontUseItShouldFailAtNextVerify_AlsoNoMoreInteractions() {
    // Same result desired as in previous test.
    mCaptureString.capture();
    verifyNoMoreInteractions(mFoo);
  }

  @ShouldThrow(IllegalStateException.class)
  public void testCreateACaptureButDontUseItShouldFailAtNextVerify_AlsoZeroInteraction() {
    mCaptureString.capture();
    verifyZeroInteractions(mFoo);
  }

  @ShouldThrow(IllegalStateException.class)
  public void testCheckStaticVariablesMethod() {
    // To help us avoid programming errors, I'm adding a method that you can call from tear down,
    // which will explode if there is anything still left in your static variables at the end
    // of the test (so that you know you did something wrong) and that also clears that static
    // variable (so that the next test won't fail).  It should fail if we create a matcher
    // be it a capture or anything else that is then not consumed.
    anyInt();
    checkForProgrammingErrorsDuringTearDown();
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testCantPassNullToVerifyCount() {
    verify(mFoo, null).aBoolean();
  }

  public void testInjectionInNestedClasses() throws Exception {
    class Outer {
      @Mock protected Foo outerMock;
    }
    class Inner extends Outer {
      @Mock protected Foo innerMock;
    }
    Inner inner = new Inner();
    assertNull(inner.innerMock);
    assertNull(inner.outerMock);
    initMocks(inner);
    assertNotNull(inner.innerMock);
    assertNotNull(inner.outerMock);
  }

  public void testIsA_Succeeds() {
    mFoo.takesObject(new Object());
    verify(mFoo).takesObject(isA(Object.class));
  }

  public void testIsA_WithSubclass() {
    mFoo.takesObject("hello");
    verify(mFoo).takesObject(isA(Object.class));
    verify(mFoo).takesObject(isA(String.class));
  }

  @ShouldThrow(AssertionError.class)
  public void testIsA_FailsWithSuperclass() {
    mFoo.takesObject(new Object());
    verify(mFoo).takesObject(isA(String.class));
  }

  public void testIsA_WillAcceptNull() {
    mFoo.takesObject(null);
    verify(mFoo).takesObject(isA(Object.class));
    verify(mFoo).takesObject(isA(String.class));
  }

  public void testIsA_MatchSubtype() {
    mFoo.takesBar(mBarSubtype);
    verify(mFoo).takesBar(isA(BarSubtype.class));
  }

  @ShouldThrow(AssertionError.class)
  public void testIsA_MatchSubtypeFailed() {
    mFoo.takesBar(mBar);
    verify(mFoo).takesBar(isA(BarSubtype.class));
  }

  @ShouldThrow(value = AssertionError.class,
      messages = {"cannot verify call to", "boolean java.lang.Object.equals(java.lang.Object)"})
  public void testVerifyEquals_ShouldFail() {
    mFoo.equals(null);
    verify(mFoo).equals(null);
  }

  @ShouldThrow(value = AssertionError.class,
      messages = {"cannot verify call to", "int java.lang.Object.hashCode()"})
  public void testVerifyHashCode_ShouldFail() {
    mFoo.hashCode();
    verify(mFoo).hashCode();
  }

  @ShouldThrow(value = AssertionError.class,
      messages = {"cannot verify call to", "java.lang.String java.lang.Object.toString()"})
  public void testVerifyToString_ShouldFail() {
    mFoo.toString();
    verify(mFoo).toString();
  }

  @ShouldThrow(value = AssertionError.class,
      messages = {"cannot stub call to", "boolean java.lang.Object.equals(java.lang.Object)"})
  public void testStubEquals_ShouldFail() {
    doReturn(false).when(mFoo).equals(null);
  }

  @ShouldThrow(value = AssertionError.class,
      messages = {"cannot stub call to", "int java.lang.Object.hashCode()"})
  public void testStubHashCode_ShouldFail() {
    doReturn(0).when(mFoo).hashCode();
  }

  @ShouldThrow(value = AssertionError.class,
      messages = {"cannot stub call to", "java.lang.String java.lang.Object.toString()"})
  public void testStubToString_ShouldFail() {
    doReturn("party").when(mFoo).toString();
  }

  public void testEqualsMethod_DoesntCountAsInteraction() {
    mFoo.takesBar(mBar);
    verify(mFoo).takesBar(mBar);
    verifyNoMoreInteractions(mBar);
  }

  public void testHashCodeMethod_DoesntCountAsInteraction() {
    mFoo.hashCode();
    verifyNoMoreInteractions(mFoo);
  }

  public void testToStringMethod_DoesntCountAsInteraction() {
    mFoo.toString();
    verifyNoMoreInteractions(mFoo);
  }

  public void testEquals_OnMock() {
    assertTrue(mFoo.equals(mFoo));
  }

  public void testHashCode_OnMock() {
    // The hashCode() is checked against zero, the default int value, to make sure it is actually
    // being treated differently.
    // It is possible for a hashCode() to be zero, but very unlikely.
    assertNotSame(0, mFoo.hashCode());
  }

  public void testToString_OnMock() {
    assertTrue(mFoo.toString().contains(Foo.class.getName()));
  }

  public void testErrorMessages_NoArgMethodAndNoInteractions() {
    /* I would like the error message to look like this:
     * Expected exactly 2 calls to:
     *   mFoo.aBoolean()
     *   at the.line.where.the.verify.happened:xxx
     *
     * No method calls happened to this mock
     */
    int verifyLineNumber = 0;
    try {
      verifyLineNumber = getNextLineNumber();
      verify(mFoo, times(2)).aBoolean();
      fail("Should have thrown an assertion error");
    } catch (AssertionError e) {
      // Good, verify that the message is exactly as expected.
      String expectedMessage =
          "\nExpected exactly 2 calls to:\n"
          + "  mFoo.aBoolean()\n"
          + "  at " + singleLineStackTrace(verifyLineNumber) + "\n"
          + "\n"
          + "No method calls happened to this mock\n";
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  public void testErrorMessages_SomeArgsMethodAndSomeInteractions() {
    /* I would like the error message to look like this:
     * Expected exactly 1 call to:
     *   mFoo.add(String)
     *   at the.line.where.the.verify.happened:xxx
     *
     * Method calls that did happen:
     *   mFoo.aByte()
     *   at the.line.where.the.byte.happened:xxx
     *   mFoo.findByBoolean(boolean)
     *   at the line.where.the.boolean.happened:xxx
     */
    int aByteLineNumber = 0;
    int findByBooleanLineNumber = 0;
    int verifyLineNumber = 0;
    try {
      aByteLineNumber = getNextLineNumber();
      mFoo.aByte();
      findByBooleanLineNumber = getNextLineNumber();
      mFoo.findByBoolean(true);
      verifyLineNumber = getNextLineNumber();
      verify(mFoo).add("jim");
      fail("Should have thrown an assertion error");
    } catch (AssertionError e) {
      // Good, verify that the message is exactly as expected.
      String expectedMessage =
          "\nExpected exactly 1 call to:\n"
          + "  mFoo.add(String)\n"
          + "  at " + singleLineStackTrace(verifyLineNumber) + "\n"
          + "\n"
          + "Method calls that did happen:\n"
          + "  mFoo.aByte()\n"
          + "  at " + singleLineStackTrace(aByteLineNumber) + "\n"
          + "  mFoo.findByBoolean(boolean)\n"
          + "  at " + singleLineStackTrace(findByBooleanLineNumber) + "\n";
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  public void testErrorMessage_DoReturnExplainsWhatWentWrong() {
    /* I would like the error message to look like this:
     * Can't return Long from stub for:
     *   (int) mFoo.anInt()
     *   at the.line.where.the.assignment.happened:xxx
     */
    int lineNumber = 0;
    try {
      lineNumber = getNextLineNumber();
      doReturn(10L).when(mFoo).anInt();
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Good, expected, verify the message.
      String expectedMessage =
          "\nCan't return Long from stub for:\n"
          + "  (int) mFoo.anInt()\n"
          + "  at " + singleLineStackTrace(lineNumber) + "\n";
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  public void testErrorMessage_DoReturnAlsoHasGoodErrorMessageForVoidMethods() {
    /* I would like the error message to look like this:
     * Can't return String from stub for:
     *   (void) mFoo.add(String)
     *   at the.line.where.the.assignment.happened:xxx
     */
    int lineNumber = 0;
    try {
      lineNumber = getNextLineNumber();
      doReturn("hello").when(mFoo).add("jim");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Good, expected, verify the message.
      String expectedMessage =
          "\nCan't return String from stub for:\n"
          + "  (void) mFoo.add(String)\n"
          + "  at " + singleLineStackTrace(lineNumber) + "\n";
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testDoReturn_ThisShouldFailSinceDoubleIsNotAString() {
    doReturn("hello").when(mFoo).aDouble();
  }

  public void testDoReturn_ThisShouldPassSinceStringCanBeReturnedFromObjectMethod() {
    doReturn("hello").when(mFoo).anObject();
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testDoReturn_ThisShouldFailSinceObjectCantBeReturnedFromString() {
    doReturn(new Object()).when(mFoo).aString();
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testDoReturn_ThisShouldFailSinceBarIsNotSubtypeOfBarSubtype() {
    doReturn(mBar).when(mFoo).aBarSubtype();
  }

  public void testDoReturn_ThisShouldPassSinceBarSubTypeIsABar() {
    doReturn(mBarSubtype).when(mFoo).aBar();
  }

  // TODO(hugohudson): 7. Should fix this.
//  @ShouldThrow(IllegalArgumentException.class)
  public void testDoReturn_ThisShouldFailBecauseNullIsNotAByte() {
    doReturn(null).when(mFoo).aByte();
  }

  // TODO(hugohudson): 7. Should fix this.
  // Once we fix the previous test we won't need this one.
  // I'm just demonstrating that currently this fails with NPE at use-time not at stub-time.
  @ShouldThrow(NullPointerException.class)
  public void testDoReturn_ThisShouldFailBecauseNullIsNotAByte2() {
    doReturn(null).when(mFoo).aByte();
    mFoo.aByte();
  }

  public void testDoReturn_ThisShouldPassSinceNullIsAnObject() {
    doReturn(null).when(mFoo).anObject();
  }

  // TODO(hugohudson): 7. Should fix this.
  // At present we aren't catching this, and would have difficulty doing so since we don't know
  // the type of the callable.
//  @ShouldThrow(IllegalArgumentException.class)
  public void testDoAnswer_ThisShouldFailSinceStringIsNotAByte() {
    doAnswer(new Callable<String>() {
      @Override public String call() throws Exception { return "hi"; }
    }).when(mFoo).aByte();
  }

  // TODO(hugohudson): 7. Should fix this to give proper message.
  // We could at least give a good message saying why you get failure - saying that your string
  // is not a byte, and pointing to where you stubbed it.
  @ShouldThrow(ClassCastException.class)
  public void testDoAnswer_ThisShouldFailSinceStringIsNotAByte2() {
    doAnswer(new Callable<String>() {
      @Override public String call() throws Exception { return "hi"; }
    }).when(mFoo).aByte();
    mFoo.aByte();
  }

  @ShouldThrow(value = IllegalArgumentException.class,
      messages = { "  (Bar) mFoo.aBar()" })
  public void testDoAnswer_ShouldHaveSimpleNameOnReturnValue() {
    doReturn("hi").when(mFoo).aBar();
  }

  @ShouldThrow(IllegalArgumentException.class)
  public void testCantCreateMockOfNullType() {
    mock(null);
  }

  @ShouldThrow(value = AssertionError.class,
      messages = { "Expected exactly 1 call to:", "onClickListener.onClick(Bar)" })
  public void testCreateMockWithNullFieldName() {
    OnClickListener mockClickListener = mock(OnClickListener.class);
    verify(mockClickListener).onClick(null);
  }

  public void testDoubleVerifyNoProblems() {
    // Reusing a mock after a verify should be fine.
    // There was a bug with this, let's check it doesn't regress.
    mFoo.aBar();
    verify(mFoo).aBar();

    mFoo.aByte();
    verify(mFoo).aByte();
  }

  // TODO(hugohudson): 5. Every @ShouldThrow method should be improved by adding test for the
  // content of the error message.  First augment the annotation to take a String which must
  // form a substring of the getMessage() for the Exception, then check that the exceptions
  // thrown are as informative as possible.

  // TODO(hugohudson): 5. Add InOrder class, so that we can check that the given methods on
  // the given mocks happen in the right order.  It will be pretty easy to do.  The syntax
  // looks like this:
  // InOrder inOrder = inOrder(firstMock, secondMock);
  // inOrder.verify(firstMock).firstMethod();
  // inOrder.verify(secondMock).secondMethod();
  // This allows us to verify that the calls happened in the desired order.
  // By far the simplest way to do this is have a static AtomicInteger on the class which
  // indicates exactly when every method call happened, and then just compare order based on
  // that.

  // TODO(hugohudson): 5. Make the doReturn() method take variable arguments.
  // The syntax is:
  // doReturn(1, 2, 3).when(mFoo).anInt();
  // And of course means that the method returns 1 the first time, 2, the second and 3 the third.
  // Note that this doesn't imply verification, and I presume that the 3 is always returned for
  // the 4th and subsequent times.

  // TODO(hugohudson): 6. Could also offer a nicer syntax for multiple returns like this:
  // How about doReturn().thenThrow().thenReturn().when(mFoo).aDouble();

  // TODO(hugohudson): 5. Get around to implementing Mockito's when() syntax.
  // I don't really like it, because it means a lot more static nonsense, with yet more
  // opportunities to shoot oneself in the foot.
  // Plus, where's the upside in more than one way to do the same thing - it just gets confusing.
  // But, I imagine that plenty of people who are familiar with Mockito will want this, so I
  // probably should do it, plus there's a good argument that it allows typechecking of the
  // method calls, so I guess we probably should.  Looks like this:
  // when(mock.foo(0)).thenReturn(1);
  // when(mock.foo(1)).thenThrow(new RuntimeException)
  // when(mock.foo(anyInt())).thenReturn("bar")
  // when(mock.foo(argThat(isValid()))).thenReturn("element")
  // when(mock.someMethod("some arg")).thenThrow(new RuntimeException()).thenReturn("foo");
  // when(mock.someMethod("some arg")).thenReturn("one", "two", "three");
  // when(mock.someMethod(anyString())).thenAnswer(new Answer() {
  //   @Override
  //   Object answer(InvocationOnMock invocation) {
  //     Object[] args = invocation.getArguments();
  //     Object mock = invocation.getMock();
  //     return "called with arguments: " + args;
  //   }
  // }

  // TODO(hugohudson): 6. Again we can chain things like doNothing() then doThrow() I suppose.
  // doNothing().doThrow(new RuntimeException()).when(mock).someVoidMethod();

  // TODO(hugohudson): 6. I really like the idea of implementing the Spy, which is a wrapper on
  // a real object and delegates all calls to that real object, but allows you to intercept
  // the ones that you want to.
  // Sounds like it will be particularly good for testing legacy code.
  // But also wouldn't be so much use without us being able to mock concrete classes, which I
  // imagine is not on the cards for a while yet.

  // TODO(hugohudson): 6. Could possibly look into more aliases for the common methods, so that
  // you can do the 'given... when... assertThat...' pattern as follows:
  //  //given
  //  given(seller.askForBread()).willReturn(new Bread());
  //  //when
  //  Goods goods = shop.buyBread();
  //  //then
  //  assertThat(goods, containBread());

  // TODO(hugohudson): 6. How about timeouts that are mentioned in Mockito docs?
  // Sounds like a good idea.  Would look like this:
  // verify(mFoo, within(50).milliseconds()).get(0);

  // TODO(hugohudson): 6. Consider bringing back in the async code I wrote for the AndroidMock
  // framework so that you can expect to wait for the method call.
  // Although obviously we're more keen on encouraging people to write unit tests that don't
  // require async behaviour in the first place.

  /** Returns the line number of the line following the method call. */
  private int getNextLineNumber() {
    return new Exception().getStackTrace()[1].getLineNumber() + 1;
  }

  /** Returns a string like: "com.google.foo.TestFoo.testMethod(TestFoo:50)" */
  private String singleLineStackTrace(int lineNumber) {
    return getClass().getName() + "." + getName() + "(" + getClass().getSimpleName() +
        ".java:" + lineNumber + ")";
  }

  /** Simple callable that invokes the callback captured in the callback member variable. */
  private class CallCapturedCallbackCallable implements Callable<Object> {
    @Override
    public Object call() {
      mCaptureCallback.getValue().callMeNow();
      return null;
    }
  }

  private Foo createNotARealMock() {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
      }
    };
    Foo notARealMock = (Foo) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[]{ Foo.class }, handler);
    assertNotNull(notARealMock);
    return notARealMock;
  }

  private static Callback createNoOpCallback() {
    return new Callback() {
      @Override
      public void callMeNow() {
      }
    };
  }
}