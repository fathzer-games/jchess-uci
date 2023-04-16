package com.fathzer.jchess.uci.option;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class OptionTest {

	@Test
	void testButton() {
		final String name = "Clear Hash";
		final AtomicBoolean called = new AtomicBoolean();
		ButtonOption b = new ButtonOption(name, x->{called.set(true);});
		assertFalse(called.get());
		assertEquals("option name "+name+" type button",b.toUCI());
		assertNull(b.getValue());
		assertThrows(IllegalArgumentException.class, () -> new ButtonOption(null, x->{}));
		assertThrows(IllegalArgumentException.class, () -> new ButtonOption("x", null));
		// Test we can call setValue with null
		b.setValue(null);
		assertTrue(called.get());
	}

	@Test
	void testString() {
		final AtomicReference<String> ref = new AtomicReference<>();
		final String name = "NalimovPath";
		final String defaultValue = "c:\\\\";
		StringOption s = new StringOption(name, x->{ref.set(x);}, defaultValue);
		assertEquals(defaultValue, ref.get());
		assertEquals(defaultValue, s.getValue());
		final String value = "http://x.com";
		s.setValue(value);
		assertEquals(value, ref.get());
		assertEquals(value, s.getValue());
		assertEquals("option name "+name+" type string default "+defaultValue,s.toUCI());
		assertThrows(IllegalArgumentException.class, () -> new StringOption(null, x->{},"x"));
		assertThrows(IllegalArgumentException.class, () -> new StringOption("x", x->{}, null));
		assertThrows(IllegalArgumentException.class, () -> new StringOption("x", null, "y"));
		assertThrows(IllegalArgumentException.class, () -> s.setValue(null));
	}

	@Test
	void testCheck() {
		final AtomicBoolean ref = new AtomicBoolean();
		final String name = "Nullmove";
		final boolean defaultValue = true;
		CheckOption s = new CheckOption(name, x->{ref.set(x);}, defaultValue);
		assertEquals(defaultValue, ref.get());
		assertEquals(defaultValue, s.getValue());
		s.setValue("false");
		assertFalse(ref.get());
		assertFalse(s.getValue());
		assertEquals("option name "+name+" type check default "+defaultValue,s.toUCI());
		assertThrows(IllegalArgumentException.class, () -> new CheckOption(null, x->{}, true));
		assertThrows(IllegalArgumentException.class, () -> new CheckOption("x", null, true));
		assertThrows(IllegalArgumentException.class, () -> s.setValue(null));
		assertThrows(IllegalArgumentException.class, () -> s.setValue("FALSE"));
	}

	@Test
	void testCombo() {
		final AtomicReference<String> ref = new AtomicReference<>();
		final String name = "Style";
		final String defaultValue = "Normal";
		final LinkedHashSet<String> values = new LinkedHashSet<>(Arrays.asList("Solid",defaultValue,"Risky"));
		ComboOption s = new ComboOption(name, x->{ref.set(x);}, defaultValue, values);
		assertEquals(defaultValue, ref.get());
		assertEquals(defaultValue, s.getValue());
		final String value = "Solid";
		s.setValue(value);
		assertEquals(value, ref.get());
		assertEquals(value, s.getValue());
		assertEquals("option name "+name+" type combo default "+defaultValue+" var Solid var Normal var Risky",s.toUCI());
		assertThrows(IllegalArgumentException.class, () -> new ComboOption(null, x->{}, defaultValue, values));
		assertThrows(IllegalArgumentException.class, () -> new ComboOption(name, x->{}, "x", values));
		assertThrows(IllegalArgumentException.class, () -> new ComboOption(name, x->{}, null, values));
		assertThrows(IllegalArgumentException.class, () -> new ComboOption(name, null, defaultValue, values));
		final Set<String> emptySet = Collections.emptySet();
		assertThrows(IllegalArgumentException.class, () -> new ComboOption(name, x->{}, null, emptySet));
		assertThrows(IllegalArgumentException.class, () -> s.setValue(null));
		assertThrows(IllegalArgumentException.class, () -> s.setValue("x"));
	}

	@Test
	void testSpin() {
		final AtomicInteger ref = new AtomicInteger();
		final String name = "Selectivity";
		final int defaultValue = 2;
		SpinOption s = new SpinOption(name, x->{ref.set(x);}, defaultValue, 0, 4);
		assertEquals(defaultValue, ref.get());
		assertEquals(defaultValue, s.getValue());
		final int value = 3;
		s.setValue(Integer.toString(value));
		assertEquals(value, ref.get());
		assertEquals(value, s.getValue());
		assertEquals("option name "+name+" type spin default "+defaultValue+" min 0 max 4",s.toUCI());
		assertThrows(IllegalArgumentException.class, () -> new SpinOption(null, x->{}, defaultValue, 0, 4));
		assertThrows(IllegalArgumentException.class, () -> new SpinOption(name, null, defaultValue, 0, 4));
		assertThrows(IllegalArgumentException.class, () -> new SpinOption(name, x->{}, -1, 0, 4));
		assertThrows(IllegalArgumentException.class, () -> new SpinOption(name, x->{}, 2, 4, 0));
		assertThrows(IllegalArgumentException.class, () -> s.setValue(null));
		assertThrows(IllegalArgumentException.class, () -> s.setValue("8"));
	}
}
