/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.instrument.classloading.glassfish;

import static org.junit.Assert.*;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.spi.ClassTransformer;

import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.instrument.classloading.LoadTimeWeaver;

import com.sun.enterprise.loader.InstrumentableClassLoader;

// converting away from old-style EasyMock APIs was problematic with this class
@SuppressWarnings("deprecation")
public class GlassFishLoadTimeWeaverTests {

	private MockControl<InstrumentableClassLoader> loaderCtrl;
	private InstrumentableClassLoader loader;
	private LoadTimeWeaver ltw;

	private class DummyInstrumentableClassLoader extends SecureClassLoader implements InstrumentableClassLoader {

		public DummyInstrumentableClassLoader() {
			super();
		}

		public DummyInstrumentableClassLoader(ClassLoader parent) {
			super(parent);
		}

		private List<ClassTransformer> transformers = new ArrayList<ClassTransformer>();

		public void addTransformer(ClassTransformer transformer) {
			transformers.add(transformer);
		}

		public ClassLoader copy() {
			return new DummyInstrumentableClassLoader();
		}
	}

	@Before
	public void setUp() throws Exception {
		loaderCtrl = MockControl.createControl(InstrumentableClassLoader.class);
		loader = loaderCtrl.getMock();
		loaderCtrl.replay();

		ltw = new GlassFishLoadTimeWeaver() {
			@Override
			protected InstrumentableClassLoader determineClassLoader(ClassLoader cl) {
				return loader;
			}
		};
	}

	@After
	public void tearDown() throws Exception {
		loaderCtrl.verify();
		ltw = null;
	}

	@Test
	public void testGlassFishLoadTimeWeaver() {
		try {
			ltw = new GlassFishLoadTimeWeaver();
			fail("expected exception");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

	}

	@Test
	public void testGlassFishLoadTimeWeaverClassLoader() {
		try {
			ltw = new GlassFishLoadTimeWeaver(null);
			fail("expected exception");
		}
		catch (RuntimeException e) {
			// expected
		}

		ClassLoader cl1 = new URLClassLoader(new URL[0]);
		ClassLoader cl2 = new URLClassLoader(new URL[0], cl1);
		ClassLoader cl3 = new DummyInstrumentableClassLoader(cl2);
		ClassLoader cl4 = new URLClassLoader(new URL[0], cl3);

		ltw = new GlassFishLoadTimeWeaver(cl4);
		assertSame(cl3, ltw.getInstrumentableClassLoader());

		cl1 = new URLClassLoader(new URL[0]);
		cl2 = new URLClassLoader(new URL[0], cl1);
		cl3 = new DummyInstrumentableClassLoader(cl2);
		cl4 = new DummyInstrumentableClassLoader(cl3);

		ltw = new GlassFishLoadTimeWeaver(cl4);
		assertSame(cl4, ltw.getInstrumentableClassLoader());
	}

	@Test
	public void testAddTransformer() {
		ClassFileTransformer transformer = MockControl.createNiceControl(
				ClassFileTransformer.class).getMock();
		loaderCtrl.reset();
		loader.addTransformer(new ClassTransformerAdapter(transformer));
		loaderCtrl.setMatcher(new ArgumentsMatcher() {

			public boolean matches(Object[] arg0, Object[] arg1) {
				for (int i = 0; i < arg0.length; i++) {
					if (arg0 != null && arg0.getClass() != arg1.getClass())
						return false;
				}
				return true;
			}

			public String toString(Object[] arg0) {
				return Arrays.toString(arg0);
			}

		});

		loaderCtrl.replay();

		ltw.addTransformer(transformer);
	}

	@Test
	public void testGetThrowawayClassLoader() {
		loaderCtrl.reset();
		ClassLoader cl = new URLClassLoader(new URL[0]);
		loaderCtrl.expectAndReturn(loader.copy(), cl);
		loaderCtrl.replay();

		assertSame(ltw.getThrowawayClassLoader(), cl);
	}

}
