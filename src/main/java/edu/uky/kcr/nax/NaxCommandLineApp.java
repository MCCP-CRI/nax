/*
 * Copyright 2020 University of Kentucky
 * Kentucky Cancer Registry
 * University of Kentucky Markey Cancer Control Program
 * Markey Cancer Research Informatics Shared Resource Facility
 *
 * Permission is hereby granted, free of charge, to use a copy of this software
 * and associated documentation files (the “Software”) for any non-profit or
 * educational use, including without limitation the right to use, copy, modify,
 * merge, publish, and distribute copies of the Software, and to permit persons
 * to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * For any for-profit or other commercial use, potential users should contact:
 * Kentucky Cancer Registry
 * ATTN: Associate Director of Informatics
 * 2365 Harrodsburg Road, Suite A230
 * Lexington, KY 40504-3381
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package edu.uky.kcr.nax;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point, argument parsing, and help text output for the command-line Nax processing application.
 */
public class NaxCommandLineApp
{
	private static final Logger logger = Logger.getLogger(Nax.class.getName());

	private static final String DEFAULT_LOG_LEVEL = Level.INFO.getName();

	public static final Option LOGGING_OPTION = new Option("l", "loglevel", true, String
			.format(
					"Log level (default is %s): %s",
					DEFAULT_LOG_LEVEL,
					StringUtils.joinWith(
							", ",
							Level.ALL.getName(),
							Level.OFF.getName(),
							Level.CONFIG.getName(),
							Level.FINE.getName(),
							Level.FINER.getName(),
							Level.FINEST.getName(),
							Level.INFO.getName(),
							Level.SEVERE.getName(),
							Level.WARNING.getName())));

