/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.junit4.orm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.transaction.TransactionTestUtils.assertInTransaction;

import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.orm.domain.DriversLicense;
import org.springframework.test.context.junit4.orm.domain.Person;
import org.springframework.test.context.junit4.orm.service.PersonService;

/**
 * Transactional integration tests regarding <i>automatic vs. manual</i> session
 * flushing with Hibernate.
 * 
 * @author Sam Brannen
 * @since 3.0
 */
@ContextConfiguration
public class HibernateSessionFlushingTests extends AbstractTransactionalJUnit4SpringContextTests {

	private static final String SAM = "Sam";
	private static final String JUERGEN = "Juergen";

	@Autowired
	private PersonService personService;

	@Autowired
	private SessionFactory sessionFactory;


	@Before
	public void setUp() {
		assertInTransaction(true);
		assertNotNull("PersonService should have been autowired.", personService);
		assertNotNull("SessionFactory should have been autowired.", sessionFactory);
	}

	@Test
	public void findSam() {
		Person sam = personService.findByName(SAM);
		assertNotNull("Should be able to find Sam", sam);
		DriversLicense driversLicense = sam.getDriversLicense();
		assertNotNull("Sam's driver's license should not be null", driversLicense);
		assertEquals("Verifying Sam's driver's license number", new Long(1234), driversLicense.getNumber());
	}

	@Test
	public void saveJuergenWithDriversLicense() {
		DriversLicense driversLicense = new DriversLicense(2L, 2222L);
		Person juergen = new Person(JUERGEN, driversLicense);
		personService.save(juergen);
		assertNotNull("Should be able to save and retrieve Juergen", personService.findByName(JUERGEN));
		assertNotNull("Juergen's ID should have been set", juergen.getId());
	}

	@Ignore("Disabled until SPR-5699 is resolved")
	@Test(expected = ConstraintViolationException.class)
	public void saveJuergenWithNullDriversLicense() {
		personService.save(new Person(JUERGEN));
	}

	private void updateSamWithNullDriversLicense() {
		Person sam = personService.findByName(SAM);
		assertNotNull("Should be able to find Sam", sam);
		sam.setDriversLicense(null);
		personService.save(sam);
	}

	@Test(expected = GenericJDBCException.class)
	// @IfProfileValue(name = "spring-compatibility", value = "2.5.6")
	public void updateSamWithNullDriversLicenseSpring256() {
		updateSamWithNullDriversLicense();
		sessionFactory.getCurrentSession().flush();
	}

	@Ignore("Disabled until SPR-5699 is resolved")
	@Test(expected = GenericJDBCException.class)
	// @Test(expected = UncategorizedSQLException.class)
	// @IfProfileValue(name = "spring-compatibility", value = "3.0.0.M2")
	public void updateSamWithNullDriversLicenseSpring300() {
		updateSamWithNullDriversLicense();
	}

}
