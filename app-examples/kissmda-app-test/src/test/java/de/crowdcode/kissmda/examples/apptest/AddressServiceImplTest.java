/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.crowdcode.kissmda.examples.apptest;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import de.crowdcode.kissmda.examples.apptest.components.CompanyImpl;
import de.crowdcode.kissmda.testapp.Address;
import de.crowdcode.kissmda.testapp.AddressService;
import de.crowdcode.kissmda.testapp.AddressType;
import de.crowdcode.kissmda.testapp.CompanyAttribute;
import de.crowdcode.kissmda.testapp.Person;
import de.crowdcode.kissmda.testapp.components.Company;

/**
 * Unit test for AddressServiceImpl class.
 * 
 * @author Lofi Dewanto
 * @version 1.1.0
 */
public class AddressServiceImplTest {

	private Person person;

	private AddressService addressService;

	@Before
	public void setUp() throws Exception {
		person = new PersonImpl();
		person.setName("Lofi");

		addressService = new AddressServiceImpl();
	}

	@Test
	public void testCreateAddressFromPerson() {
		Company company = new CompanyImpl();
		company.setName("CrowdCode");

		CompanyAttribute<String, Integer> companyAttribute = new CompanyAttributeImpl();
		companyAttribute.setName("Lofi");
		companyAttribute.add("Test Element", 23);
		company.setCompanyAttribute(companyAttribute);

		Address address = new AddressImpl();
		address.setStreet("Jakarta");
		address.setAddressType(AddressType.HOME);

		assertEquals(null, address.getPerson());

		addressService.createAddressFromPerson(address, person);

		assertEquals(person, address.getPerson());
	}
}
