/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * generated by Xtext 2.25.0
 */
package tools.refinery.language;

import org.eclipse.emf.ecore.EPackage;

import com.google.inject.Injector;

import tools.refinery.language.model.problem.ProblemPackage;

/**
 * Initialization support for running Xtext languages without Equinox extension
 * registry.
 */
public class ProblemStandaloneSetup extends ProblemStandaloneSetupGenerated {

	public static void doSetup() {
		new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
	}

	// Here we can't rely on java.util.HashMap#putIfAbsent, because
	// org.eclipse.emf.ecore.impl.EPackageRegistryImpl#containsKey is overridden
	// without also overriding putIfAbsent. We must make sure to call the
	// overridden containsKey implementation.
	@SuppressWarnings("squid:S3824")
	@Override
	public Injector createInjectorAndDoEMFRegistration() {
		if (!EPackage.Registry.INSTANCE.containsKey(ProblemPackage.eNS_URI)) {
			EPackage.Registry.INSTANCE.put(ProblemPackage.eNS_URI, ProblemPackage.eINSTANCE);
		}
		return super.createInjectorAndDoEMFRegistration();
	}
}
