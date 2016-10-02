package org.corpus_tools.peppermodules.treetagger;

import static org.assertj.core.api.Assertions.assertThat;

import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.common.ModuleFitness;
import org.corpus_tools.pepper.common.ModuleFitness.FitnessFeature;
import org.corpus_tools.pepper.core.ModuleFitnessChecker;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.corpus_tools.salt.SaltFactory;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

public class TreetaggerImporterTest extends PepperImporterTest {

	@Before
	public void setUp() throws Exception {
		super.setFixture(new TreetaggerImporter());
		super.getFixture().setSaltProject(SaltFactory.createSaltProject());

		// set formats to support
		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName("treetagger");
		formatDef.setFormatVersion("1.0");
		addSupportedFormat(formatDef);
	}

	@Test
	public void whenCorpusIsImportable_thenReturn10() {
		final URI location = URI.createFileURI(PepperTestUtil.getTestResources() + "isImportable");
		assertThat(getFixture().isImportable(location)).isEqualTo(1.0);

	}

	@Test
	public void whenSelfTestingModule_thenResultShouldBeTrue() {
		final ModuleFitness fitness = new ModuleFitnessChecker(PepperTestUtil.createDefaultPepper()).selfTest(fixture);
		assertThat(fitness.getFitness(FitnessFeature.HAS_SELFTEST)).isTrue();
		assertThat(fitness.getFitness(FitnessFeature.HAS_PASSED_SELFTEST)).isTrue();
		assertThat(fitness.getFitness(FitnessFeature.IS_IMPORTABLE_SEFTEST_DATA)).isTrue();
		assertThat(fitness.getFitness(FitnessFeature.IS_VALID_SELFTEST_DATA)).isTrue();
	}
}
