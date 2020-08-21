package edu.uky.kcr.nax.tests;

import com.imsweb.naaccrxml.NaaccrOptions;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;
import com.imsweb.naaccrxml.PatientXmlReader;
import com.imsweb.naaccrxml.entity.Item;
import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;
import com.imsweb.naaccrxml.entity.Tumor;
import com.imsweb.naaccrxml.runtime.NaaccrStreamConfiguration;
import edu.uky.kcr.nax.Nax;
import edu.uky.kcr.nax.NaxConfig;
import edu.uky.kcr.nax.NaxResult;
import edu.uky.kcr.nax.ProgressTrackingDigestInputStream;
import edu.uky.kcr.nax.model.NaaccrDictionary;
import edu.uky.kcr.nax.tests.xmlns.Author;
import edu.uky.kcr.nax.tests.xmlns.Contact;
import edu.uky.kcr.nax.tests.xmlns.Description;
import edu.uky.kcr.nax.tests.xmlns.Email;
import edu.uky.kcr.nax.tests.xmlns.FullName;
import edu.uky.kcr.nax.tests.xmlns.InternalId;
import edu.uky.kcr.nax.tests.xmlns.NaaccrFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class NaxTest
{
	public static final String TEST_FILE_10_USER_NAME = "naaccr-xml-sample-v180-incidence-10-user.xml";
	public static final long TEST_FILE_10_USER_SIZE = 46386;

	public static final String TEST_FILE_10_NS_NAME = "naaccr-xml-sample-v180-abstract-10-extra-ns.xml";
	public static final long TEST_FILE_10_NS_SIZE = 61026;

	public static final String TEST_FILE_1000_GZ_NAME = "naaccr-xml-sample-v180-abstract-1000.xml.gz";
	public static final long TEST_FILE_1000_GZ_SIZE = 183327;
	public static final String TEST_FILE_1000_ZIP_NAME = "naaccr-xml-sample-v180-abstract-1000.xml.zip";
	public static final long TEST_FILE_1000_ZIP_SIZE = 183388;
	public static final long TEST_FILE_1000_GZ_PATIENT_COUNT = 944;
	public static final long TEST_FILE_1000_GZ_TUMOR_COUNT = 1000;
	public static final long TEST_FILE_1000_GZ_ITEM_COUNT = 82905;
	public static final String TEST_FILE_1000_GZ_ITEM_MD5 = "8b8844b8f791b8f6aa1627298feb0a67";

	public static final String RANDOM_REGISTRYID = RandomStringUtils.random(10, false, true);
	public static final String RANDOM_DATE = RandomStringUtils.random(8, false, true);
	public static final String RANDOM_LAST_NAME = RandomStringUtils.random(25, true, false);

	public static final String SHARED_INCLUDE_EXCLUDE_ITEM = "dateOfDiagnosis";

	public static final Map<String, String> TEST_CONSTANTS_MAP = Collections
			.unmodifiableMap(new HashMap<String, String>()
			{
				{
					put("registryId", RANDOM_REGISTRYID);
					put("nameLast", RANDOM_LAST_NAME);
					put("dateConclusiveDx", RANDOM_DATE);
				}
			});

	public static final List<String> EXCLUDED_ITEMS = Collections.unmodifiableList(new ArrayList<String>()
	{
		{
			add("recordType");
			add("20"); //patientIdNumber
			add(SHARED_INCLUDE_EXCLUDE_ITEM);
			add("491"); //censusTrPovertyIndictr
		}
	});

	public static final List<String> NS_STRINGS_INCLUDED = Collections.unmodifiableList(new ArrayList<String>()
	{
		{
			add("<test:FULL_NAME>Gabriel Simpson</test:FULL_NAME>");
			add("<test:EMAIL>00000010@server.ru</test:EMAIL>");
			add("<test:PATH_REPORT id=\"pathreport1\"><![CDATA[");
		}
	});

	public static final List<String> INCLUDED_ITEMS = Collections.unmodifiableList(new ArrayList<String>()
	{
		{
			add("40");
			add("nameMiddle");
			add(SHARED_INCLUDE_EXCLUDE_ITEM);
			add("410"); //laterality
		}
	});

	public static final List<String[]> USER_DICTIONARY_NAMES_AND_SIZES = Collections
			.unmodifiableList(new ArrayList<String[]>()
			{
				{
					add(new String[]{"naaccr-prep-dictionary-180.xml", "21355"});
					add(new String[]{"test-user-dictionary-180.xml", "485"});
				}
			});

	@DataProvider(name = "nonnaxmlfiles")
	public Object[][] createNonNaxmlData()
	{
		return new Object[][]{
				{"NonNaaccrXmlLarge.xml.zip", 104283L},
				{"NonNaaccrXmlSmall.xml", 469L},
				{"NonNaaccrXmlSmall.zip", 483L}
		};
	}

	private static NaaccrStreamConfiguration createNamespaceConfiguration()
	{
		NaaccrStreamConfiguration configuration = NaaccrStreamConfiguration.getDefault();
		configuration.registerNamespace("test", "https://www.naaccr.org/xmltest");
		configuration.registerTag("test", "DELETION_LIST", edu.uky.kcr.nax.tests.xmlns.DeletionList.class);
		configuration.registerTag("test", "INTERNAL_ID", edu.uky.kcr.nax.tests.xmlns.InternalId.class);
		configuration.registerTag("test", "NAACCR_FILE", edu.uky.kcr.nax.tests.xmlns.NaaccrFile.class);
		configuration.registerTag("test", "AUTHOR", edu.uky.kcr.nax.tests.xmlns.Author.class);
		configuration.registerTag("test", "FULL_NAME", edu.uky.kcr.nax.tests.xmlns.FullName.class);
		configuration.registerTag("test", "DESCRIPTION", edu.uky.kcr.nax.tests.xmlns.Description.class);
		configuration.registerTag("test", "NAACCR_FILE", NaaccrData.class, "NAACCR_FILE", NaaccrFile.class);
		configuration
				.registerTag("test", "AUTHOR", edu.uky.kcr.nax.tests.xmlns.NaaccrFile.class, "AUTHOR", Author.class);
		configuration
				.registerTag("test", "DESCRIPTION", edu.uky.kcr.nax.tests.xmlns.NaaccrFile.class, "DESCRIPTION", Description.class);
		configuration
				.registerTag("test", "FULL_NAME", edu.uky.kcr.nax.tests.xmlns.Author.class, "FULL_NAME", FullName.class);
		configuration.registerTag("test", "PATIENT", edu.uky.kcr.nax.tests.xmlns.Patient.class);
		configuration
				.registerTag("test", "CONTACT", edu.uky.kcr.nax.tests.xmlns.Patient.class, "CONTACT", Contact.class);
		configuration.registerTag("test", "EMAIL", edu.uky.kcr.nax.tests.xmlns.Patient.class, "EMAIL", Email.class);
		configuration.registerTag("test", "PATH_REPORT", String.class);
		configuration.registerTag("test", "TUMOR", edu.uky.kcr.nax.tests.xmlns.Tumor.class);
		configuration.registerImplicitCollection(edu.uky.kcr.nax.tests.xmlns.Tumor.class, "PATH_REPORT", String.class);
		configuration
				.registerImplicitCollection(edu.uky.kcr.nax.tests.xmlns.DeletionList.class, "INTERNAL_ID", InternalId.class);

		return configuration;
	}


	private InputStream getTestResourceInputStream(String name)
	{
		return this.getClass().getResourceAsStream("/" + name);
	}

	@DataProvider(name = "alltestfiles")
	public Object[][] createData()
	{
		return new Object[][]{
				{TEST_FILE_10_USER_NAME, TEST_FILE_10_USER_SIZE, NaaccrStreamConfiguration.getDefault(), USER_DICTIONARY_NAMES_AND_SIZES},
				{TEST_FILE_1000_GZ_NAME, TEST_FILE_1000_GZ_SIZE, NaaccrStreamConfiguration.getDefault(), Collections.EMPTY_LIST},
				{TEST_FILE_1000_ZIP_NAME, TEST_FILE_1000_ZIP_SIZE, NaaccrStreamConfiguration.getDefault(), Collections.EMPTY_LIST},
				{TEST_FILE_10_NS_NAME, TEST_FILE_10_NS_SIZE, createNamespaceConfiguration(), Collections.EMPTY_LIST}
		};
	}

	@Test
	public void testBasicCounts()
			throws Exception
	{
		NaxConfig naxConfig = new NaxConfig();
		Nax nax = Nax.newInstance(naxConfig);

		try (InputStream inputStream = getTestResourceInputStream(TEST_FILE_1000_GZ_NAME))
		{
			List<NaxResult> naxResults = nax.process(inputStream, TEST_FILE_1000_GZ_NAME, TEST_FILE_1000_GZ_SIZE);

			Assert.assertEquals(naxResults.size(), 1);

			NaxResult naxResult = naxResults.get(0);

			Assert.assertEquals(TEST_FILE_1000_GZ_PATIENT_COUNT, naxResult.getNaxMetrics().getElementCounts()
					.get("Patient").longValue(), "Check Patient Count");
			Assert.assertEquals(TEST_FILE_1000_GZ_TUMOR_COUNT, naxResult.getNaxMetrics().getElementCounts().get("Tumor")
					.longValue(), "Check Tumor Count");
			Assert.assertEquals(TEST_FILE_1000_GZ_ITEM_COUNT, naxResult.getNaxMetrics().getElementCounts().get("Item")
					.longValue(), "Check Item Count");
			Assert.assertEquals(1L, naxResult.getNaxMetrics().getElementCounts().get("NaaccrData")
					.longValue(), "Check NaaccrData Count");
			Assert.assertEquals(naxResult.getInputFileInfo()
										.getMd5(), TEST_FILE_1000_GZ_ITEM_MD5, "Check Input File MD5");
		}
	}

	@Test
	public void testIncludeNamespace()
			throws Exception
	{
		NaxConfig naxConfig = new NaxConfig();
		naxConfig.withIncludeNamespaces(true);

		Nax nax = Nax.newInstance(naxConfig);

		File tempFile = File.createTempFile("tempNaxTest", ".xml");

		try (InputStream inputStream = getTestResourceInputStream(TEST_FILE_10_NS_NAME))
		{
			List<NaxResult> naxResults = nax.process(inputStream, TEST_FILE_10_NS_NAME, TEST_FILE_10_NS_SIZE, tempFile);

			Assert.assertEquals(naxResults.size(), 1);

			String allFileText = FileUtils.readFileToString(tempFile);
			allFileText = StringUtils.substringAfter(allFileText, "?>").trim();

			try (InputStream inputStream2 = this.getClass().getResourceAsStream("/" + TEST_FILE_10_NS_NAME))
			{
				String fileString = IOUtils.toString(inputStream2);
				fileString = StringUtils.substringAfter(fileString, "?>").trim();

				fileString = StringUtils.replace(fileString, "\r\n", "\n");

				Assert.assertEquals(StringUtils.right(allFileText, 100), StringUtils.right(fileString, 100));
			}
		}
		finally
		{
			tempFile.delete();
		}
	}

	@Test(dataProvider = "nonnaxmlfiles")
	public void testNonNaaccrXmlFile(String filename,
									 long size)
			throws IOException
	{
		Nax nax = Nax.newInstance(new NaxConfig());

		try (InputStream inputStream = getTestResourceInputStream(filename))
		{
			List<NaxResult> naxResults = nax.process(inputStream, filename, size);

			Assert.assertEquals(naxResults.size(), 1);

			NaxResult naxResult = naxResults.get(0);

			Assert.assertFalse(naxResult.isParsingSuccess());
		}
	}

	@Test(dataProvider = "alltestfiles")
	public void testProcessFileWithoutChanges(String filename,
											  long filesize,
											  NaaccrStreamConfiguration configuration,
											  List<String[]> userDictionaryFiles)
			throws Exception
	{
		NaxConfig naxConfig = new NaxConfig();
		naxConfig.withIncludeNamespaces(true);

		initializeUserDictionariesFromFiles(userDictionaryFiles, null, naxConfig);

		Nax nax = Nax.newInstance(naxConfig);

		File tempFile = File.createTempFile("tempNaxTest", "." + FilenameUtils.getExtension(filename));

		try (InputStream inputStream = getTestResourceInputStream(filename))
		{
			List<NaxResult> naxResults = nax.process(inputStream, filename, filesize, tempFile);

			Assert.assertEquals(naxResults.size(), 1);

			String allFileText = null;

			try(InputStream tempFileInputStream = createInputStream(tempFile))
			{
				allFileText = IOUtils.toString(tempFileInputStream);
			}

			//We dont care about differences in the XML PI
			allFileText = StringUtils.substringAfter(allFileText, "?>").trim();

			try (InputStream inputStream2 = getTestResourceInputStream(filename))
			{
				String fileString = StringUtils.EMPTY;

				if (filename.endsWith(".gz"))
				{
					try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream2, Nax.GZIP_BUFFER))
					{
						fileString = IOUtils.toString(gzipInputStream);
					}
				}
				else if (filename.endsWith(".zip"))
				{
					try (ZipInputStream zipInputStream = new ZipInputStream(inputStream2))
					{
						ZipEntry zipEntry = zipInputStream.getNextEntry();
						fileString = IOUtils.toString(zipInputStream);
					}
				}
				else
				{
					fileString = IOUtils.toString(inputStream2);
				}

				//We dont care about differences in the XML PI
				fileString = StringUtils.substringAfter(fileString, "?>").trim();

				//XML parsers will normalize all inter-element line breaks to line feeds (LF) according to the XML standard: https://www.w3.org/TR/xml/#sec-line-ends
				fileString = StringUtils.replace(fileString, "\r\n", "\n");

				Assert.assertEquals(allFileText, fileString, "File contents did not match");
			}
		}
		finally
		{
			tempFile.delete();
		}
	}


	@Test
	public void testExcludeNamespace()
			throws Exception
	{
		NaxConfig naxConfig = new NaxConfig();
		naxConfig.withIncludeNamespaces(false);

		Nax nax = Nax.newInstance(naxConfig);

		File tempFile = File.createTempFile("tempNaxTest", ".xml");

		try (InputStream inputStream = getTestResourceInputStream(TEST_FILE_10_NS_NAME))
		{
			List<NaxResult> naxResults = nax.process(inputStream, TEST_FILE_10_NS_NAME, TEST_FILE_10_NS_SIZE, tempFile);

			Assert.assertEquals(naxResults.size(), 1);

			String allFileText = FileUtils.readFileToString(tempFile);

			for (String ns_text : NS_STRINGS_INCLUDED)
			{
				Assert.assertFalse(allFileText
										   .contains(ns_text), "Ensure output does not contain namespaced content: " + ns_text);
			}

			Assert.assertFalse(allFileText.contains("<test:"), "Ensure output does not contain any namespaced content");
		}
		finally
		{
			tempFile.delete();
		}
	}

	@Test(dataProvider = "alltestfiles")
	public void testOutputFileMetrics(String filename,
									  long filesize,
									  NaaccrStreamConfiguration configuration,
									  List<String[]> userDictionaryFiles)
			throws Exception
	{
		NaxConfig naxConfig = new NaxConfig();
		naxConfig.withExcludedItems(Arrays.asList(new String[]{"patientIdNumber", "primarySite"}));

		initializeUserDictionariesFromFiles(userDictionaryFiles, null, naxConfig);

		File tempFile = File.createTempFile("tempNaxTest", "." + FilenameUtils.getExtension(filename));

		try (InputStream firstInputStream = getTestResourceInputStream(filename))
		{
			List<NaxResult> naxResults = Nax.newInstance(naxConfig)
					.process(firstInputStream, filename, filesize, tempFile);

			Assert.assertEquals(naxResults.size(), 1);

			NaxResult naxResult = naxResults.get(0);

			try (InputStream secondInputStream = new FileInputStream(tempFile))
			{
				List<NaxResult> naxResultsAfterOutput = Nax.newInstance(naxConfig).process(secondInputStream, tempFile
						.getName(), tempFile.length());

				Assert.assertEquals(naxResultsAfterOutput.size(), 1);

				NaxResult naxResultAfterOutput = naxResultsAfterOutput.get(0);

				Assert.assertEquals(naxResult.getNaxMetrics().getElementCounts(), naxResultAfterOutput.getNaxMetrics()
						.getElementCounts());
				Assert.assertEquals(naxResult.getNaxMetrics().getOtherElementCounts(), naxResultAfterOutput
						.getNaxMetrics().getOtherElementCounts());
				Assert.assertNotEquals(naxResult.getNaxMetrics().getExcludedElementCounts()
											   .get("Item"), naxResultAfterOutput.getNaxMetrics()
											   .getExcludedElementCounts().get("Item"));
				Assert.assertEquals(naxResult.getNaxMetrics().getExcludedElementCounts()
											.get("Tumor"), naxResultAfterOutput.getNaxMetrics()
											.getExcludedElementCounts().get("Tumor"));
				Assert.assertNotEquals(naxResult.getNaxMetrics().getExcludedNaaccrIdCounts(), naxResultAfterOutput
						.getNaxMetrics().getExcludedNaaccrIdCounts());
				Assert.assertNull(naxResult.getNaxMetrics().getNaaccrIdCounts().get("patientIdNumber"));
				Assert.assertNull(naxResultAfterOutput.getNaxMetrics().getNaaccrIdCounts().get("patientIdNumber"));
				Assert.assertNull(naxResult.getNaxMetrics().getNaaccrIdCounts().get("primarySite"));
				Assert.assertNull(naxResultAfterOutput.getNaxMetrics().getNaaccrIdCounts().get("primarySite"));
				Assert.assertEquals(naxResult.getNaxMetrics().getNaaccrIdCounts()
											.get("dateOfDiagnosis"), naxResultAfterOutput.getNaxMetrics()
											.getNaaccrIdCounts().get("dateOfDiagnosis"));
			}
		}
		finally
		{
			tempFile.delete();
		}
	}

	private void initializeUserDictionariesFromFiles(List<String[]> userDictionaryFiles,
													 List<com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary> userDictionaries,
													 NaxConfig naxConfig)
			throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException
	{
		if (userDictionaryFiles != null)
		{
			for (String[] userDictionaryFileinfo : userDictionaryFiles)
			{
				if (userDictionaries != null)
				{
					try (InputStream userDictionaryInputStream = this.getClass()
							.getResourceAsStream("/" + userDictionaryFileinfo[0]))
					{
						userDictionaries.add(NaaccrXmlDictionaryUtils
													 .readDictionary(new InputStreamReader(userDictionaryInputStream)));
					}
				}

				try (ProgressTrackingDigestInputStream userDictionaryInputStream = new ProgressTrackingDigestInputStream(
						getTestResourceInputStream(userDictionaryFileinfo[0]),
						userDictionaryFileinfo[0],
						Long.parseLong(userDictionaryFileinfo[1])))
				{
					naxConfig.withUserDictionary(userDictionaryInputStream);
				}
			}
		}
	}


	@Test(dataProvider = "alltestfiles")
	public void testExcludeItems(String filename,
								 long filesize,
								 NaaccrStreamConfiguration configuration,
								 List<String[]> userDictionaryFiles)
			throws Exception
	{
		File tempFile = File.createTempFile("tempNaxTest", "." + FilenameUtils.getExtension(filename));

		try (InputStream inputStream = getTestResourceInputStream(filename))
		{
			NaxConfig naxConfig = new NaxConfig();

			naxConfig.withExcludedItems(EXCLUDED_ITEMS);

			List<com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary> userDictionaries = new ArrayList<>();

			initializeUserDictionariesFromFiles(userDictionaryFiles, userDictionaries, naxConfig);

			Nax nax = Nax.newInstance(naxConfig);

			List<NaxResult> naxResults = nax.process(inputStream, filename, filesize, tempFile);

			Assert.assertEquals(naxResults.size(), 1);
			NaxResult naxResult = naxResults.get(0);

			NaaccrDictionary naaccrDictionary = NaaccrDictionary.createBaseDictionary(naxResult.getNaaccrVersion());

			NaaccrDictionary defaultUserDictionary = NaaccrDictionary.createDefaultUserDictionary(naxResult
																										  .getNaaccrVersion());

			try (InputStream tempFileInputStream = createInputStream(tempFile);
				 PatientXmlReader reader = new PatientXmlReader(
						 new InputStreamReader(tempFileInputStream),
						 NaaccrOptions.getDefault(),
						 userDictionaries,
						 configuration))
			{
				checkExcludedItems(reader.getRootData().getItems(), EXCLUDED_ITEMS, naaccrDictionary, naxConfig
						.getUserDictionaries(), defaultUserDictionary);

				Patient patient = reader.readPatient();

				while (patient != null)
				{
					checkExcludedItems(patient.getItems(), EXCLUDED_ITEMS, naaccrDictionary, naxConfig
							.getUserDictionaries(), defaultUserDictionary);

					List<Tumor> tumors = patient.getTumors();

					for (Tumor tumor : tumors)
					{
						checkExcludedItems(tumor.getItems(), EXCLUDED_ITEMS, naaccrDictionary, naxConfig
								.getUserDictionaries(), defaultUserDictionary);
					}

					patient = reader.readPatient();
				}
			}
		}
		finally
		{
			tempFile.delete();
		}
	}


	@Test(dataProvider = "alltestfiles")
	public void testIncludeItems(String filename,
								 long filesize,
								 NaaccrStreamConfiguration configuration,
								 List<String[]> userDictionaryFiles)
			throws Exception
	{
		File tempFile = File.createTempFile("tempNaxTest", "." + FilenameUtils.getExtension(filename));

		try (InputStream inputStream = getTestResourceInputStream(filename))
		{
			NaxConfig naxConfig = new NaxConfig();

			naxConfig.withIncludedItems(INCLUDED_ITEMS);

			List<com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary> userDictionaries = new ArrayList<>();

			initializeUserDictionariesFromFiles(userDictionaryFiles, userDictionaries, naxConfig);

			Nax nax = Nax.newInstance(naxConfig);

			List<NaxResult> naxResults = nax.process(inputStream, filename, filesize, tempFile);

			Assert.assertEquals(naxResults.size(), 1);
			NaxResult naxResult = naxResults.get(0);

			NaaccrDictionary naaccrDictionary = NaaccrDictionary.createBaseDictionary(naxResult.getNaaccrVersion());
			NaaccrDictionary defaultUserDictionary = NaaccrDictionary.createDefaultUserDictionary(naxResult
																										  .getNaaccrVersion());

			try (InputStream tempFileInputStream = createInputStream(tempFile);
				 PatientXmlReader reader = new PatientXmlReader(
						 new InputStreamReader(tempFileInputStream),
						 NaaccrOptions.getDefault(),
						 userDictionaries,
						 configuration))
			{
				checkIncludedItems(reader.getRootData().getItems(), INCLUDED_ITEMS, naaccrDictionary, naxConfig
						.getUserDictionaries(), defaultUserDictionary);

				Patient patient = reader.readPatient();

				while (patient != null)
				{
					checkIncludedItems(patient.getItems(), INCLUDED_ITEMS, naaccrDictionary, naxConfig
							.getUserDictionaries(), defaultUserDictionary);

					List<Tumor> tumors = patient.getTumors();

					for (Tumor tumor : tumors)
					{
						checkIncludedItems(tumor.getItems(), INCLUDED_ITEMS, naaccrDictionary, naxConfig
								.getUserDictionaries(), defaultUserDictionary);
					}

					patient = reader.readPatient();
				}
			}
		}
		finally
		{
			tempFile.delete();
		}
	}

	private InputStream createInputStream(File tempFile)
			throws IOException
	{
		InputStream tempFileInputStream = null;

		if (tempFile.getName().endsWith(".gz"))
		{
			tempFileInputStream = new GZIPInputStream(new FileInputStream(tempFile));
		}
		else if (tempFile.getName().endsWith(".zip"))
		{
			ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(tempFile));
			zipInputStream.getNextEntry();

			tempFileInputStream = zipInputStream;
		}
		else
		{
			tempFileInputStream = new FileInputStream(tempFile);
		}

		return tempFileInputStream;
	}

	@Test(dataProvider = "alltestfiles")
	public void testConstantValue(String filename,
								  long filesize,
								  NaaccrStreamConfiguration configuration,
								  List<String[]> userDictionaryFiles)
			throws Exception
	{
		File tempFile = File.createTempFile("tempNaxTest", "." + FilenameUtils.getExtension(filename));

		try (InputStream inputStream = getTestResourceInputStream(filename))
		{
			NaxConfig naxConfig = new NaxConfig();

			for (String naaccrId : TEST_CONSTANTS_MAP.keySet())
			{
				String value = TEST_CONSTANTS_MAP.get(naaccrId);
				naxConfig.withConstantValue(naaccrId, value);
			}

			List<com.imsweb.naaccrxml.entity.dictionary.NaaccrDictionary> userDictionaries = new ArrayList<>();

			initializeUserDictionariesFromFiles(userDictionaryFiles, userDictionaries, naxConfig);

			Nax nax = Nax.newInstance(naxConfig);

			nax.process(inputStream, filename, filesize, tempFile);

			try (InputStream tempFileInputStream = createInputStream(tempFile);
					PatientXmlReader reader = new PatientXmlReader(
					new InputStreamReader(tempFileInputStream),
					NaaccrOptions.getDefault(),
					userDictionaries,
					configuration))
			{
				checkConstantItems(reader.getRootData().getItems(), TEST_CONSTANTS_MAP);

				Patient patient = reader.readPatient();

				while (patient != null)
				{
					checkConstantItems(patient.getItems(), TEST_CONSTANTS_MAP);

					List<Tumor> tumors = patient.getTumors();

					for (Tumor tumor : tumors)
					{
						checkConstantItems(tumor.getItems(), TEST_CONSTANTS_MAP);
					}

					patient = reader.readPatient();
				}
			}
		}
		finally
		{
			tempFile.delete();
		}
	}

	private void checkExcludedItems(List<Item> items,
									List<String> excludedItems,
									NaaccrDictionary naaccrDictionary,
									List<NaaccrDictionary> userDictionaries,
									NaaccrDictionary defaultUserDictionary)
	{
		for (Item item : items)
		{
			String naaccrId = item.getNaaccrId();
			Integer naaccrNum = NaaccrDictionary
					.lookupNaaccrNum(naaccrId, naaccrDictionary, userDictionaries, defaultUserDictionary);

			Assert.assertFalse(excludedItems
									   .contains(naaccrId), "Ensure excluded naaccrId is not in output: " + naaccrId);
			Assert.assertFalse(excludedItems.contains(naaccrNum
															  .toString()), "Ensure excluded naaccrNum is not in output: " + naaccrNum);
		}
	}

	private void checkIncludedItems(List<Item> items,
									List<String> includedItems,
									NaaccrDictionary naaccrDictionary,
									List<NaaccrDictionary> userDictionaries,
									NaaccrDictionary defaultUserDictionary)
	{
		for (Item item : items)
		{
			String naaccrId = item.getNaaccrId();
			Integer naaccrNum = NaaccrDictionary
					.lookupNaaccrNum(naaccrId, naaccrDictionary, userDictionaries, defaultUserDictionary);

			Assert.assertTrue(includedItems.contains(naaccrId) || includedItems.contains(naaccrNum.toString()),
							  "Ensure included naaccrId or naaccrNum is in output: " + naaccrId);
		}
	}

	private void checkConstantItems(List<Item> items,
									Map<String, String> constantValues)
	{
		for (Item item : items)
		{
			if (constantValues.containsKey(item.getNaaccrId()))
			{
				Assert.assertEquals(item.getValue(), constantValues
						.get(item.getNaaccrId()), "Ensure constant equals " + item.getNaaccrId());
			}
			else
			{
				Assert.assertNotEquals(item.getValue(), constantValues
						.get(item.getNaaccrId()), "Ensure constant does not equal " + item.getNaaccrId());
			}
		}
	}
}
