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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.TemplateParameter;
import org.eclipse.uml2.uml.TemplateSignature;
import org.eclipse.uml2.uml.Type;

import de.crowdcode.kissmda.core.jdt.JdtHelper;
import de.crowdcode.kissmda.core.jdt.MethodHelper;
import de.crowdcode.kissmda.core.uml.PackageHelper;

/**
 * Generate Interface from UML class.
 * 
 * <p>
 * Most important helper classes from kissmda-core which are used in this
 * Transformer: PackageHelper, MethodHelper, JdtHelper.
 * </p>
 * 
 * @author Lofi Dewanto
 * @version 1.0.0
 * @since 1.0.0
 */
public class InterfaceGenerator {

	private static final Logger logger = Logger
			.getLogger(InterfaceGenerator.class.getName());

	@Inject
	private MethodHelper methodHelper;

	@Inject
	private JdtHelper jdtHelper;

	@Inject
	private PackageHelper packageHelper;

	private String sourceDirectoryPackageName;

	public void setMethodHelper(MethodHelper methodHelper) {
		this.methodHelper = methodHelper;
	}

	public void setJdtHelper(JdtHelper javaHelper) {
		this.jdtHelper = javaHelper;
	}

	public void setPackageHelper(PackageHelper packageHelper) {
		this.packageHelper = packageHelper;
	}

	/**
	 * Generate the Class Interface. This is the main generation part for this
	 * SimpleJavaTransformer.
	 * 
	 * @param Class
	 *            clazz the UML class
	 * @return String the complete class with its content as a String
	 */
	public String generateInterface(Classifier clazz,
			String sourceDirectoryPackageName) {
		this.sourceDirectoryPackageName = sourceDirectoryPackageName;

		AST ast = AST.newAST(AST.JLS3);
		CompilationUnit cu = ast.newCompilationUnit();

		generatePackage(clazz, ast, cu);
		TypeDeclaration td = generateClass(clazz, ast, cu);
		generateMethods(clazz, ast, td);
		generateGettersSetters(clazz, ast, td);

		logger.log(Level.INFO, "Compilation unit: \n\n" + cu.toString());
		return cu.toString();
	}

	/**
	 * Generate the Getters and Setters methods.
	 * 
	 * @param clazz
	 *            the UML class
	 * @param ast
	 *            the JDT Java AST
	 * @param td
	 *            TypeDeclaration Java JDT
	 */
	@SuppressWarnings("unchecked")
	private void generateGettersSetters(Classifier clazz, AST ast,
			TypeDeclaration td) {
		// Create getter and setter for all attributes
		// Without inheritance
		EList<Property> properties = clazz.getAttributes();
		for (Property property : properties) {
			// Create getter for each property
			MethodDeclaration mdGetter = ast.newMethodDeclaration();
			String getterName = methodHelper.getGetterName(property.getName());
			mdGetter.setName(ast.newSimpleName(getterName));
			// Return type?
			Type type = property.getType();
			logger.log(Level.FINE, "Class: " + clazz.getName() + " - "
					+ "Property: " + property.getName() + " - "
					+ "Property Upper: " + property.getUpper() + " - "
					+ "Property Lower: " + property.getLower());
			String umlTypeName = type.getName();
			String umlQualifiedTypeName = type.getQualifiedName();
			if (property.getUpper() >= 0) {
				// Upper Cardinality 0..1
				jdtHelper.createReturnType(ast, td, mdGetter, umlTypeName,
						umlQualifiedTypeName, sourceDirectoryPackageName);
			} else {
				// Upper Cardinality 0..*
				// We need to add Collection<Type> as returnType
				jdtHelper.createReturnTypeAsCollection(ast, td, mdGetter,
						umlTypeName, umlQualifiedTypeName,
						sourceDirectoryPackageName);
			}
			// Getter Javadoc
			generateGetterSetterJavadoc(ast, property, mdGetter);

			// Create setter method for each property
			MethodDeclaration mdSetter = ast.newMethodDeclaration();
			// Return type void
			PrimitiveType primitiveType = jdtHelper.getAstPrimitiveType(ast,
					"void");
			mdSetter.setReturnType2(primitiveType);
			td.bodyDeclarations().add(mdSetter);
			String umlPropertyName = property.getName();

			if (property.getUpper() >= 0) {
				// Upper Cardinality 0..1 params
				String setterName = methodHelper.getSetterName(property
						.getName());
				mdSetter.setName(ast.newSimpleName(setterName));
				jdtHelper.createParameterTypes(ast, td, mdSetter, umlTypeName,
						umlQualifiedTypeName, umlPropertyName,
						sourceDirectoryPackageName);
			} else {
				// Upper Cardinality 0..* params
				// We need to use addXxx instead of setXxx
				String adderName = methodHelper
						.getAdderName(property.getName());
				umlPropertyName = methodHelper.getSingularName(umlPropertyName);
				mdSetter.setName(ast.newSimpleName(adderName));
				jdtHelper.createParameterTypes(ast, td, mdSetter, umlTypeName,
						umlQualifiedTypeName, umlPropertyName,
						sourceDirectoryPackageName);
			}
			// Setter Javadoc
			generateGetterSetterJavadoc(ast, property, mdSetter);
		}
	}

