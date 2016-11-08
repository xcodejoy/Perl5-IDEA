/*
 * Copyright 2016 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package completion;

import com.intellij.testFramework.UsefulTestCase;
import com.perl5.lang.perl.fileTypes.PerlFileTypePackage;
import com.perl5.lang.perl.idea.intellilang.PerlLanguageInjector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by hurricup on 04.03.2016.
 */
public class PerlCompletionTest extends PerlCompletionCodeInsightFixtureTestCase
{
	@Override
	protected String getTestDataPath()
	{
		return "testData/completion/perl";
	}

	public void testRefTypes()
	{
		initWithTextSmart("my $var = '<caret>'");
		List<String> strings = myFixture.getLookupElementStrings();
		assertNotNull(strings);

		UsefulTestCase.assertSameElements(strings, mergeLists(REF_TYPES, LIBRARY_PACKAGES));
	}

	public void testHashIndexBare()
	{
		initWithTextSmart("$$a{testindex}; $b->{<caret>}");
		List<String> strings = myFixture.getLookupElementStrings();
		assertNotNull(strings);
		UsefulTestCase.assertSameElements(strings, Arrays.asList("testindex"));
	}


	public void testAnnotation()
	{
		doTest("returns", "inject", "method", "override", "abstract", "deprecated", "noinspection");
	}

	public void testInjectMarkers()
	{
		ArrayList<String> strings = new ArrayList<>(PerlLanguageInjector.LANGUAGE_MAP.keySet());
		assert !strings.isEmpty();
		doTest(strings);
	}


	public void testNextLabels()
	{
		doTest("LABEL1", "LABEL2", "LABEL3");
	}

	public void testGotoLabels()
	{
		doTest("LABEL1", "LABEL2", "LABEL3", "LABEL4", "LABEL5", "LABEL6", "LABEL8");
	}

	@Override
	public String getFileExtension()
	{
		return PerlFileTypePackage.EXTENSION;
	}

	public void testPackageDefinition()
	{
		doTest("packageDefinition");
	}

	public void testPackageUse()
	{
		doTestPackageAndVersions();
	}

	public void testPackageNo()
	{
		doTestPackageAndVersions();
	}

	public void testPackageRequire()
	{
		doTestPackageAndVersions();
	}

	public void testPackageMy()
	{
		doTestAllPackages();
	}

	public void testPackageOur()
	{
		doTestAllPackages();
	}

	public void testPackageState()
	{
		doTestAllPackages();
	}

	public void testTryCatch()
	{
		doTestAllPackages();
	}

	@SafeVarargs
	private final void doTest(List<String>... result)
	{
		assertCompletionIs(result);
	}

	private void doTest(String... result)
	{
		doTest(Arrays.asList(result));
	}

	private void doTestPackageAndVersions()
	{
		doTest(BUILT_IN_PACKAGES, BUILT_IN_VERSIONS, LIBRARY_PACKAGES);
	}

	private void doTestAllPackages()
	{
		doTest(BUILT_IN_PACKAGES, LIBRARY_PACKAGES);
	}

}
