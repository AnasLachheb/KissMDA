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
package de.crowdcode.kissmda.cartridges.simplejava;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.emf.common.util.UniqueEList;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Interface;
import org.junit.Before;
import org.junit.Test;

import de.crowdcode.kissmda.core.uml.PackageHelper;

/**
 * Test Enum Generator.
 * 
 * @author Lofi Dewanto
 * @version 1.0.0
 * @since 1.0.0
 */
public class EnumGeneratorTest {

	private EnumGenerator enumGenerator;
	private PackageHelper packageHelper;

	private Class clazz;

	@Before
	public void setUp() throws Exception {
		packageHelper = new PackageHelper();
		enumGenerator = new EnumGenerator();
		enumGenerator.setPackageHelper(packageHelper);

		setUpMocks();
	}

	public void setUpMocks() throws Exception {
		String fullQualifiedName = "de::crowdcode::kissmda::testapp::components::Company";
		clazz = mock(Class.class);
		when(clazz.getQualifiedName()).thenReturn(fullQualifiedName);
		when(clazz.getName()).thenReturn("Company");
		when(clazz.getAssociations())
				.thenReturn(new UniqueEList<Association>());
		when(clazz.getImplementedInterfaces()).thenReturn(
				new UniqueEList<Interface>());
	}

	@Test
	public void testGeneratePackage() {
		AST ast = AST.newAST(AST.JLS3);
		CompilationUnit cu = ast.newCompilationUnit();

		enumGenerator.generatePackage(clazz, ast, cu);

		assertEquals("package de.crowdcode.kissmda.testapp.components;\n",
				cu.toString());
	}

	@Test
	public void testGenerateEnum() {
		AST ast = AST.newAST(AST.JLS3);
		CompilationUnit cu = ast.newCompilationUnit();

		enumGenerator.generateEnum(clazz, ast, cu);

		assertEquals("public enum Company {}\n", cu.toString());
	}
}