	/**
	 * Generate Javadoc for Getter and Setter method.
	 * 
	 * @param ast
	 *            JDT AST tree
	 * @param property
	 *            UML Property
	 * @param methodDeclaration
	 *            MethodDeclaration for Getter and Setter
	 */
	public void generateGetterSetterJavadoc(AST ast, Property property,
			MethodDeclaration methodDeclaration) {
		EList<Comment> comments = property.getOwnedComments();
		for (Comment comment : comments) {
			Javadoc javadoc = ast.newJavadoc();
			generateJavadoc(ast, comment, javadoc);
			methodDeclaration.setJavadoc(javadoc);
		}
	}

	/**
	 * Generate the Interface.
	 * 
	 * @param clazz
	 *            the UML class
	 * @param ast
	 *            the JDT Java AST
	 * @param cu
	 *            the generated Java compilation unit
	 * @return TypeDeclaration JDT
	 */
	@SuppressWarnings("unchecked")
	public TypeDeclaration generateClass(Classifier clazz, AST ast,
			CompilationUnit cu) {
		String className = getClassName(clazz);
		TypeDeclaration td = ast.newTypeDeclaration();
		td.setInterface(true);
		td.modifiers().add(
				ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		td.setName(ast.newSimpleName(className));

		// Add inheritance
		generateClassInheritance(clazz, ast, td);
		// Add template params
		generateClassTemplateParams(clazz, ast, td);
		// Add Javadoc
		generateClassJavadoc(clazz, ast, td);

		cu.types().add(td);

		return td;
	}

	/**
	 * Generate Javadoc for Interface.
	 * 
	 * @param clazz
	 *            Classifier
	 * @param ast
	 *            JDT AST tree
	 * @param td
	 *            TypeDeclaration
	 */
	public void generateClassJavadoc(Classifier clazz, AST ast,
			TypeDeclaration td) {
		EList<Comment> comments = clazz.getOwnedComments();
		for (Comment comment : comments) {
			Javadoc javadoc = ast.newJavadoc();
			generateJavadoc(ast, comment, javadoc);
			td.setJavadoc(javadoc);
		}
	}

	/**
	 * Generate the Generics for this Interface.
	 * 
	 * @param clazz
	 *            the UML class
	 * @param ast
	 *            the JDT Java AST
	 * @param td
	 *            TypeDeclaration JDT
	 */
	@SuppressWarnings("unchecked")
	public void generateClassTemplateParams(Classifier clazz, AST ast,
			TypeDeclaration td) {
		TemplateSignature templateSignature = clazz.getOwnedTemplateSignature();
		if (templateSignature != null) {
			EList<TemplateParameter> templateParameters = templateSignature
					.getParameters();
			for (TemplateParameter templateParameter : templateParameters) {
				Classifier classifier = (Classifier) templateParameter
						.getOwnedParameteredElement();
				String typeName = classifier.getLabel();
				TypeParameter typeParameter = ast.newTypeParameter();
				typeParameter.setName(ast.newSimpleName(typeName));
				td.typeParameters().add(typeParameter);
			}
		}
	}

	/**
	 * Generate the inheritance for the Interface "extends".
	 * 
	 * @param clazz
	 *            the UML class
	 * @param ast
	 *            the JDT Java AST
	 * @param td
	 *            TypeDeclaration JDT
	 */
	@SuppressWarnings("unchecked")
	private void generateClassInheritance(Classifier clazz, AST ast,
			TypeDeclaration td) {
		EList<Generalization> generalizations = clazz.getGeneralizations();
		if (generalizations != null) {
			for (Generalization generalization : generalizations) {
				Classifier interfaceClassifier = generalization.getGeneral();
				String fullQualifiedInterfaceName = interfaceClassifier
						.getQualifiedName();
				Name name = jdtHelper.createFullQualifiedTypeAsName(ast,
						fullQualifiedInterfaceName, sourceDirectoryPackageName);
				SimpleType simpleType = ast.newSimpleType(name);
				td.superInterfaceTypes().add(simpleType);
			}
		}
	}

	/**
	 * Generate the Java package from UML package.
	 * 
	 * @param clazz
	 *            the UML class
	 * @param ast
	 *            the JDT Java AST
	 * @param cu
	 *            the generated Java compilation unit
	 */
	public void generatePackage(Classifier clazz, AST ast, CompilationUnit cu) {
		PackageDeclaration p1 = ast.newPackageDeclaration();
		String fullPackageName = getFullPackageName(clazz);
		p1.setName(ast.newName(fullPackageName));
		cu.setPackage(p1);
	}

	/**
	 * Generaate the Java methods from UML.
	 * 
	 * @param clazz
	 *            the UML class
	 * @param ast
	 *            the JDT Java AST
	 * @param td
	 *            TypeDeclaration JDT
	 */
	public void generateMethods(Classifier clazz, AST ast, TypeDeclaration td) {
		// Get all methods for this clazz
		// Only for this class without inheritance
		EList<Operation> operations = clazz.getOperations();
		for (Operation operation : operations) {
			MethodDeclaration md = ast.newMethodDeclaration();
			md.setName(ast.newSimpleName(operation.getName()));

			// Parameters, exclude the return parameter
			generateMethodParams(ast, td, operation, md);
			// Return type
			generateMethodReturnType(ast, td, operation, md);
			// Throws Exception
			generateMethodThrowException(ast, operation, md);
			// Generate Javadoc
			generateMethodJavadoc(ast, operation, md);
			// Generate Method template params
			generateMethodTemplateParams(ast, operation, md);
		}
	}

	/**
	 * Generate the template parameter for the given method - Generic Method.
	 * 
	 * @param ast
	 *            AST tree JDT
	 * @param operation
	 *            UML2 Operation
	 * @param md
	 *            MethodDeclaration JDT
	 */
	@SuppressWarnings("unchecked")
	public void generateMethodTemplateParams(AST ast, Operation operation,
			MethodDeclaration md) {
		TemplateSignature templateSignature = operation
				.getOwnedTemplateSignature();
		if (templateSignature != null) {
			EList<TemplateParameter> templateParameters = templateSignature
					.getParameters();
			for (TemplateParameter templateParameter : templateParameters) {
				Classifier classifier = (Classifier) templateParameter
						.getOwnedParameteredElement();
				String typeName = classifier.getLabel();
				TypeParameter typeParameter = ast.newTypeParameter();
				typeParameter.setName(ast.newSimpleName(typeName));
				md.typeParameters().add(typeParameter);
			}
		}
	}

	/**
	 * Generate Javadoc for UML Operation.
	 * 
	 * @param ast
	 *            AST tree JDT
	 * @param operation
	 *            UML Operation - Method
	 * @param md
	 *            MethodDeclaration
	 */
	public void generateMethodJavadoc(AST ast, Operation operation,
			MethodDeclaration md) {
		EList<Comment> comments = operation.getOwnedComments();
		for (Comment comment : comments) {
			Javadoc javadoc = ast.newJavadoc();
			generateJavadoc(ast, comment, javadoc);
			md.setJavadoc(javadoc);
		}
	}

	@SuppressWarnings("unchecked")
	private void generateJavadoc(AST ast, Comment comment, Javadoc javadoc) {
		String[] commentContents = parseComent(comment.getBody());
		for (String commentContent : commentContents) {
			TagElement tagElement = ast.newTagElement();
			tagElement.setTagName(commentContent);
			javadoc.tags().add(tagElement);
		}
	}

	private String[] parseComent(String body) {
		String lines[] = body.split("\\r?\\n");
		return lines;
	}

	@SuppressWarnings("unchecked")
	private void generateMethodThrowException(AST ast, Operation operation,
			MethodDeclaration md) {
		EList<Type> raisedExceptions = operation.getRaisedExceptions();
		for (Type raisedExceptionType : raisedExceptions) {
			String umlExceptionQualifiedTypeName = raisedExceptionType
					.getQualifiedName();
			String name = jdtHelper.createFullQualifiedTypeAsString(ast,
					umlExceptionQualifiedTypeName, sourceDirectoryPackageName);
			Name typeName = ast.newName(name);
			md.thrownExceptions().add(typeName);
		}
	}

	private void generateMethodReturnType(AST ast, TypeDeclaration td,
			Operation operation, MethodDeclaration md) {
		Type type = operation.getType();
		String umlTypeName = type.getName();
		String umlQualifiedTypeName = type.getQualifiedName();
		logger.log(Level.FINE, "UmlQualifiedTypeName: " + umlQualifiedTypeName
				+ " - " + "umlTypeName: " + umlTypeName);
		jdtHelper.createReturnType(ast, td, md, umlTypeName,
				umlQualifiedTypeName, sourceDirectoryPackageName);
	}

	private void generateMethodParams(AST ast, TypeDeclaration td,
			Operation operation, MethodDeclaration md) {
		EList<Parameter> parameters = operation.getOwnedParameters();
		for (Parameter parameter : parameters) {
			if (parameter.getDirection().getValue() != ParameterDirectionKind.RETURN) {
				Type type = parameter.getType();
				String umlTypeName = type.getName();
				String umlQualifiedTypeName = type.getQualifiedName();
				String umlPropertyName = StringUtils.uncapitalize(parameter
						.getName());
				logger.log(Level.FINE, "Parameter: " + parameter.getName()
						+ " - " + "Type: " + umlTypeName);
				jdtHelper.createParameterTypes(ast, td, md, umlTypeName,
						umlQualifiedTypeName, umlPropertyName,
						sourceDirectoryPackageName);
			}
		}
	}

	private String getClassName(Classifier clazz) {
		String className = clazz.getName();
		return className;
	}

	private String getFullPackageName(Classifier clazz) {
		String fullPackageName = packageHelper.getFullPackageName(clazz,
				sourceDirectoryPackageName);
		return fullPackageName;
	}
}