	public static void main(String[] args)
			throws Exception
	{
		Options options = new Options();
		options.addOption(LOGGING_OPTION);
		options.addOption("v", "version", false, "Show version.");
		options.addOption("h", "help", false, "Show help.");
		options.addOption(
				"fp", "filterPatient", true,
				"File or inline groovy script called on every Patient element that evaluates to false to exclude the patient from output.");
		options.addOption(
				"ft", "filterTumor", true,
				"File or inline groovy script called on every Tumor element that evaluates to false to exclude the tumor from output.");
		options.addOption(
				"fi", "filterItem", true,
				"File or inline groovy script called on Items specified by a naaccrId using the format: <naaccrId>=<inline script or file> If the script evaluates to false, the item will be excluded from output.");
		options.addOption("s", "script", true, "File or inline groovy script to filter out elements or make changes to any Patients/Tumors/Item. " +
				"This script will be called for all Patient, Tumor, Item, and other namespace elements, if the script returns false then the element is excluded from output. " +
				"If the script returns true or does not have a return value, the element will be included in output. This parameter can be specified more than once.");
		options.addOption("o", "outputfile", true, "Output file or directory");
		options.addOption(
				"i", "includeItems", true,
				"Comma-separated list of naaccrIds to include (ex. nameFirst,socialSecurityNumber). If this argument is specified, all other Items will be removed from output. Takes precedence over excluded items parameter.");
		options.addOption(
				"ns", "includeNamespaces", true, "Boolean value to include non-NAACCR namespaces, defaults to true unless includedItems is non-blank");
		options.addOption(
				"e", "excludeItems", true,
				"Comma-separated list of naaccrIds to exclude (ex. nameFirst,socialSecurityNumber). Included items take preference over this parameter.");
		options.addOption(
				"ts", "timestamp", true, "When the output file is a directory, boolean value to append a timestamp to output filenames, 'true' by default");
		options.addOption("pre", "fileprefix", true, "When the output file is a directory, prefix to append to output file names, empty by default");
		options.addOption("rpl", "replace", true, "CSV file with replacement values for Items, must have a header with: naaccrId, itemValue, newItemValue");
		options.addOption(
				"con", "constant", true,
				"Constant value to set for an Item, using the format: <naaccrId>=<value>. Any values from a CSV replacement file (rpl) take precedence over constant values. This parameter can be specified more than once.");
		options.addOption("met", "metrics", true, "Level of metrics logging to output, defaults to 1 (0=none, 1=basic, 2=extended)");
		options.addOption("usr", "userDictionary", true, "User Dictionary File to include when parsing XML. This parameter can be specified more than once.");
		options.addOption(
				"del", "deleteOutputFiles", true,
				"Criteria for deleting output files when they don't have enough data after all of the processing from nax, defaults to 0 (0=never delete, 1=delete if no patients, 2=delete if no tumors)");
		options.addOption(
				"rep", "removeEmptyPatients", true,
				"When a Patient has no Tumor records, boolean value to remove the empty Patient record, defaults to 'false'");
		options.addOption(
				"vc", "valueCounts", true,
				"A comma-separated list of naaccrIds to get a count of all values in a file. For continuous values that require data binning before counts, specify a single naaccrId and a Groovy script to 'bin' the data: <naaccrId>=<Groovy script>. (For example, to get counts of all diagnosis years: -vc dateOfDiagnosis=\"left(dateOfDiagnosis, 4)\". This parameter can be specified more than once.");

		options.addOption("ems", "emailSubject", true, "Email Subject line");
		options.addOption("emh", "emailSmtpHost", true, "Email SMTP hostname");
		options.addOption("emf", "emailFrom", true, "Email address of sender");
		options.addOption("emt", "emailTo", true, "Email address of recipient (can specify multiple times)");
		options.addOption("emu", "emailUsername", true, "Email address of sender");
		options.addOption("emu", "emailPassword", true, "Email address of sender");

		CommandLineParser parser = new DefaultParser();

		try
		{
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("v"))
			{
				printVersion();
			}
			else if (line.hasOption("h"))
			{
				printHelp(options);
			}
			else if (line.getArgList().size() != 1)
			{
				throw new ParseException("A single input file or directory must be specified.");
			}
			else if (StringUtils.isAllEmpty(
					line.getOptionValue("emailSmtpHost"),
					line.getOptionValue("emailFrom"),
					line.getOptionValue("emailTo")) == false &&
					StringUtils.isAnyEmpty(
							line.getOptionValue("emailSmtpHost"),
							line.getOptionValue("emailFrom"),
							line.getOptionValue("emailTo")))
			{
				throw new ParseException(
						"If any email settings are specified, the following minimum email settings must also be specified: emailSmtpHost, emailFrom, emailTo");
			}
			else
			{
				Level level = Level.parse(line.getOptionValue(LOGGING_OPTION.getOpt(), DEFAULT_LOG_LEVEL));
				ConsoleLogging.initializeConsoleLogging(level);

				logger.info("Logging intialized to level: " + level.getName());

				String inputFileString = line.getArgs()[0];
				File inputFile = new File(inputFileString);

				logger.info(String.format("Input file is: %s", inputFileString));

				NaxConfig naxConfig = new NaxConfig();

				String[] userDictionaryFilePaths = line.getOptionValues("usr");

				if ((userDictionaryFilePaths != null) && (userDictionaryFilePaths.length > 0))
				{
					for (int i = 0; i < userDictionaryFilePaths.length; i++)
					{
						File userFile = new File(userDictionaryFilePaths[i]);

						naxConfig.withUserDictionary(userFile);
					}
				}

				String[] scriptArguments = line.getOptionValues("s");

				if (scriptArguments != null)
				{
					if (scriptArguments.length > 0)
					{
						for (int i = 0; i < scriptArguments.length; i++)
						{
							File potentialFile = new File(scriptArguments[i]);

							if (potentialFile.exists())
							{
								naxConfig.withScriptFile(potentialFile);
							}
							else
							{
								naxConfig.withScriptString(scriptArguments[i]);
							}
						}
					}
				}

				String[] itemScriptArguments = line.getOptionValues("fi");

				if (itemScriptArguments != null)
				{
					if (itemScriptArguments.length > 0)
					{
						for (int i = 0; i < itemScriptArguments.length; i++)
						{
							String[] itemScriptArgumentArray = StringUtils.split(itemScriptArguments[i], '=');
							String naaccrId = itemScriptArgumentArray[0];
							File potentialFile = new File(itemScriptArgumentArray[1]);

							if (potentialFile.exists())
							{
								naxConfig.withItemScriptFile(naaccrId, potentialFile);
							}
							else
							{
								naxConfig.withItemScriptString(naaccrId, itemScriptArgumentArray[1]);
							}
						}
					}
				}

				String[] tumorScriptArguments = line.getOptionValues("ft");

				if (tumorScriptArguments != null)
				{
					if (tumorScriptArguments.length > 0)
					{
						for (int i = 0; i < tumorScriptArguments.length; i++)
						{
							File potentialFile = new File(tumorScriptArguments[i]);

							if (potentialFile.exists())
							{
								naxConfig.withTumorScriptFile(potentialFile);
							}
							else
							{
								naxConfig.withTumorScriptString(tumorScriptArguments[i]);
							}
						}
					}
				}

				String[] patientScriptArguments = line.getOptionValues("fp");

				if (patientScriptArguments != null)
				{
					if (patientScriptArguments.length > 0)
					{
						for (int i = 0; i < patientScriptArguments.length; i++)
						{
							File potentialFile = new File(patientScriptArguments[i]);

							if (potentialFile.exists())
							{
								naxConfig.withPatientScriptFile(potentialFile);
							}
							else
							{
								naxConfig.withPatientScriptString(patientScriptArguments[i]);
							}
						}
					}
				}

				boolean removeEmptyPatients = Boolean.parseBoolean(line.getOptionValue("rep", "false"));
				naxConfig.withRemoveEmptyPatients(removeEmptyPatients);

				boolean includeTimestamp = Boolean.parseBoolean(line.getOptionValue("ts", "true"));

				String outputFileSuffix = StringUtils.EMPTY;

				if (includeTimestamp)
				{
					outputFileSuffix = DateFormatUtils.format(System.currentTimeMillis(), "_yyyyMMdd_Hmmss");
				}

				String outputFilePrefix = line.getOptionValue("pre", StringUtils.EMPTY);

				String outputFileString = line.getOptionValue("o");
				File outputFile = null;

				if (StringUtils.isNotEmpty(outputFileString))
				{
					outputFile = new File(outputFileString);

					if (inputFile.isDirectory() != outputFile.isDirectory())
					{
						throw new ParseException("input and outputfile must both be either a directory or a file: " + outputFileString);
					}
				}

				String replacementValuesFileString = line.getOptionValue("rpl");

				if (StringUtils.isNotEmpty(replacementValuesFileString))
				{
					File replacementValuesFile = new File(replacementValuesFileString);

					naxConfig.withReplacementMapFile(replacementValuesFile);
				}

				naxConfig.withEmailSmtpHost(line.getOptionValue("emailSmtpHost"));
				naxConfig.withEmailSubject(line.getOptionValue("emailSubject"));
				naxConfig.withEmailFrom(line.getOptionValue("emailFrom"));
				naxConfig.withEmailTo(line.getOptionValues("emailTo"));

				String[] constantValuesArray = line.getOptionValues("con");

				if (constantValuesArray != null && constantValuesArray.length > 0)
				{
					for (int i = 0; i < constantValuesArray.length; i++)
					{
						String[] constantValueSplit = StringUtils.split(constantValuesArray[i], '=');
						naxConfig.withConstantValue(constantValueSplit[0], constantValueSplit[1]);
					}
				}

				String includedItemsString = line.getOptionValue("i");

				if (includedItemsString != null)
				{
					includedItemsString = StringUtils.deleteWhitespace(includedItemsString);
					naxConfig.withIncludedItems(Arrays.asList(StringUtils.split(includedItemsString, ',')));
				}

				String[] valueCountsArray = line.getOptionValues("vc");

				if (valueCountsArray != null)
				{
					for (int i = 0; i < valueCountsArray.length; i++)
					{
						String valueCountsString = valueCountsArray[i];
						int indexOfFirstEquals = valueCountsString.indexOf('=');

						if (indexOfFirstEquals > -1)
						{
							String[] naaccrIdAndName = StringUtils.split(valueCountsString.substring(0, indexOfFirstEquals), '/');
							String naaccrId = naaccrIdAndName[0];
							String name = naaccrIdAndName[0];

							if (naaccrIdAndName.length > 1)
							{
								name = naaccrIdAndName[1];
							}

							String scriptString = valueCountsString.substring(indexOfFirstEquals + 1);
							File potentialFile = new File(scriptString);

							if (potentialFile.exists())
							{
								naxConfig.withValueCountsScriptFile(naaccrId, name, potentialFile);
							}
							else
							{
								naxConfig.withValueCountsScriptString(naaccrId, name, scriptString);
							}
						}
						else
						{
							String[] valueCountArray = StringUtils.split(valueCountsString, ',');

							for (int j = 0; j < valueCountArray.length; j++)
							{
								naxConfig.withValueCounts(valueCountArray[j]);
							}
						}
					}
				}

				String includeNamespacesString = line.getOptionValue("ns");

				if (includeNamespacesString == null)
				{
					if (naxConfig.getIncludedItems().size() > 0)
					{
						includeNamespacesString = "false";
					}
					else
					{
						includeNamespacesString = "true";
					}
				}

				naxConfig.withIncludeNamespaces(Boolean.valueOf(includeNamespacesString));


				Integer metricsLogging = Integer.valueOf(StringUtils.defaultString(line.getOptionValue("met"), "1"));

				Integer deleteOutputFiles = Integer.valueOf(StringUtils.defaultString(line.getOptionValue("del"), "0"));

				String excludedItemsString = line.getOptionValue("e");

				if (excludedItemsString != null)
				{
					excludedItemsString = StringUtils.deleteWhitespace(excludedItemsString);
					naxConfig.withExcludedItems(Arrays.asList(StringUtils.split(excludedItemsString, ',')));
				}

				Nax nax = Nax.newInstance(naxConfig);

				if (inputFile.isDirectory())
				{
					Collection<File> inputFiles = FileUtils
							.listFiles(inputFile, new String[]{"xml", "gz"}, true);

					for (File currentInputFile : inputFiles)
					{
						String outputFilename = String.format(
								"%s%s%s.%s",
								outputFilePrefix,
								FilenameUtils.getBaseName(currentInputFile.getName()),
								outputFileSuffix,
								FilenameUtils
										.getExtension(currentInputFile.getName()));

						File outputFileInDir = null;

						if (outputFile != null)
						{
							outputFileInDir = new File(outputFile, outputFilename);
						}

						try (ProgressTrackingDigestInputStream inputStream = ProgressTrackingDigestInputStream.newInstance(currentInputFile))
						{
							handleResult(nax.process(inputStream, outputFileInDir), metricsLogging, deleteOutputFiles);
						}
					}
				}
				else
				{
					try (ProgressTrackingDigestInputStream inputStream = ProgressTrackingDigestInputStream.newInstance(inputFile))
					{
						handleResult(nax.process(inputStream, outputFile), metricsLogging, deleteOutputFiles);
					}
				}
			}
		}
		catch (ParseException exception)
		{
			System.err.println("Command Line Parsing failed: " + exception.getMessage());

			printHelp(options);
		}
	}

	public static Map<String, String> getManifestValues()
			throws IOException
	{
		Map<String, String> valueMap = new LinkedHashMap<>();

		URL classUrl = NaxCommandLineApp.class.getResource(NaxCommandLineApp.class.getSimpleName() + ".class");
		URLConnection classUrlConnection = classUrl.openConnection();

		if (classUrlConnection instanceof JarURLConnection)
		{
			JarURLConnection classJarUrlConnecion = (JarURLConnection) classUrlConnection;
			Manifest mf = classJarUrlConnecion.getManifest();
			Attributes atts = mf.getMainAttributes();
			for (Object key : atts.keySet())
			{
				Attributes.Name name = (Attributes.Name) key;
				valueMap.put(name.toString(), atts.getValue(name));
			}
		}

		return valueMap;
	}

	private static String getFullAppVersionString()
			throws IOException
	{
		Map<String, String> manifestValues = getManifestValues();
		String versionString = String.format(
				"version %s - (%s; %s; %s)", manifestValues.get("App-Version"), manifestValues.get("Build-Time"), manifestValues.get("Build-Jdk"),
				Runtime.version());

		return versionString;
	}

	private static void printHelp(Options options)
			throws IOException
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("nax [OPTIONS] <Input File (.xml or .gz)>", null, options, getFullAppVersionString());
	}

	private static void printVersion()
			throws IOException
	{
		System.out.println(getFullAppVersionString());
	}

	private static Email createEmail(NaxResult naxResult)
			throws EmailException
	{
		Email email = new SimpleEmail();

		email.setHostName(naxResult.getNaxConfig().getEmailSmtpHost());
		email.setFrom(naxResult.getNaxConfig().getEmailFrom());

		if (StringUtils.isEmpty(naxResult.getNaxConfig().getEmailSubject()))
		{
			email.setSubject(
					String.format("nax Result from processing %s on %s", naxResult.getInputFileInfo().getName(), naxResult.getNaxMetrics().getStartTime()));
		}
		else
		{
			email.setSubject(naxResult.getNaxConfig().getEmailSubject());
		}

		email.addTo(naxResult.getNaxConfig().getEmailToList().toArray(new String[]{}));

		if (StringUtils.isNoneEmpty(naxResult.getNaxConfig().getEmailUsername(), naxResult.getNaxConfig().getEmailPassword()))
		{
			email.setAuthentication(naxResult.getNaxConfig().getEmailUsername(), naxResult.getNaxConfig().getEmailPassword());
		}

		if (naxResult.getNaxConfig().getEmailSmtpPort() != null)
		{
			email.setSmtpPort(naxResult.getNaxConfig().getEmailSmtpPort());
		}

		if (naxResult.getNaxConfig().getEmailSslCheckServerIdentity() != null)
		{
			email.setSSLCheckServerIdentity(naxResult.getNaxConfig().getEmailSslCheckServerIdentity());
		}

		if (naxResult.getNaxConfig().getEmailStartTlsEnabled() != null)
		{
			email.setStartTLSEnabled(naxResult.getNaxConfig().getEmailStartTlsEnabled());
		}

		if (naxResult.getNaxConfig().getEmailStartTlsRequired() != null)
		{
			email.setStartTLSRequired(naxResult.getNaxConfig().getEmailStartTlsRequired());
		}

		if (naxResult.getNaxConfig().getEmailSslOnConnect() != null)
		{
			email.setSSLOnConnect(naxResult.getNaxConfig().getEmailSslOnConnect());
		}

		if (naxResult.getNaxConfig().getEmailSslSmtpPort() != null)
		{
			email.setSslSmtpPort(naxResult.getNaxConfig().getEmailSslSmtpPort());
		}

		return email;
	}

	private static void handleResult(
			NaxResult naxResult,
			Integer metricsLogging,
			Integer deleteOutputFiles)
			throws IOException, EmailException
	{
		String resultString = resultAsString(naxResult, metricsLogging);

		logger.info("NaxResult:");
		System.out.println(resultString);

		if (naxResult.getNaxConfig().getEmailToList().isEmpty() == false &&
				StringUtils.isNoneEmpty(
						naxResult.getNaxConfig().getEmailSmtpHost(),
						naxResult.getNaxConfig().getEmailFrom()))
		{
			logger.info("Sending email...");
			Email email = createEmail(naxResult);
			email.setMsg(resultString);
			email.send();
		}

		if (naxResult.getOutputFile() != null)
		{
			if (deleteOutputFiles.intValue() == 1)
			{
				Integer patientCount = naxResult.getNaxMetrics().getElementCounts().get(NaaccrConstants.PATIENT_ELEMENT);

				if (patientCount == null || patientCount.intValue() == 0)
				{
					logger.info(String.format("Deleting file with no patients: %s", naxResult.getOutputFile().getName()));
					FileUtils.forceDelete(naxResult.getOutputFile());
				}
			}
			else if (deleteOutputFiles.intValue() == 2)
			{
				Integer tumorCount = naxResult.getNaxMetrics().getElementCounts().get(NaaccrConstants.TUMOR_ELEMENT);

				if (tumorCount == null || tumorCount.intValue() == 0)
				{
					logger.info(String.format("Deleting file with no tumors: %s", naxResult.getOutputFile().getName()));
					FileUtils.forceDelete(naxResult.getOutputFile());
				}
			}
		}
	}

	private static String resultAsString(
			NaxResult naxResult,
			Integer metricsLogging)
			throws JsonProcessingException
	{
		String returnValue = null;

		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

		if (metricsLogging.intValue() == 0)
		{
			naxResult.setNaxMetrics(null);
		}

		if (metricsLogging.intValue() == 1)
		{
			naxResult.getNaxMetrics().setOtherElementCounts(null);
			naxResult.getNaxMetrics().setExcludedOtherElementCounts(null);
			naxResult.getNaxMetrics().setNaaccrIdCounts(null);
			naxResult.getNaxMetrics().setExcludedNaaccrIdCounts(null);
		}

		returnValue = String.format("%s", objectMapper.writer().withoutAttribute("").writeValueAsString(naxResult));

		return returnValue;
	}

}
